package com.github.manolo8.darkbot.core.objects.swf.group;

import com.github.manolo8.darkbot.core.itf.Updatable;

import static com.github.manolo8.darkbot.Main.API;

public class MemberInfo extends Updatable {
    public int shipType;
    public int hp;
    public int maxHp;
    public int hull;
    public int maxHull;
    public int shield;
    public int maxShield;
    public String userName;

    @Override
    public void update() {
        shipType  = API.readMemoryInt(address + 0x20);
        hp        = API.readMemoryInt(address + 0x24);
        maxHp     = API.readMemoryInt(address + 0x28);
        hull      = API.readMemoryInt(address + 0x2C);
        maxHull   = API.readMemoryInt(address + 0x30);
        shield    = API.readMemoryInt(address + 0x34);
        maxShield = API.readMemoryInt(address + 0x38);
        userName  = API.readMemoryString(API.readMemoryLong(address + 0x40));
    }
}
