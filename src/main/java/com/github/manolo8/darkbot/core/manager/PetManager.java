package com.github.manolo8.darkbot.core.manager;

import com.github.manolo8.darkbot.Main;
import com.github.manolo8.darkbot.config.NpcExtra;
import com.github.manolo8.darkbot.config.NpcInfo;
import com.github.manolo8.darkbot.config.types.suppliers.PetGearSupplier;
import com.github.manolo8.darkbot.core.entities.Npc;
import com.github.manolo8.darkbot.core.entities.Pet;
import com.github.manolo8.darkbot.core.entities.Ship;
import com.github.manolo8.darkbot.core.itf.UpdatableAuto;
import com.github.manolo8.darkbot.core.objects.Gui;
import com.github.manolo8.darkbot.core.objects.swf.ObjArray;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import static com.github.manolo8.darkbot.Main.API;

public class PetManager extends Gui {

    private static final int MAIN_BUTTON_X = 30,
            MODULES_X_MAX = 260,
            MODULE_Y = 135,
            MODULE_Y_OFFSET = 17,
            MODULE_HEIGHT = 23,
            SUBMODULE_HEIGHT = 22;

    private final Main main;
    private final List<Ship> ships;
    private final Pet pet;

    private long togglePetTime, selectModuleTime;
    private long activeUntil;
    private Ship target;
    private boolean enabled = false;

    private final ObjArray gearsArr = ObjArray.ofArrObj();
    private final List<Gear> gearList = new ArrayList<>();

    private final ObjArray locatorWrapper = ObjArray.ofArrObj(), locatorNpcList = ObjArray.ofArrObj();
    private final List<Gear> locatorList = new ArrayList<>();

    private final ObjArray petBuffsSpriteArray = ObjArray.ofSprite();
    private final List<Integer> petBuffsIds = new ArrayList<>();

    private ModuleStatus selection = ModuleStatus.NOTHING;
    private Gear currentModule;   // The Module used, like Passive mode, kamikaze, or enemy locator
    private Gear currentSubModule;// The submodule used, like an npc inside enemy locator.
    private long validUntil;
    private NpcInfo selectedNpc;

    private Integer gearOverride = null;
    private long gearOverrideTime = 0;
    private boolean petRepaired;

    private enum ModuleStatus {
        NOTHING,
        DROPDOWN,
        SUB_DROPDOWN,
        SELECTED
    }

    PetManager(Main main) {
        this.main = main;
        this.ships = main.mapManager.entities.ships;
        this.pet = main.hero.pet;

        PetGearSupplier.GEARS = gearList;
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
        int moduleId = main.config.PET.MODULE_ID;
        if (main.config.PET.COMPATIBILITY_MODE && main.config.PET.MODULE < gearList.size()) {
            moduleId = gearList.get(main.config.PET.MODULE).id;
        }

        if (gearOverrideTime > System.currentTimeMillis() && gearOverride != null) {
            moduleId = gearOverride;
        }

        if (target != null && !(target instanceof Npc) && target.playerInfo.isEnemy()) {
            moduleId = PetGearSupplier.Gears.PASSIVE.getId();
        }

        int submoduleId = -1, submoduleIdx = -1;
        if (moduleId == PetGearSupplier.Gears.ENEMY_LOCATOR.getId()) {
            NpcPick submodule = main.config.LOOT.NPC_INFOS.entrySet()
                    .stream()
                    .filter(e -> e.getValue().extra.has(NpcExtra.PET_LOCATOR))
                    .sorted(Comparator.comparingInt(e -> e.getValue().priority))
                    .map(entry -> new NpcPick(entry.getKey(), entry.getValue()))
                    .filter(p -> p.gear != null)
                    .findFirst()
                    .orElse(null);
            if (submodule != null) {
                selectedNpc = submodule.npc;
                submoduleId = submodule.gear.id;
                submoduleIdx = locatorList.indexOf(submodule.gear);
            }
        }
        if (submoduleId == -1) selectedNpc = null;

        if (selection != ModuleStatus.SELECTED
                || (currentModule != null && currentModule.id != moduleId)
                || (currentSubModule == null && submoduleIdx != -1)
                || (currentSubModule != null && currentSubModule.id != submoduleId)) {
            if (show(true)) this.selectModule(moduleId, submoduleIdx);
        } else if (System.currentTimeMillis() > this.selectModuleTime) show(false);
    }

    private class NpcPick {
        private final NpcInfo npc;
        private final Gear gear;
        public NpcPick(String npcName, NpcInfo npc) {
            this.npc = npc;
            String fuzzyName = npcName.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
            this.gear = locatorList.stream().filter(l -> fuzzyName.equals(l.fuzzyName)).findFirst().orElse(null);
        }
    }
    public NpcInfo getTrackedNpc() {
        return selectedNpc;
    }

