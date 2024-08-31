package com.compdog.rover.control.rover_control;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;

public class MainApplication extends Application {

    private @Nullable Client client;
    private MainController controller;

    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(MainApplication.class.getResource("main-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 650, 370);
        stage.setTitle("Rover Control");
        stage.setScene(scene);
        stage.setResizable(false);
        stage.show();

        controller = fxmlLoader.getController();

        client = new Client("10.67.31.2", 5001);
        client.Start();
        controller.setClient(client);
    }

    @Override
    public void stop() {
        controller.deinitialize();

        if (client != null) {
            client.Dispose();
        }
    }

    public static void main(String[] args) {
        launch();
    }
}