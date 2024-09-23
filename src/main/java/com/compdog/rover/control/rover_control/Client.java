package com.compdog.rover.control.rover_control;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.*;
import java.util.concurrent.TimeUnit;

import com.compdog.rover.control.rover_control.packet.*;
import com.compdog.rover.control.rover_control.util.ManualResetEvent;
import com.compdog.rover.control.rover_control.util.RollingBuffer;
import com.compdog.rover.control.rover_control.util.Vector3;
import com.compdog.rover.control.rover_control.util.Vector4;
import org.apache.commons.lang3.time.StopWatch;
import org.jetbrains.annotations.Nullable;

public class Client {

    private static final String HEALTH_PACKET = "HEALTH:";
    private static final String DRIVETRAIN_PACKET = "DRIVE:";
    private static final String WHISKERS_PACKET = "WHISKR:";
    private static final String CHARACTERISTICS_PACKET = "CHARS:";

    private static final String COMMAND_REQUEST_HEALTH = "GETHEALTH:";
    private static final String COMMAND_REQUEST_CHARACTERISTICS = "GETCHARS:";
    private static final String COMMAND_MANUAL_DRIVE = "MDRIVE:";
    private static final String COMMAND_SET_OPTS = "SETOPTS:";

    public interface UpdatedListener extends EventListener {
        void updated(DrivetrainPacket packet);
        void updated(HealthPacket packet);
        void updated(WhiskersPacket packet);
    }

    public interface ConnectionUpdatedListener extends EventListener {
        void updated(boolean statusChanged, long avg, long gap);
    }

    private final List<UpdatedListener> updated = new ArrayList<>();
    private final List<ConnectionUpdatedListener> connectionUpdated = new ArrayList<>();

    public void addUpdateListener(UpdatedListener listener){
        updated.add(listener);
    }

    private void dispatchUpdatedEvent(DrivetrainPacket packet){
        for(UpdatedListener listener : updated){
            listener.updated(packet);
        }
    }

    private void dispatchUpdatedEvent(HealthPacket packet){
        for(UpdatedListener listener : updated){
            listener.updated(packet);
        }
    }

    private void dispatchUpdatedEvent(WhiskersPacket packet){
        for(UpdatedListener listener : updated){
            listener.updated(packet);
        }
    }

    public void addConnectionUpdateListener(ConnectionUpdatedListener listener){
        connectionUpdated.add(listener);
    }

    private void dispatchConnectionUpdatedEvent(){
        for(ConnectionUpdatedListener listener : connectionUpdated){
            listener.updated(true ,0 ,0);
        }
    }

    private void dispatchConnectionUpdatedEvent(long avg, long gap){
        for(ConnectionUpdatedListener listener : connectionUpdated){
            listener.updated(false, avg, gap);
        }
    }

    private @Nullable Socket socketPrimary;
    private @Nullable Socket socketDriver;
    private @Nullable OutputStreamWriter writer;
    private @Nullable OutputStreamWriter driverWriter;
    private boolean connected;
    private boolean running;
    private boolean disposed = false;
    private final ManualResetEvent startEvent;
    private final ManualResetEvent receiveEvent;

    private final String host;
    private final int port;

    private @Nullable Thread socketThread;
    private @Nullable Thread timeoutThread;

    private @Nullable CharacteristicsPacket characteristics = null;

    public Client(String host, int port){
        socketPrimary = null;
        socketDriver = null;
        writer = null;
        driverWriter = null;
        connected = false;
        running = false;

        socketThread = null;

        this.host = host;
        this.port = port;

        startEvent = new ManualResetEvent(false);
        receiveEvent = new ManualResetEvent(false);
    }

    public void Start() {
        running = true;
        startEvent.set();

        if (socketThread == null) {
            socketThread = new Thread(this::clientSocketThread, "Client Socket Thread");
            socketThread.setDaemon(true);
            socketThread.start();
        }

        if (timeoutThread == null) {
            timeoutThread = new Thread(this::clientTimeoutThread, "Client Timeout Thread");
            timeoutThread.setDaemon(true);
            timeoutThread.start();
        }
    }

    public void Stop(){
        running = false;
    }

    public void Dispose() {
        disposed = true;
        Stop();
        startEvent.set();
        receiveEvent.set();
        if (socketThread != null) {
            try {
                socketThread.join(10000);
            } catch (InterruptedException ignored) {
                socketThread.interrupt();
            }
        }
        if (timeoutThread != null) {
            try {
                timeoutThread.join(10000);
            } catch (InterruptedException ignored) {
                timeoutThread.interrupt();
            }
        }
    }

