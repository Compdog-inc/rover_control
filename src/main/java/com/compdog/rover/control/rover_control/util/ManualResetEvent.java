package com.compdog.rover.control.rover_control.util;

/**
 * From <a href="https://stackoverflow.com/a/12941753">https://stackoverflow.com/a/12941753</a>
 */

public  class ManualResetEvent {
    private final Object monitor = new Object();
    private volatile boolean open;

    public ManualResetEvent(boolean open) {
        this.open = open;
    }

    public void waitOne() throws InterruptedException {
        synchronized (monitor) {
            while (!open) {
                monitor.wait();
            }
        }
    }

    public boolean waitOne(long timeout) throws InterruptedException {
        synchronized (monitor) {
            if (!open) {
                monitor.wait(timeout);
            }

            return open;
        }
    }

    public void set() {//open start
        synchronized (monitor) {
            open = true;
            monitor.notifyAll();
        }
    }

    public void reset() {//close stop
        synchronized (monitor) {
            open = false;
        }
    }
}