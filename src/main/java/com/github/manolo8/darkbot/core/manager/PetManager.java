package com.github.manolo8.darkbot.core.manager;

import com.github.manolo8.darkbot.Main;
import com.github.manolo8.darkbot.core.entities.Npc;
import com.github.manolo8.darkbot.core.entities.Pet;
import com.github.manolo8.darkbot.core.entities.Ship;
import com.github.manolo8.darkbot.core.objects.Gui;
import com.github.manolo8.darkbot.core.objects.swf.VectorPtr;

import java.util.List;

import static com.github.manolo8.darkbot.Main.API;

public class PetManager extends Gui {

    private static final int MAIN_BUTTON_X = 30, MODULES_X_MAX = 260, MODULE_Y = 120;

    private long togglePetTime, selectModuleTime;
    private long activeUntil;
    private int moduleStatus = -2; // -2 no module, -1 selecting module, >= 0 module selected
    private Main main;
    private List<Ship> ships;
    private Ship target;
    private Pet pet;
    private boolean enabled = false;

    PetManager(Main main) {
        this.main = main;
        this.ships = main.mapManager.entities.ships;
        this.pet = main.hero.pet;
    }

    public void tick() {
        if (!main.isRunning() || !main.config.PET.ENABLED) return;
        if (active() != enabled) {
            if (show(true)) clickToggleStatus();
            return;
        }
        if (!enabled) {
            show(false);
            return;
        }
        updatePetTarget();
        int module = (target == null || target instanceof Npc || target.playerInfo.isEnemy()) ? main.config.PET.MODULE : 0;
        if (moduleStatus != module && show(true)) this.selectModule(module);
        else if (moduleSelected()) show(false);
    }

    private void updatePetTarget() {
        if (target == null || target.removed || !pet.isAttacking(target))
            target = ships.stream().filter(s -> pet.isAttacking(s)).findFirst().orElse(null);
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isEnabled() {
        return enabled;
    }

    private boolean active() {
        if (!pet.removed) activeUntil = System.currentTimeMillis() + 1000;
        return System.currentTimeMillis() < activeUntil;
    }

    private boolean moduleSelected() {
        return System.currentTimeMillis() - this.selectModuleTime > 1000L;
    }

    private void clickToggleStatus() {
        if (System.currentTimeMillis() - this.togglePetTime > 5000L) {
            click(MAIN_BUTTON_X, MODULE_Y);
            this.moduleStatus = -2;
            this.togglePetTime = System.currentTimeMillis();
        }
    }

    private void selectModule(int module) {
        if (System.currentTimeMillis() - this.selectModuleTime > 1000L) {
            if (moduleStatus != -1) {
                click(MODULES_X_MAX - 5, MODULE_Y);
                this.moduleStatus = -1;
            } else {
                click(MODULES_X_MAX - 30, MODULE_Y + 40 + (20 * module));
                this.moduleStatus = module;
            }
            this.selectModuleTime = System.currentTimeMillis();
        }
    }

    VectorPtr petPtr = VectorPtr.ofPet();
    VectorPtr modules = VectorPtr.ofPetCheck();
    private void petModules() {
        petPtr.update(API.readMemoryLong(API.readMemoryLong(address + 72) + 64));
        petPtr.update();

        modules.update(API.readMemoryLong(API.readMemoryLong(API.readMemoryLong(petPtr.get(petPtr.size - 1) + 216) + 176) + 224));
        modules.update();

        long currentModule = currentModule();

        for (int i = 0; i < modules.size; i++) {
            int gearId = API.readMemoryInt(modules.get(i) + 172);
            String gearName = API.readMemoryString(API.readMemoryLong(modules.get(i) + 200));
            long checkAddr = API.readMemoryLong(API.readMemoryLong(modules.get(i) + 208) + 152);

            if (currentModule != checkAddr) continue;

            System.out.println("gearId = " + gearId);
            System.out.println("gearName = " + gearName);
            System.out.println("checkAddr = " + checkAddr);
        }
    }

    VectorPtr ptr = VectorPtr.ofPetCheck();
    VectorPtr currentModule = VectorPtr.ofPet();
    private long currentModule() {
        long temp = API.readMemoryLong(address + 400);
        ptr.update(temp);
        ptr.update();

        for (int i = 0; i < ptr.size; i++) {
            if (API.readMemoryInt(ptr.get(i) + 172) == 54) {
                temp = ptr.get(i);
                break;
            }
        }

        ptr.update(API.readMemoryLong(temp + 184));
        ptr.update();

        for (int i = 0; i < ptr.size; i++) {
            if (API.readMemoryInt(ptr.get(i) + 168) == 72) {
                temp = ptr.get(i);
                break;
            }
        }

        currentModule.update(API.readMemoryLong(API.readMemoryLong(temp + 72) + 64));
        currentModule.update();
        temp = API.readMemoryLong(API.readMemoryLong(currentModule.get(0) + 216) + 176);

        currentModule.update(API.readMemoryLong(API.readMemoryLong(temp + 72) + 64));
        currentModule.update();

        return API.readMemoryLong(API.readMemoryLong(currentModule.get(1) + 216) + 152);
    }
}
