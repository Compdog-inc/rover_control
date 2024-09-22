package com.compdog.rover.control.rover_control;

import com.compdog.rover.control.rover_control.packet.DrivetrainPacket;
import com.compdog.rover.control.rover_control.packet.HealthPacket;
import com.compdog.rover.control.rover_control.packet.ManualDrivePacket;
import com.compdog.rover.control.rover_control.packet.WhiskersPacket;
import com.compdog.rover.control.rover_control.util.CurveUtils;
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
    private Label memStatus;

    @FXML
    private Label connection;

    @FXML
    private Label quality;

    private Client client;

    private final StopWatch lastDrive = StopWatch.createStarted();
    private final StopWatch lastHealth = StopWatch.createStarted();
    private Timer heartbeatTimer;

    private Paint notConnectedPaint;
    private Paint connectedPaint;

    @FXML
    public void initialize() {
        joystick.addUpdateListener(isOneShot -> {
                    if (client == null) return;
                    if (isOneShot) {
                        Drivetrain.DrivetrainResult drive = Drivetrain.Drive(joystick.getX(), joystick.getY());
                        client.SendPacket(new ManualDrivePacket(drive.left, drive.right), true);
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
                if (lastHealth.getTime(TimeUnit.MILLISECONDS) > 500) {
                    client.RequestHealth();
                    lastHealth.reset();
                    lastHealth.start();
                }

                if (lastDrive.getTime(TimeUnit.MILLISECONDS) > 50) {
                    Drivetrain.DrivetrainResult drive = Drivetrain.Drive(joystick.getX(), joystick.getY());
                    client.SendPacket(new ManualDrivePacket(drive.left, drive.right), true);
                    lastDrive.reset();
                    lastDrive.start();
                }
            }
        }, 100, 10);
    }

    public void deinitialize() {
        heartbeatTimer.cancel();
    }

    public void setClient(Client client) {
        this.client = client;
        client.addUpdateListener(new Client.UpdatedListener() {
            @Override
            public void updated(DrivetrainPacket packet) {
                m0.setValue(packet.motor0);
                m1.setValue(packet.motor1);
                m2.setValue(packet.motor2);
                m3.setValue(packet.motor3);
                m4.setValue(packet.motor4);
                m5.setValue(packet.motor5);
            }

            @Override
            public void updated(HealthPacket packet) {
                Platform.runLater(() -> {
                    coreTemp.setText(Math.round(packet.temp * 100.0) / 100.0 + " C");
                    memStatus.setText(String.format("%.2f kB / %.2f kB (%d%%)", packet.memoryUsed / 1024.0, packet.memoryTotal / 1024.0, (packet.memoryUsed * 100 / packet.memoryTotal)));
                });
            }

            @Override
            public void updated(WhiskersPacket packet) {

            }
        });

        client.addConnectionUpdateListener((status, avg, gap) -> Platform.runLater(() -> {
            if (status) {
                connection.setText(client.IsConnected() ? "Connected" : "Not Connected");
                connection.setTextFill(client.IsConnected() ? connectedPaint : notConnectedPaint);
            } else {
                /* Consists of two parts, latency (50%) and consistency (50%) */
                double qualityValue =
                        /* average interval of 100 is good (50% quality) */
                        CurveUtils.InverseCurve(avg, 100, 50, 1.2) +
                        /* gap of 1 is good consistency (50% quality) */
                        CurveUtils.InverseCurve(gap, 1, 50, 0.2);
                quality.setText("Quality: " + Math.round(qualityValue));
            }
        }));
    }
}