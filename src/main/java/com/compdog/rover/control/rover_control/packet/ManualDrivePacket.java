package com.compdog.rover.control.rover_control.packet;

public class ManualDrivePacket {
    public double left;
    public double right;

    public ManualDrivePacket(double left, double right){
        this.left = left;
        this.right = right;
    }
}