    private void updatePetTarget() {
        if (target == null || target.removed || !pet.isAttacking(target))
            target = ships.stream().filter(pet::isAttacking).findFirst().orElse(null);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void setOverride(Integer gearId) {
        this.gearOverride = gearId;
        this.gearOverrideTime = System.currentTimeMillis() + 5000;
    }

    private boolean active() {
        if (!pet.removed) activeUntil = System.currentTimeMillis() + 1000;
        return System.currentTimeMillis() < activeUntil;
    }

    private void clickToggleStatus() {
        if (System.currentTimeMillis() - this.togglePetTime > 5000L) {
            click(MAIN_BUTTON_X, MODULE_Y);
            this.selection = ModuleStatus.NOTHING;
            this.togglePetTime = System.currentTimeMillis();
        }
    }

    private void selectModule(int moduleId, int submoduleIdx) {
        if (System.currentTimeMillis() < this.selectModuleTime) return;
        this.selectModuleTime = System.currentTimeMillis() + 750;

        switch (selection) {
            case SELECTED:
            case NOTHING:
                click(MODULES_X_MAX - 5, MODULE_Y);
                selection = ModuleStatus.DROPDOWN;
                break;
            case DROPDOWN:
                if (submoduleIdx != -1) {
                    hover(MODULES_X_MAX - 30, getModuleY(moduleId, true));
                    selection = ModuleStatus.SUB_DROPDOWN;
                } else {
                    click(MODULES_X_MAX - 30, getModuleY(moduleId, true));
                    selection = ModuleStatus.SELECTED;
                }
                break;
            case SUB_DROPDOWN:
                selection = ModuleStatus.SELECTED;
                if (submoduleIdx != -1)
                    click(MODULES_X_MAX + 50, getModuleY(moduleId, false) + (SUBMODULE_HEIGHT * submoduleIdx));
        }

        if (selection == ModuleStatus.SELECTED)
            this.selectModuleTime = System.currentTimeMillis() + 3000;
    }

    private int getModuleY(int moduleId, boolean centered) {
        return MODULE_Y + MODULE_Y_OFFSET + (MODULE_HEIGHT * moduleIdToIndex(moduleId)) +
                (centered ? (MODULE_HEIGHT / 2) : 0);
    }

    private int moduleIdToIndex(int moduleId) {
        for (int i = 0; i < gearList.size(); i++) {
            if (gearList.get(i).id == moduleId) return i;
        }
        return 0;
    }

    @Override
    public void update() {
        super.update();
        if (address == 0) return;

        //update gearsList
        long gearsSprite = getSpriteChild(address, -1); //read gui sprite
        gearsArr.update(API.readMemoryLong(gearsSprite, 176, 224));
        gearsArr.sync(gearList, Gear::new, null);

        //update locator npcs list
        updateLocator(gearsSprite);

        long elementsListAddress = getElementsList(54);
        //update current module
        updateCurrentModule(elementsListAddress);

        //update pet buffs
        updatePetBuffs(elementsListAddress);

        //update petRepaired
        long element = getSpriteElement(elementsListAddress, 67);
        petRepaired = API.readMemoryLong(getSpriteChildWrapper(element, 0), 0x148) == 0;
    }

    // TODO: 01.10.2020 needs more testing.
    public boolean isPetRepaired() {
        return petRepaired;
    }

    public boolean haveBuff(PetBuff buff) {
        return haveBuff(buff.getId());
    }

    public boolean haveBuff(int buffId) {
        return petBuffsIds.contains(buffId);
    }

    private void updatePetBuffs(long elementsListAddress) {
        long temp = getSpriteElement(elementsListAddress, 70);
        temp = getSpriteChild(temp, 0);

        petBuffsIds.clear();
        petBuffsSpriteArray.update(temp);

        petBuffsSpriteArray.forEach(addr -> petBuffsIds.add(API.readMemoryInt(addr, 216 , 168)));
    }

    private void updateCurrentModule(long elementsListAddress) {
        long temp = getSpriteElement(elementsListAddress, 72);
        temp = API.readMemoryLong(getSpriteChild(temp, 0), 176); //get first sprite child then read 176 offset

        long currGearCheck = API.readMemoryLong(getSpriteChild(temp, 1), 152, 16);

        currentModule = findGear(gearList, currGearCheck);
        if (currentModule != null) currentSubModule = null;
        else {
            currentSubModule = findGear(locatorList, currGearCheck);
            if (currentSubModule != null) currentModule = byId(currentSubModule.parentId);
        }
    }

    private void updateLocator(long gearsSprite) {
        locatorWrapper.update(API.readMemoryLong(gearsSprite + 168));

        int oldSize = locatorNpcList.getSize();
        locatorNpcList.update(API.readMemoryLong(locatorWrapper.get(0) + 224));

        // Sometimes the NPC list will be half-updated and there may be way less npcs than before.
        // If we have a recent update and list is smaller, we'll ignore updating for a bit
        if (locatorNpcList.getSize() < oldSize && validUntil > System.currentTimeMillis()) return;

        validUntil = System.currentTimeMillis() + 100;
        locatorNpcList.sync(locatorList, Gear::new, null);
    }

    private void updateGear(Gear module, Gear subModule) {
        currentModule = module;
        currentSubModule = module == null ? null : subModule;
    }

    public Gear findGear(List<Gear> gears, long check) {
        for (Gear gear : gears) if (gear.check == check) return gear;
        return null;
    }

    public Gear byId(int id) {
        for (Gear gear : gearList) if (gear.id == id) return gear;
        return null;
    }

    public static class Gear extends UpdatableAuto {
        public int id, parentId;
        public long check;
        public String name, fuzzyName;

        @Override
        public void update() {
            this.id = API.readMemoryInt(address + 172);
            this.parentId = API.readMemoryInt(address + 176); //assume, -1 if none
            this.name = API.readMemoryString(API.readMemoryLong(address + 200));
            this.fuzzyName = name.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
            this.check = API.readMemoryLong(address, 208, 152, 0x10);
        }
    }

    public enum PetBuff {
        SINGULARITY,
        SPEED_LEECH,
        TRADE,
        WEAKEN_SHIELD,
        KAMIKAZE_CD,
        COMBO_REPAIR_CD,
        FRIENDLY_SACRIFICE,
        RETARGETING_CD,
        HP_LINK_CD,
        MEGA_MINE_CD;

        public int getId() {
            return ordinal() + 1;
        }
    }
}
