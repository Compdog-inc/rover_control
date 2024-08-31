package com.compdog.rover.control.rover_control;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.EventListener;
import java.util.List;

import org.json.*;

public class Client {
    public interface UpdatedListener extends EventListener {
        void updated(double m0,double m1,double m2, double m3, double m4, double m5, double coreTemp);
    }

    private final List<UpdatedListener> updated = new ArrayList<>();

    public void addUpdateListener(UpdatedListener listener){
        updated.add(listener);
    }

    private void dispatchUpdatedEvent(double m0,double m1,double m2, double m3, double m4, double m5, double coreTemp){
        for(UpdatedListener listener : updated){
            listener.updated(m0, m1, m2, m3, m4, m5, coreTemp);
        }
    }

    private Socket socket;
    private OutputStreamWriter writer;

    public void Connect(){
        try {
            socket = new Socket("10.67.31.2", 5001);
            System.out.println("Connected on remote port " + socket.getPort() + " from " + socket.getLocalPort());

            writer = new OutputStreamWriter(socket.getOutputStream());

            Thread readThread = new Thread(this::ReadThread, "readThread");
            readThread.setDaemon(true);
            readThread.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void Disconnect(){
        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void processJsonStr(String str){
        try {
            JSONObject json = new JSONObject(str);
            double m0 = json.getDouble("motor0");
            double m1 = json.getDouble("motor1");
            double m2 = json.getDouble("motor2");
            double m3 = json.getDouble("motor3");
            double m4 = json.getDouble("motor4");
            double m5 = json.getDouble("motor5");
            double coreTemp = json.getDouble("temp");
            dispatchUpdatedEvent(m0, m1, m2, m3, m4, m5, coreTemp);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private final StringBuilder jsonBuffer = new StringBuilder();

    private void ReadThread() {
        try {
            InputStreamReader reader = new InputStreamReader(socket.getInputStream());
            char[] buffer = new char[1024];
            while (!socket.isClosed()) {
                int length = reader.read(buffer);
                if (length == -1)
                    return;
                for (int i = 0; i < length; i++) {
                    if (buffer[i] == '}') {
                        jsonBuffer.append(buffer[i]);
                        processJsonStr(jsonBuffer.toString());
                        jsonBuffer.setLength(0);
                        jsonBuffer.trimToSize();
                    } else {
                        jsonBuffer.append(buffer[i]);
                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void SendUpdate(double left, double right) {
        try {
            JSONObject json = new JSONObject();

            left = Math.max(-1, Math.min(1, left));
            right = Math.max(-1, Math.min(1, right));

            left = Math.floor(left * 100.0) / 100.0;
            right = Math.floor(right * 100.0) / 100.0;

            json.put("left", left);
            json.put("right", right);

            writer.write(json.toString(0));
            writer.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
