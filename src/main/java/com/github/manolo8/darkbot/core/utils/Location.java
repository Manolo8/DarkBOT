package com.github.manolo8.darkbot.core.utils;

import static java.lang.Math.pow;
import static java.lang.Math.sqrt;
import static java.lang.StrictMath.atan2;
import static java.lang.StrictMath.sin;
import static java.lang.StrictMath.cos;

public class Location {

    public double x;
    public double y;

    public Location() {
    }

    public Location(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public static Location of(Location loc, double angle, double distance) {
        return new Location(loc.x - cos(angle) * distance, loc.y - sin(angle) * distance);
    }

    public double distance(double ox, double oy) {
        return sqrt(pow(x - ox, 2) + pow(y - oy, 2));
    }

    public double distance(Location o) {
        return sqrt(pow(x - o.x, 2) + pow(y - o.y, 2));
    }

    public double angle(Location o) {
        return atan2(y - o.y, x - o.x);
    }

    public Location toAngle(Location center, double angle, double distance) {
        this.x = center.x - cos(angle) * distance;
        this.y = center.y - sin(angle) * distance;
        return this;
    }

    public Location set(double x, double y) {
        this.x = x;
        this.y = y;
        return this;
    }

    public Location copy() {
        return new Location(x, y);
    }

    @Override
    public String toString() {
        return x + "," + y;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Location) {
            Location location = (Location) obj;

            return location.x == x && location.y == y;
        }

        return false;
    }
}
