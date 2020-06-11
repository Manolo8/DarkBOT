package com.github.manolo8.darkbot.core.entities;

import static com.github.manolo8.darkbot.Main.API;

public class Pet extends Ship {

    public Pet() {}

    public Pet(int id, long address) {
        super(id);
        this.update(address);
    }

    @Override
    public void update() {
        super.update();
        id = API.readMemoryInt(address + 56);
    }

    @Override
    public void update(long address) {
        super.update(address);

        clickable.setRadius(0);
    }
}
