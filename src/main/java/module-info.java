module com.compdog.rover.control.rover_control {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.json;
    requires org.apache.commons.lang3;
    requires org.jetbrains.annotations;

    opens com.compdog.rover.control.rover_control to javafx.fxml;
    exports com.compdog.rover.control.rover_control;
}