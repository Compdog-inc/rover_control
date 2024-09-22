package com.compdog.rover.control.rover_control.packet;

public final class ClientOptionFlags {
    public static final byte NONE = 0;
    public static final byte LISTEN_DRIVETRAIN = (1 << 0);
    public static final byte LISTEN_WHISKERS = (1 << 1);
}
