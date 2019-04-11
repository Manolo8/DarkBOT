package com.github.manolo8.darkbot.modules;

import com.github.manolo8.darkbot.Main;
import com.github.manolo8.darkbot.config.Config;
import com.github.manolo8.darkbot.core.entities.Box;
import com.github.manolo8.darkbot.core.itf.Module;
import com.github.manolo8.darkbot.core.manager.HeroManager;
import com.github.manolo8.darkbot.core.manager.PetManager;
import com.github.manolo8.darkbot.core.utils.Drive;

public class LootNCollectorModule implements Module {

    private final LootModule lootModule;
    private final CollectorModule collectorModule;

    private PetManager pet;
    private HeroManager hero;
    private Drive drive;
    private Config config;
    private long tiempo = System.currentTimeMillis();

    public LootNCollectorModule() {
        this.lootModule = new LootModule();
        this.collectorModule = new CollectorModule();
    }

    @Override
    public void install(Main main) {
        lootModule.install(main);
        collectorModule.install(main);

        this.pet = main.guiManager.pet;
        this.hero = main.hero;
        this.drive = main.hero.drive;
        this.config = main.config;
    }

    @Override
    public String status() {
        return "Loot: " + lootModule.status() + " - Collect: " + collectorModule.status();
    }

    @Override
    public boolean canRefresh() {

        if(collectorModule.isNotWaiting()) {
            return lootModule.canRefresh();
        }

        return false;
    }

    @Override
    public void tick() {
        if (collectorModule.isNotWaiting() && lootModule.checkDangerousAndCurrentMap()) {
            pet.setEnabled(true);

            if (lootModule.findTarget()) {

                collectorModule.findBox();

                Box box = collectorModule.current;

                if (box == null || box.locationInfo.distance(hero) > config.LOOT_COLLECT.RADIUS
                        || lootModule.target.health.hpPercent() < 0.25) {
                    lootModule.moveToAnSafePosition();
                } else {
                    collectorModule.tryCollectNearestBox();
                }

                lootModule.doKillTargetTick();

            } else {
                hero.roamMode();
                collectorModule.findBox();

                if (!collectorModule.tryCollectNearestBox() && (!drive.isMoving() || drive.isOutOfMap())) {
                    drive.moveRandom();
                }

            }

        }

    }
}
