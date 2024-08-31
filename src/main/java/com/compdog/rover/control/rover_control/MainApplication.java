package com.compdog.rover.control.rover_control;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Screen;
import javafx.stage.Stage;

import java.io.IOException;

public class MainApplication extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(MainApplication.class.getResource("main-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 842, 403);
        stage.setTitle("Rover Control");
        stage.setScene(scene);
        stage.setResizable(false);
        stage.show();

        MainController controller = fxmlLoader.getController();

        Client client = new Client();
        //client.Connect();
        controller.setClient(client);
    }

    public static void main(String[] args) {
        launch();
    }
}