    private void clientSocketThread() {
        while (!disposed) {
            connected = false;
            dispatchConnectionUpdatedEvent();

            try {
                startEvent.waitOne();
            } catch (InterruptedException e) {
                System.out.println("[Client] Client socket thread interrupted!");
                break;
            }

            if(disposed)
                break;

            startEvent.reset();
            while (running) {
                try {
                    connected = false;
                    dispatchConnectionUpdatedEvent();

                    socketPrimary = new Socket();
                    socketDriver = new Socket();
                    System.out.println("[Client] Trying to connect");

                    socketPrimary.connect(new InetSocketAddress(host, port), 5000);
                    socketDriver.connect(new InetSocketAddress(host, port), 5000);

                    connected = true;
                    System.out.println("[Client] Connected on remote port " + socketPrimary.getPort() + " from " + socketPrimary.getLocalPort()+"/"+socketDriver.getLocalPort());
                    dispatchConnectionUpdatedEvent();

                    writer = new OutputStreamWriter(socketPrimary.getOutputStream());
                    driverWriter = new OutputStreamWriter(socketDriver.getOutputStream());
                    Scanner reader = new Scanner(socketPrimary.getInputStream());

                    // Request info about the server we are connected to
                    RequestCharacteristics();
                    SetOptions(ClientOptionFlags.LISTEN_DRIVETRAIN | ClientOptionFlags.LISTEN_WHISKERS);

                    while (!socketPrimary.isClosed() && !socketDriver.isClosed() && running) {
                        String line;
                        try {
                            line = reader.nextLine();
                        } catch (NoSuchElementException e){
                            break;
                        }

                        receiveEvent.set();

                        if (line.startsWith(HEALTH_PACKET)) {
                            String[] parts = line.substring(HEALTH_PACKET.length()).split("\\|");
                            HealthPacket packet = new HealthPacket();
                            packet.temp = Double.parseDouble(parts[0]);
                            packet.memoryUsed = Long.parseLong(parts[1]);
                            packet.memoryTotal = Long.parseLong(parts[2]);

                            dispatchUpdatedEvent(packet);
                        } else if (line.startsWith(DRIVETRAIN_PACKET)) {
                            String[] parts = line.substring(DRIVETRAIN_PACKET.length()).split("\\|");
                            DrivetrainPacket packet = new DrivetrainPacket();
                            packet.motor0 = Double.parseDouble(parts[0]);
                            packet.motor1 = Double.parseDouble(parts[1]);
                            packet.motor2 = Double.parseDouble(parts[2]);
                            packet.motor3 = Double.parseDouble(parts[3]);
                            packet.motor4 = Double.parseDouble(parts[4]);
                            packet.motor5 = Double.parseDouble(parts[5]);

                            dispatchUpdatedEvent(packet);
                        } else if (line.startsWith(WHISKERS_PACKET)) {
                            String[] parts = line.substring(WHISKERS_PACKET.length()).split("\\|");
                            WhiskersPacket packet = new WhiskersPacket();
                            packet.sensor0 = Integer.parseInt(parts[0]);
                            packet.sensor1 = Integer.parseInt(parts[1]);
                            packet.sensor2 = Integer.parseInt(parts[2]);
                            packet.sensor3 = Integer.parseInt(parts[3]);
                            packet.sensor4 = Integer.parseInt(parts[4]);
                            packet.sensor5 = Integer.parseInt(parts[5]);

                            dispatchUpdatedEvent(packet);
                        } else if (line.startsWith(CHARACTERISTICS_PACKET)) {
                            String[] parts = line.substring(CHARACTERISTICS_PACKET.length()).split("\\|");
                            CharacteristicsPacket packet = new CharacteristicsPacket();

                            packet.frameSize = new Vector3(
                                    Double.parseDouble(parts[0]),
                                    Double.parseDouble(parts[1]),
                                    Double.parseDouble(parts[2])
                            );

                            packet.wheelDiameter = Double.parseDouble(parts[3]);

                            packet.wheel0Location = new Vector3(
                                    Double.parseDouble(parts[4]),
                                    Double.parseDouble(parts[5]),
                                    Double.parseDouble(parts[6])
                            );
                            packet.wheel1Location = new Vector3(
                                    Double.parseDouble(parts[7]),
                                    Double.parseDouble(parts[8]),
                                    Double.parseDouble(parts[9])
                            );
                            packet.wheel2Location = new Vector3(
                                    Double.parseDouble(parts[10]),
                                    Double.parseDouble(parts[11]),
                                    Double.parseDouble(parts[12])
                            );
                            packet.wheel3Location = new Vector3(
                                    Double.parseDouble(parts[13]),
                                    Double.parseDouble(parts[14]),
                                    Double.parseDouble(parts[15])
                            );
                            packet.wheel4Location = new Vector3(
                                    Double.parseDouble(parts[16]),
                                    Double.parseDouble(parts[17]),
                                    Double.parseDouble(parts[18])
                            );
                            packet.wheel5Location = new Vector3(
                                    Double.parseDouble(parts[19]),
                                    Double.parseDouble(parts[20]),
                                    Double.parseDouble(parts[21])
                            );

                            packet.whisker0 = new Vector4(
                                    Double.parseDouble(parts[22]),
                                    Double.parseDouble(parts[23]),
                                    Double.parseDouble(parts[24]),
                                    Double.parseDouble(parts[25])
                            );
                            packet.whisker1 = new Vector4(
                                    Double.parseDouble(parts[26]),
                                    Double.parseDouble(parts[27]),
                                    Double.parseDouble(parts[28]),
                                    Double.parseDouble(parts[29])
                            );
                            packet.whisker2 = new Vector4(
                                    Double.parseDouble(parts[30]),
                                    Double.parseDouble(parts[31]),
                                    Double.parseDouble(parts[32]),
                                    Double.parseDouble(parts[33])
                            );
                            packet.whisker3 = new Vector4(
                                    Double.parseDouble(parts[34]),
                                    Double.parseDouble(parts[35]),
                                    Double.parseDouble(parts[36]),
                                    Double.parseDouble(parts[37])
                            );
                            packet.whisker4 = new Vector4(
                                    Double.parseDouble(parts[38]),
                                    Double.parseDouble(parts[39]),
                                    Double.parseDouble(parts[40]),
                                    Double.parseDouble(parts[41])
                            );
                            packet.whisker5 = new Vector4(
                                    Double.parseDouble(parts[42]),
                                    Double.parseDouble(parts[43]),
                                    Double.parseDouble(parts[44]),
                                    Double.parseDouble(parts[45])
                            );

                            characteristics = packet;
                        } else {
                            System.out.println("[Client] Unexpected command " + line);
                        }
                    }

                    reader.close();
                    socketPrimary.close();
                    socketDriver.close();
                    connected = false;
                    System.out.println("[Client] Disconnected from server");
                    dispatchConnectionUpdatedEvent();
                }
                catch (SocketTimeoutException timeout){
                    System.out.println("[Client] Connection timed out!");
                }
                catch (IOException e) {
                    System.err.println("[Client] Error " + e.getMessage());
                }

                System.out.println("[Client] Lost connection with server");

                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.yield();
                }
            }
        }

