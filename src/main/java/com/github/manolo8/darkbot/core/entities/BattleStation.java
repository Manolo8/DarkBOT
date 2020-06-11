package com.github.manolo8.darkbot.core.entities;

import com.github.manolo8.darkbot.config.ConfigEntity;
import com.github.manolo8.darkbot.core.itf.Obstacle;
import com.github.manolo8.darkbot.core.objects.PlayerInfo;
import com.github.manolo8.darkbot.core.utils.pathfinder.Area;

import static com.github.manolo8.darkbot.Main.API;

public class BattleStation
        extends Entity
        implements Obstacle {

    public PlayerInfo info = new PlayerInfo();
    public Area area = new Area(0, 0, 0, 0);
    public int hullId;

    public BattleStation(int id, long address) {
        super(id);
        this.update(address);
    }

    @Override
    public void update() {
        super.update();

        info.update();
        if (locationInfo.isMoving()) {
            area.set(locationInfo.now, 1200, 1000);
            ConfigEntity.INSTANCE.updateSafetyFor(this);
        }
    }

    @Override
    public void removed() {
        super.removed();
        ConfigEntity.INSTANCE.updateSafetyFor(this);
    }

    @Override
    public void update(long address) {
        super.update(address);

        hullId = API.readMemoryInt(address + 116);
        info.update(API.readMemoryLong(address + 120));
    }

    @Override
    public Area getArea() {
        return area;
    }

    @Override
    public boolean isRemoved() {
        return removed;
    }

    @Override
    public boolean use() {
        return hullId > 0 && hullId < 255 && info.isEnemy();
    }

    @Override
    public String toString() {
        return id + "," + hullId;
    }
}
