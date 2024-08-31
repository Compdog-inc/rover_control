package com.compdog.rover.control.rover_control;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;

public class MotorPanel extends Pane {
    private final Canvas canvas;

    private double value = 0.0;

    public void setValue(double value){
        this.value = value;
        draw();
    }

    public double getValue(){
        return value;
    }

    public MotorPanel() {
        canvas = new Canvas(getWidth(), getHeight());
        getChildren().add(canvas);
        widthProperty().addListener(e -> canvas.setWidth(getWidth()));
        heightProperty().addListener(e -> canvas.setHeight(getHeight()));
    }

    private void draw(){
        GraphicsContext gc = canvas.getGraphicsContext2D();

        gc.clearRect(0, 0, getWidth(), getHeight());

        if (value > 0)
        {
            gc.setFill(Color.GREEN);
            gc.fillRect(getWidth()/2,0,value*getWidth()/2,getHeight());
        }
        else
        {
            double x = (-value) * getWidth() / 2;
            gc.setFill(Color.RED);
            gc.fillRect(getWidth()/2-x,0,x,getHeight());
        }
    }

    @Override
    protected void layoutChildren() {
        super.layoutChildren();
        draw();
    }
}
