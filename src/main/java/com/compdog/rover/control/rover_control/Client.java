package com.compdog.rover.control.rover_control;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.EventListener;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

import com.compdog.rover.control.rover_control.util.ManualResetEvent;
import com.compdog.rover.control.rover_control.util.RollingBuffer;
import org.apache.commons.lang3.time.StopWatch;
import org.jetbrains.annotations.Nullable;
import org.json.*;

public class Client {
    public interface UpdatedListener extends EventListener {
        void updated(double m0,double m1,double m2, double m3, double m4, double m5, double coreTemp);
    }

    public interface ConnectionUpdatedListener extends EventListener {
        void updated(boolean statusChanged, long avg, long gap);
    }

    private final List<UpdatedListener> updated = new ArrayList<>();
    private final List<ConnectionUpdatedListener> connectionUpdated = new ArrayList<>();

    public void addUpdateListener(UpdatedListener listener){
        updated.add(listener);
    }

    private void dispatchUpdatedEvent(double m0,double m1,double m2, double m3, double m4, double m5, double coreTemp){
        for(UpdatedListener listener : updated){
            listener.updated(m0, m1, m2, m3, m4, m5, coreTemp);
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

    private @Nullable Socket socket;
    private @Nullable OutputStreamWriter writer;
    private boolean connected;
    private boolean running;
    private boolean disposed = false;
    private final ManualResetEvent startEvent;
    private final ManualResetEvent receiveEvent;

    private final String host;
    private final int port;

    private @Nullable Thread socketThread;
    private @Nullable Thread timeoutThread;

    public Client(String host, int port){
        socket = null;
        writer = null;
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

                    socket = new Socket();
                    System.out.println("[Client] Trying to connect");

                    socket.connect(new InetSocketAddress(host, port), 5000);

                    connected = true;
                    System.out.println("[Client] Connected on remote port " + socket.getPort() + " from " + socket.getLocalPort());
                    dispatchConnectionUpdatedEvent();

                    writer = new OutputStreamWriter(socket.getOutputStream());
                    Scanner reader = new Scanner(socket.getInputStream());

                    while (!socket.isClosed() && running) {
                        String line = reader.nextLine();

                        receiveEvent.set();

                        System.out.println("Recieved  line: " + line);
                    }

                    reader.close();
                    socket.close();
                    connected = false;
                    System.out.println("[Client] Disconnected from server");
                    dispatchConnectionUpdatedEvent();
                }
                catch (SocketTimeoutException timeout){
                    System.out.println("[Client] Connection timed out!");
                }
                catch (IOException e) {
                    e.printStackTrace();
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
                    if (socket != null) {
                        try {
                            socket.close();
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

    public void SendUpdate(double left, double right) {
        if(writer == null || !connected)
            return;

        try {
            writer.write(String.format("DRIVE:%f|%f\n", left, right));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean IsConnected(){
        return connected;
    }
}
