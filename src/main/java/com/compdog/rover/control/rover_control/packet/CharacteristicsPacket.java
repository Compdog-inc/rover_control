package com.compdog.rover.control.rover_control.packet;

import com.compdog.rover.control.rover_control.util.Vector3;
import com.compdog.rover.control.rover_control.util.Vector4;

public class CharacteristicsPacket {
    public Vector3 frameSize;

    public double wheelDiameter;

    public Vector3 wheel1Location;
    public Vector3 wheel0Location;
    public Vector3 wheel3Location;
    public Vector3 wheel4Location;
    public Vector3 wheel5Location;
    public Vector3 wheel2Location;

    public Vector4 whisker0;
    public Vector4 whisker1;
    public Vector4 whisker2;
    public Vector4 whisker3;
    public Vector4 whisker4;
    public Vector4 whisker5;
}
