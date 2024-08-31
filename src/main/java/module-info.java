module com.compdog.rover.control.rover_control {
    requires javafx.controls;
    requires javafx.fxml;
            
        requires org.controlsfx.controls;
            requires com.dlsc.formsfx;
                    requires org.kordamp.bootstrapfx.core;
    requires org.json;
    requires org.apache.commons.lang3;
    requires org.jetbrains.annotations;
    requires java.management;

    opens com.compdog.rover.control.rover_control to javafx.fxml;
    exports com.compdog.rover.control.rover_control;
}