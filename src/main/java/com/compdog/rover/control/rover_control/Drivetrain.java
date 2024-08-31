package com.compdog.rover.control.rover_control;

public class Drivetrain {
    public static class DrivetrainResult {
        double left;
        double right;

        public double getLeft() {
            return left;
        }

        public double getRight() {
            return right;
        }

        public DrivetrainResult(double left, double right) {
            this.left = left;
            this.right = right;
        }
    }

    public static DrivetrainResult Drive(double x, double y) {
        return new DrivetrainResult(x, y);
    }
}
