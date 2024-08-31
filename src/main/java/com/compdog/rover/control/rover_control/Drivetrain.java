package com.compdog.rover.control.rover_control;

public class Drivetrain {
    public static class DrivetrainResult {
        final double left;
        final double right;

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

    public static DrivetrainResult Drive(double left, double right) {
        return new DrivetrainResult(left, right);
    }
}
