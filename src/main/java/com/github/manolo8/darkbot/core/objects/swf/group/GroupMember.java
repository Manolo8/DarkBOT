package com.github.manolo8.darkbot.core.objects.swf.group;

import com.github.manolo8.darkbot.core.itf.Updatable;
import com.github.manolo8.darkbot.core.utils.Location;

import static com.github.manolo8.darkbot.Main.API;

public class GroupMember extends Updatable {
    public Location location = new Location();
    public MemberInfo memberInfo = new MemberInfo();
    public MemberInfo targetInfo = new MemberInfo();

    public int id;
    public int factionId;
    public int level;
    public int mapId;
    public boolean isBoss;     // is group boss
    public boolean isAttacked; // is attacked by something
    public boolean isSelected; // is selected by hero
    public String userName;

    @Override
    public void update() {
        location.set(API.readMemoryInt(address + 0x38), API.readMemoryInt(address + 0x3C));
        memberInfo.update(API.readMemoryLong(address + 0x78));
        targetInfo.update(API.readMemoryLong(address + 0x80));
        memberInfo.update();
        targetInfo.update();

        id         = API.readMemoryInt(address + 0x20);
        factionId  = API.readMemoryInt(address + 0x24);
        level      = API.readMemoryInt(address + 0x28);
        mapId      = API.readMemoryInt(address + 0x34);
        isBoss     = API.readMemoryBoolean(address + 0x50);
        isAttacked = API.readMemoryBoolean(address + 0x44);
        isSelected = API.readMemoryBoolean(address + 0x60);
        userName   = API.readMemoryString(API.readMemoryLong(address + 0x68));
    }
}
