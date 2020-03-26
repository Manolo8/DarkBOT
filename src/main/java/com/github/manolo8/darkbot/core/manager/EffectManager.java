package com.github.manolo8.darkbot.core.manager;

import com.github.manolo8.darkbot.core.BotInstaller;
import com.github.manolo8.darkbot.core.entities.Entity;
import com.github.manolo8.darkbot.core.itf.Manager;
import com.github.manolo8.darkbot.core.objects.swf.VectorPtr;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.github.manolo8.darkbot.Main.API;

public class EffectManager implements Manager {
    private long mapAddressStatic;

    private VectorPtr effectsPtr = new VectorPtr();
    private Map<Long, List<Integer>> effects = new HashMap<>();

    @Override
    public void install(BotInstaller botInstaller) {
        botInstaller.screenManagerAddress.add(value -> mapAddressStatic = value + 256);
    }

    public void tick() {
        long addr = API.readMemoryLong(API.readMemoryLong(mapAddressStatic), 128, 48);

        effectsPtr.update(addr);
        effectsPtr.update();
        effects.clear();

        for (int i = 0; i < effectsPtr.size; i++) {
            int id      = API.readMemoryInt( effectsPtr.get(i) + 0x24);
            long entity = API.readMemoryLong(effectsPtr.get(i) + 0x48);

            effects.computeIfAbsent(entity, list -> new ArrayList<>()).add(id);
        }
    }

    public List<Integer> getEffects(Entity entity) {
        return effects.get(entity.address);
    }

    public boolean hasEffect(Entity entity, Effect effect) {
        return hasEffect(entity, effect.id);
    }

    public boolean hasEffect(Entity entity, int effect) {
        return getEffects(entity).contains(effect);
    }

    public enum Effect {
        UNDEFINED(-1), LOCATOR(1), PET_SPAWN(2), ENERGY_LEECH(11), ISH(84),
        STICKY_BOMB(56), POLARITY_POSITIVE(65), POLARITY_NEGATIVE(66);

        private int id;

        Effect(int id) {
            this.id = id;
        }
    }
}
