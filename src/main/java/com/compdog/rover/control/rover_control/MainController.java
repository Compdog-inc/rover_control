package com.compdog.rover.control.rover_control;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import org.apache.commons.lang3.time.StopWatch;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

public class MainController {
    @FXML
    private Joystick joystick;
    @FXML
    private MotorPanel m0;
    @FXML
    private MotorPanel m1;
    @FXML
    private MotorPanel m2;
    @FXML
    private MotorPanel m3;
    @FXML
    private MotorPanel m4;
    @FXML
    private MotorPanel m5;

    @FXML
    private Label coreTemp;

    @FXML
    private Label connection;

    @FXML
    private Label quality;

    private Client client;

    private final StopWatch lastTransaction = StopWatch.createStarted();
    private Timer heartbeatTimer;

    private Paint notConnectedPaint;
    private Paint connectedPaint;

    @FXML
    public void initialize() {
        joystick.addUpdateListener(isOneShot -> {
                    if (client == null) return;
                    if (lastTransaction.getTime(TimeUnit.MILLISECONDS) > 100 || isOneShot) {
                        Drivetrain.DrivetrainResult drive = Drivetrain.Drive(joystick.getX(), joystick.getY());
                        client.SendUpdate(drive.left, drive.right);
                        lastTransaction.reset();
                        lastTransaction.start();
                    }
                }
        );

        notConnectedPaint = Color.RED;
        connectedPaint = Color.BLACK;

        heartbeatTimer = new Timer();
        heartbeatTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (client == null) return;
                if (lastTransaction.getTime(TimeUnit.MILLISECONDS) > 1000) {
                    Drivetrain.DrivetrainResult drive = Drivetrain.Drive(joystick.getX(), joystick.getY());
                    client.SendUpdate(drive.left, drive.right);
                    lastTransaction.reset();
                    lastTransaction.start();
                }
            }
        }, 100, 100);
    }

    public void deinitialize() {
        heartbeatTimer.cancel();
    }

    public void setClient(Client client) {
        this.client = client;
        client.addUpdateListener((m01, m11, m21, m31, m41, m51, coreTemp1) -> {
            m0.setValue(m01);
            m1.setValue(m11);
            m2.setValue(m21);
            m3.setValue(m31);
            m4.setValue(m41);
            m5.setValue(m51);

            Platform.runLater(() -> coreTemp.setText(Math.round(coreTemp1 * 100.0) / 100.0 + " C"));
        });

        client.addConnectionUpdateListener((status, avg, gap) -> Platform.runLater(() -> {
            if (status) {
                connection.setText(client.IsConnected() ? "Connected" : "Not Connected");
                connection.setTextFill(client.IsConnected() ? connectedPaint : notConnectedPaint);
            } else {
                double qualityValue = 200.0  / ((double)avg+1) + 100.0 / ((double)gap + 1);
                quality.setText("Quality: " + Math.round(qualityValue));
            }
        }));
    }
}