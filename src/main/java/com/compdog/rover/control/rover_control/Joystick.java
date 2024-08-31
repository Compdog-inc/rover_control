package com.compdog.rover.control.rover_control;

import javafx.geometry.VPos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.stage.Screen;

import java.util.ArrayList;
import java.util.EventListener;
import java.util.List;

public class Joystick extends Pane {

    public interface UpdatedListener extends EventListener {
        void updated(boolean isOneShot);
    }

    private final Canvas canvas;

    private double X = 0;
    private double Y = 0;

    private double tempX = 0;
    private double tempY = 0;

    private boolean down = false;

    public double getX(){
        return  X;
    }

    public double getY() {
        return Y;
    }

    private final List<UpdatedListener> updated = new ArrayList<>();

    public void addUpdateListener(UpdatedListener listener){
        updated.add(listener);
    }

    private void dispatchUpdatedEvent(boolean isOneShot){
        for(UpdatedListener listener : updated){
            listener.updated(isOneShot);
        }
    }

    public Joystick() {
        canvas = new Canvas(getWidth(), getHeight());
        getChildren().add(canvas);
        widthProperty().addListener(e -> canvas.setWidth(getWidth()));
        heightProperty().addListener(e -> canvas.setHeight(getHeight()));
        setOnMouseMoved(e->{
            e.consume();
            tempX = (e.getX() / getWidth()) * 2.0 - 1;
            tempY = -((e.getY() / getHeight()) * 2.0 - 1);

            if (down) {
                X = tempX;
                Y = tempY;
            }

            draw();

            dispatchUpdatedEvent(false);
        });

        setOnMousePressed(e->{
            e.consume();
            tempX = (e.getX() / getWidth()) * 2.0 - 1;
            tempY = -((e.getY() / getHeight()) * 2.0 - 1);
            X=tempX;
            Y=tempY;
            down = true;
            draw();

            dispatchUpdatedEvent(true);
        });

        setOnMouseReleased(e->{
            e.consume();
            tempX=tempY=0;
            X=0;
            Y=0;
            down = false;
            draw();

            dispatchUpdatedEvent(true);
        });

        setOnMouseDragged(e->{
            e.consume();
            tempX = (e.getX() / getWidth()) * 2.0 - 1;
            tempY = -((e.getY() / getHeight()) * 2.0 - 1);

            if (down) {
                X = tempX;
                Y = tempY;
            }

            draw();

            dispatchUpdatedEvent(false);
        });
    }

    private void draw(){
        GraphicsContext gc = canvas.getGraphicsContext2D();

        gc.clearRect(0, 0, getWidth(), getHeight());

        gc.setStroke(Color.WHITE);
        gc.strokeLine(getWidth()/2,0,getWidth()/2,getHeight());
        gc.strokeLine(0,getHeight()/2,getWidth(),getHeight()/2);

        double px = (X + 1) / 2.0f * getWidth();
        double py = (-Y + 1) / 2.0f * getHeight();
        double size = 20 / Screen.getPrimary().getOutputScaleX();
        gc.setFill(Color.RED);
        gc.fillOval(px-size/2, py-size/2,size,size);

        String str = (Math.floor(tempX * 100.0) / 100.0) + ", " + (Math.floor(tempY * 100.0) / 100.0f);
        gc.setTextBaseline(VPos.BOTTOM);
        gc.setFill(Color.WHITE);
        gc.fillText(str, 10, getHeight() - 10);
    }

    @Override
    protected void layoutChildren() {
        super.layoutChildren();
        draw();
    }
}
