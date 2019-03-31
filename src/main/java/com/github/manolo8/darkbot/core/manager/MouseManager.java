package com.github.manolo8.darkbot.core.manager;

import com.github.manolo8.darkbot.Main;
import com.github.manolo8.darkbot.core.utils.ClickPoint;
import com.github.manolo8.darkbot.core.utils.Location;

import static com.github.manolo8.darkbot.Main.API;

public class MouseManager extends Thread {

    private final Object LOCK = new Object();
    private volatile ClickPoint clickPoint = new ClickPoint(0, 0);
    private volatile long address;
    private volatile boolean waiting;

    private final MapManager map;

    public MouseManager(MapManager map) {
        super("MouseClicker");
        this.map = map;
        start();
    }

    public void clickCenter(boolean single, Location aim) {
        ClickPoint clickPoint = pointCenter(aim);
        API.mouseClick(clickPoint.x, clickPoint.y);
        if (!single) API.mouseClick(clickPoint.x, clickPoint.y);
    }

    public void clickLoc(Location loc) {
        clickPoint.set((int) loc.x, (int) loc.y);

        ClickPoint click = pointCenter(loc);

        address = Main.API.readMemoryLong(Main.API.readMemoryLong(map.eventAddress) + 64L);

        waiting = false;
        synchronized (LOCK) {
            LOCK.notifyAll();
        }
        Main.API.mouseClick(click.x, click.y);
        waiting = true;
    }


    private ClickPoint pointCenter(Location aim) {
        Location center = new Location(map.boundX + map.width / 2, map.boundY + map.height / 2);
        center.toAngle(center, center.angle(aim) + Math.random() * 0.2 - 0.1, 125 + Math.random() * 75);
        return pointLoc(center);
    }

    private ClickPoint pointLoc(Location loc) {
        return new ClickPoint((int) ((loc.x - map.boundX) / (map.boundMaxX - map.boundX) * (double) MapManager.clientWidth),
                (int) ((loc.y - map.boundY) / (map.boundMaxY - map.boundY) * (double) MapManager.clientHeight));
    }

    public final void run() {
        while (true) {
            synchronized (this.LOCK) {
                if (waiting) {
                    try {
                        this.LOCK.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                Main.API.writeMemoryDouble(address + 32L, (double) clickPoint.x);
                Main.API.writeMemoryDouble(address + 40L, (double) clickPoint.y);
            }
        }
    }

}