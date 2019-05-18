package com.github.manolo8.darkbot.core.utils;

import com.github.manolo8.darkbot.config.ZoneInfo;
import com.github.manolo8.darkbot.core.entities.Entity;
import com.github.manolo8.darkbot.core.manager.HeroManager;
import com.github.manolo8.darkbot.core.manager.MapManager;
import com.github.manolo8.darkbot.core.manager.MouseManager;
import com.github.manolo8.darkbot.core.objects.LocationInfo;
import com.github.manolo8.darkbot.core.utils.pathfinder.PathFinder;
import com.github.manolo8.darkbot.core.utils.pathfinder.PathPoint;

import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import static java.lang.Math.random;

public class Drive {

    private static final Random RANDOM = new Random();
    private boolean force = false;

    private final MapManager map;
    private final MouseManager mouse;
    private final HeroManager hero;
    private final LocationInfo heroLoc;

    public PathFinder pathFinder;
    public LinkedList<PathPoint> paths = new LinkedList<>();

    private Location tempDest, endLoc, lastSegment;

    private int clicked;
    public long lastMoved;

    public Drive(HeroManager hero, MapManager map, MouseManager mouse) {
        this.map = map;
        this.mouse = mouse;
        this.hero = hero;
        this.heroLoc = hero.locationInfo;
        this.pathFinder = new PathFinder(map);
    }

    public void checkMove() {
        if (endLoc != null && pathFinder.changed() && tempDest == null) tempDest = endLoc;

        boolean newPath = tempDest != null;
        if (tempDest != null) {
            paths = pathFinder.createRote(heroLoc.now, tempDest);
            tempDest = null;
        }

        if (paths.isEmpty() || !heroLoc.isLoaded())
            return;

        lastMoved = System.currentTimeMillis();

        Location now = heroLoc.now, last = heroLoc.last, next = current();
        if (next == null) return;
        newPath |= !next.equals(lastSegment);
        lastSegment = next;

        boolean diffAngle = Math.abs(now.angle(next) - last.angle(now)) > 0.1;
        if (hero.timeTo(now.distance(next)) > 100) {
            clicked++;
            if (heroLoc.isMoving() && !diffAngle) return;
            if (!force && heroLoc.isMoving() && !newPath && clicked > 3) stop();
            else {
                clicked = 0;
                mouse.holdTowards(next, heroLoc.isMoving());
            }
        } else {
            paths.removeFirst();
            if (paths.isEmpty()) {
                this.endLoc = null;
                stop();
            }
        }
    }

    private Location current() {
        if (paths.isEmpty()) return null;
        PathPoint point = paths.getFirst();
        return new Location(point.x, point.y);
    }

    public boolean canMove(Location location) {
        return !map.isOutOfMap(location.x, location.y) && pathFinder.canMove((int) location.x, (int) location.y);
    }

    public double closestDistance(Location location) {
        PathPoint closest = pathFinder.fixToClosest(new PathPoint((int) location.x, (int) location.y));
        return location.distance(closest.toLocation());
    }

    public double distanceBetween(Location loc, int x, int y) {
        double sum = 0;
        PathPoint begin = new PathPoint((int) loc.x, (int) loc.y);
        for (PathPoint curr : pathFinder.createRote(begin, new PathPoint(x, y)))
            sum += Math.sqrt(Math.pow(begin.x - curr.x, 2) + Math.pow(begin.y - curr.y, 2));
        return sum;
    }

    public void toggleRunning(boolean running) {
        this.force = running;
        stop();
    }

    public void stop() {
        mouse.release();

        endLoc = null;
        if (!paths.isEmpty()) paths.clear();
    }

    public void clickCenter(boolean single, Location aim) {
        mouse.clickCenter(single, aim);
    }

    public void move(Entity entity) {
        move(entity.locationInfo.now);
    }

    public void move(Location location) {
        move(location.x, location.y);
    }

    public void move(double x, double y) {
        tempDest = endLoc = new Location(x, y);
    }

    public void moveRandom() {
        ZoneInfo area = map.preferred;
        List<ZoneInfo.Zone> zones = area.getZones();
        if (zones.isEmpty()) {
            move(random() * MapManager.internalWidth, random() * MapManager.internalHeight);
        } else {
            ZoneInfo.Zone zone = zones.get(RANDOM.nextInt(zones.size()));
            double cellSize = 1d / area.resolution;
            double xProportion = (zone.x / (double) area.resolution) + random() * cellSize,
                    yProportion = (zone.y / (double) area.resolution) + random() * cellSize;

            move(xProportion * MapManager.internalWidth, yProportion * MapManager.internalHeight);
        }
    }

    public boolean isMoving() {
        return !paths.isEmpty() || heroLoc.isMoving();
    }

    public Location movingTo() {
        return endLoc == null ? heroLoc.now.copy() : endLoc.copy();
    }

    public boolean isOutOfMap() {
        return map.isOutOfMap(heroLoc.now.x, heroLoc.now.y);
    }
}