        connected = false;
        dispatchConnectionUpdatedEvent();

        System.out.println("[Client] Client socket thread dying");
    }

    private void clientTimeoutThread() {
        boolean received;
        StopWatch sw = StopWatch.create();
        RollingBuffer buffer = new RollingBuffer(20);

        while (!disposed) {
            sw.reset();
            sw.start();
            try {
                received = receiveEvent.waitOne(5000);
            } catch (InterruptedException e) {
                System.out.println("[Client] Client timeout thread interrupted!");
                break;
            }

            if (disposed)
                break;

            if (received) {
                receiveEvent.reset();
            }

            if (connected && running) {
                if (!received) {
                    System.out.println("[Client] Reached client timeout. Disconnecting");
                    if (socketPrimary != null && socketDriver != null) {
                        try {
                            socketPrimary.close();
                            socketDriver.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                            Stop();
                        }
                    } else {
                        Stop();
                    }
                } else {
                    long interval = sw.getTime(TimeUnit.MILLISECONDS);
                    buffer.push(interval);
                    long avg = buffer.getAverage();
                    long gap = buffer.getGap();
                    dispatchConnectionUpdatedEvent(avg, gap);
                }
            }
        }

        System.out.println("[Client] Client timeout thread dying");
    }

    public void SendPacket(ManualDrivePacket packet, boolean useDriver) {
        OutputStreamWriter _wrt = useDriver ? driverWriter : writer;

        if (_wrt == null || !connected)
            return;

        try {
            _wrt.write(String.format("%s%f|%f\n", COMMAND_MANUAL_DRIVE, packet.left, packet.right));
            _wrt.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void RequestHealth(){
        if(writer == null || !connected)
            return;

        try {
            writer.write(COMMAND_REQUEST_HEALTH+"\n");
            writer.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void RequestCharacteristics(){
        if(writer == null || !connected)
            return;

        try {
            writer.write(COMMAND_REQUEST_CHARACTERISTICS+"\n");
            writer.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void SetOptions(int options){
        if(writer == null || !connected)
            return;

        try {
            writer.write(COMMAND_SET_OPTS + options+"\n");
            writer.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public @Nullable CharacteristicsPacket getCharacteristics(){
        return characteristics;
    }

    public boolean IsConnected(){
        return connected;
    }
}
