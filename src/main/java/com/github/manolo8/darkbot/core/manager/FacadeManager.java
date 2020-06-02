package com.github.manolo8.darkbot.core.manager;

import com.github.manolo8.darkbot.Main;
import com.github.manolo8.darkbot.core.BotInstaller;
import com.github.manolo8.darkbot.core.itf.Manager;
import com.github.manolo8.darkbot.core.itf.Updatable;
import com.github.manolo8.darkbot.core.objects.facades.BoosterProxy;
import com.github.manolo8.darkbot.core.objects.facades.ChatProxy;
import com.github.manolo8.darkbot.core.objects.facades.EscortProxy;
import com.github.manolo8.darkbot.core.objects.facades.EternalGateProxy;
import com.github.manolo8.darkbot.core.objects.facades.LogMediator;
import com.github.manolo8.darkbot.core.objects.facades.SlotBarsProxy;
import com.github.manolo8.darkbot.core.objects.facades.StatsProxy;
import com.github.manolo8.darkbot.core.objects.swf.PairArray;

import java.util.ArrayList;
import java.util.List;

import static com.github.manolo8.darkbot.Main.API;

public class FacadeManager implements Manager {
    private final Main main;
    private final PairArray commands          = PairArray.ofArray();
    private final PairArray proxies           = PairArray.ofArray();
    private final PairArray mediators         = PairArray.ofArray();
    private final List<Updatable> updatables  = new ArrayList<>();

    public final LogMediator log              = registerMediator("LogWindowMediator", new LogMediator());
    public final ChatProxy chat               = registerProxy("ChatProxy", new ChatProxy());
    public final StatsProxy stats             = registerProxy("StatsProxy", new StatsProxy());
    public final EscortProxy escort           = registerProxy("payload_escort", new EscortProxy());
    public final BoosterProxy booster         = registerProxy("BoosterProxy", new BoosterProxy());
    public final SlotBarsProxy slotBars       = registerProxy("ItemsControlMenuProxy", new SlotBarsProxy());
    public final EternalGateProxy eternalGate = registerProxy("eternal_gate", new EternalGateProxy());

    public FacadeManager(Main main) {
        this.main = main;
    }

    public <T extends Updatable> T registerCommand(String key, T command) {
        @SuppressWarnings("UnnecessaryLocalVariable")
        Updatable fix = command; // Workaround for a java compiler assertion bug having issues with types
        this.commands.addLazy(key, fix::update);
        updatables.add(fix);
        return command;
    }

    public <T extends Updatable> T registerProxy(String key, T proxy) {
        @SuppressWarnings("UnnecessaryLocalVariable")
        Updatable fix = proxy; // Workaround for a java compiler assertion bug having issues with types
        this.proxies.addLazy(key, fix::update);
        updatables.add(fix);
        return proxy;
    }

    public <T extends Updatable> T registerMediator(String key, T mediator) {
        @SuppressWarnings("UnnecessaryLocalVariable")
        Updatable fix = mediator; // Workaround for a java compiler assertion bug having issues with types
        this.mediators.addLazy(key, fix::update);
        updatables.add(fix);
        return mediator;
    }

    @Override
    public void install(BotInstaller botInstaller) {
        botInstaller.mainAddress.add(mainAddr -> {
            long facade = API.readMemoryLong(mainAddr + 544);

            commands.update(API.readMemoryLong(facade, 0x28, 0x20));
            proxies.update(API.readMemoryLong(facade, 0x38, 0x30));
            mediators.update(API.readMemoryLong(facade, 0x40, 0x38));
        });
    }

    public void tick() {
        commands.update();
        proxies.update();
        mediators.update();

        updatables.forEach(Updatable::update);
    }
}
