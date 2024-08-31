package com.compdog.rover.control.rover_control.util;

public class RollingBuffer {

    private final int window;
    private final long[] array;

    private int offset;
    private int count;

    public RollingBuffer(int window) {
        this.window = window;
        offset = 0;
        count = 0;
        array = new long[window];
    }

    public int getWindow() {
        return window;
    }

    public void push(long value) {
        array[offset] = value;
        offset = (offset + 1) % window;
        if (count < window)
            count++;
    }

    public long at(int index) {
        return array[(index + offset) % window];
    }

    public long getAverage() {
        if (count == 0)
            return 0;

        long avg = 0;

        for (int i = 0; i < count; i++) {
            avg += at(i);
        }

        avg /= count;

        return avg;
    }

    public long getGap() {
        if (count == 0)
            return 0;

        long min = Long.MAX_VALUE;
        long max = Long.MIN_VALUE;

        for (int i = 0; i < count; i++) {
            long v = at(i);
            if (v < min)
                min = v;
            if (v > max)
                max = v;
        }

        return max - min;
    }
}