package com.github.manolo8.darkbot;

import com.bulenkov.darcula.DarculaLaf;
import com.github.manolo8.darkbot.config.Config;
import com.github.manolo8.darkbot.config.ConfigEntity;
import com.github.manolo8.darkbot.core.BotInstaller;
import com.github.manolo8.darkbot.core.DarkBotAPI;
import com.github.manolo8.darkbot.core.IDarkBotAPI;
import com.github.manolo8.darkbot.core.itf.Module;
import com.github.manolo8.darkbot.core.manager.GuiManager;
import com.github.manolo8.darkbot.core.manager.HeroManager;
import com.github.manolo8.darkbot.core.manager.MapManager;
import com.github.manolo8.darkbot.core.manager.MouseManager;
import com.github.manolo8.darkbot.core.manager.PingManager;
import com.github.manolo8.darkbot.core.manager.StarManager;
import com.github.manolo8.darkbot.core.manager.StatsManager;
import com.github.manolo8.darkbot.core.utils.Lazy;
import com.github.manolo8.darkbot.gui.MainGui;
import com.github.manolo8.darkbot.gui.utils.SystemUtils;
import com.github.manolo8.darkbot.modules.CollectorModule;
import com.github.manolo8.darkbot.modules.EventModule;
import com.github.manolo8.darkbot.modules.LootModule;
import com.github.manolo8.darkbot.modules.LootNCollectorModule;
import com.github.manolo8.darkbot.modules.MapModule;
import com.github.manolo8.darkbot.utils.ByteArrayToBase64TypeAdapter;
import com.github.manolo8.darkbot.utils.ReflectionUtils;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.util.prefs.Preferences;

public class Main extends Thread {
    public static final String VERSION = "1.14 alpha";

    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .setLenient()
            .registerTypeHierarchyAdapter(byte[].class, new ByteArrayToBase64TypeAdapter()).create();

    public static final Object UPDATE_LOCKER = new Object();

    public static IDarkBotAPI API;

    public final StarManager starManager;
    public final MapManager mapManager;
    public final MouseManager mouseManager;
    public final HeroManager hero;
    public final GuiManager guiManager;
    public final StatsManager statsManager;
    public final PingManager pingManager;
    public final BackpageManager backpage;

    public final Lazy.Sync<Boolean> status;

    public Config config;
    private boolean failedConfig;
    private int moduleId;
    public Module module;

    public long lastRefresh;

    private final BotInstaller botInstaller;
    private final MainGui form;

    public double avgTick;

    private volatile boolean running;
    public boolean tickingModule;

    public Main() {
        super("Main");
        this.config = new Config();
        loadConfig();

        API = new DarkBotAPI(config);
        if (config.MISCELLANEOUS.FULL_DEBUG)
            API = (IDarkBotAPI) Proxy.newProxyInstance(Main.class.getClassLoader(), new Class[]{IDarkBotAPI.class}, IDarkBotAPI.getLoggingHandler((DarkBotAPI) API));

        if (config.MISCELLANEOUS.DISPLAY.USE_DARCULA_THEME) {
            try {
                UIManager.setLookAndFeel(new DarculaLaf());
            } catch (UnsupportedLookAndFeelException e) {
                e.printStackTrace();
            }
        }
        showDiscordWarning();

        if (failedConfig) popupMessage("Failed to load config",
                "Default config will be used, config won't be save.", JOptionPane.ERROR_MESSAGE);
        new ConfigEntity(config);

        botInstaller = new BotInstaller();
        status = new Lazy.Sync<>();

        starManager = new StarManager();
        mapManager = new MapManager(this);
        mouseManager = new MouseManager(mapManager);
        hero = new HeroManager(this);
        guiManager = new GuiManager(this);
        statsManager = new StatsManager(this);
        pingManager = new PingManager();

        botInstaller.add(guiManager);
        botInstaller.add(mapManager);
        botInstaller.add(hero);
        botInstaller.add(statsManager);
        botInstaller.add(pingManager);

        botInstaller.init();

        botInstaller.invalid.add(value -> {
            if (!value) lastRefresh = System.currentTimeMillis();
        });

        status.add(this::onRunningToggle);
        checkModule();

        form = new MainGui(this);
        backpage = new BackpageManager(this);

        start();
        API.createWindow();
    }

    private void showDiscordWarning() {
        Preferences prefs = Preferences.userNodeForPackage(getClass());
        long firstInit = prefs.getLong(VERSION, System.currentTimeMillis());
        prefs.putLong(VERSION, firstInit);

        long TIME_BEFORE_MESSAGE = 48 * 60 * 60 * 1000;
        if (System.currentTimeMillis() - firstInit < TIME_BEFORE_MESSAGE) return;

        JPanel panel = new JPanel(new MigLayout("ins 0, wrap 1"));
        panel.add(new JLabel("This bot is free, if you paid for it or watched ads, you were scammed!"));
        panel.add(new JLabel("Make sure you are in the official discord server to get latest updates for free."));
        JCheckBox dontShow = new JCheckBox("Don't show this message again");
        panel.add(dontShow);

        JButton join = new JButton("Join discord");
        join.addActionListener(e -> {
            SystemUtils.openUrl("https://discord.gg/KFd8vZT");
            if (dontShow.isSelected()) prefs.putLong(VERSION, Long.MAX_VALUE);
            JOptionPane.getRootFrame().dispose();
        });
        JButton ignore = new JButton("Ignore");
        ignore.addActionListener(e -> {
            if (dontShow.isSelected()) prefs.putLong(VERSION, Long.MAX_VALUE);
            JOptionPane.getRootFrame().dispose();
        });

        JOptionPane.showOptionDialog(null, panel, "Join the official discord!",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.INFORMATION_MESSAGE, null,
                new Object[]{join, ignore}, join);
    }

    @Override
    public void run() {
        long time;

        while (true) {
            time = System.currentTimeMillis();

            tick();

            double tickTime = System.currentTimeMillis() - time;
            avgTick = ((avgTick * 4) + tickTime) / 5;
            sleepMax(time, botInstaller.invalid.value ? 1000 : 100);
        }
    }

    private void tick() {
        status.tick();

        if (isInvalid())
            invalidTick();
        else
            validTick();

        form.tick();

        checkConfig();
        checkModule();
    }

    private boolean isInvalid() {
        return botInstaller.isInvalid();
    }

    private void invalidTick() {
        tickingModule = false;
        botInstaller.verify();
    }

    private void validTick() {
        guiManager.tick();
        hero.tick();
        mapManager.tick();
        statsManager.tick();

        tickingModule = running && guiManager.canTickModule();
        if (tickingModule) tickRunning();

        pingManager.tick();
        //if (config.MISCELLANEOUS.DEV_STUFF && mapManager.width > 0)
        //    API.pixelsAndDisplay(0, 0, (int) mapManager.width, (int) mapManager.height);
    }

    private void tickRunning() {
        guiManager.pet.tick();
        module.tick();
        checkPetBug();
        checkRefresh();
    }

    private void checkConfig() {
        if (config.changed) {
            config.changed = false;
            saveConfig();
        }
    }

    private void checkPetBug() {
        if (mapManager.isTarget(hero.pet)) hero.pet.clickable.setRadius(0);
    }

    private void checkRefresh() {
        if (config.MISCELLANEOUS.REFRESH_TIME == 0 ||
                System.currentTimeMillis() - lastRefresh < config.MISCELLANEOUS.REFRESH_TIME * 60 * 1000) return;

        if (!module.canRefresh()) return;
        API.handleRefresh();
        lastRefresh = System.currentTimeMillis();
    }

    public <A extends Module> A setModule(A module) {
        module.install(this);
        this.module = module;
        return module;
    }

    public void setRunning(boolean running) {
        if (this.running == running) return;
        status.send(running);
        this.running = running;
    }

    private void onRunningToggle(boolean running) {
        lastRefresh = System.currentTimeMillis();
        if (running && module instanceof MapModule) {
            moduleId = -1;
            checkModule();
        }
    }

    private void loadConfig() {
        File config = new File("config.json");
        if (!config.exists()) {
            saveConfig();
            return;
        }
        try (InputStreamReader reader = new InputStreamReader(new FileInputStream(config), StandardCharsets.UTF_8)) {
            this.config = GSON.fromJson(reader, Config.class);
            if (this.config == null) this.config = new Config();
        } catch (Exception e) {
            failedConfig = true;
            e.printStackTrace();
        }
    }

    public void saveConfig() {
        if (failedConfig) return; // Don't save defaults if config failed to load!
        File config = new File("config.json");
        try (OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(config), StandardCharsets.UTF_8)) {
            GSON.toJson(this.config, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean isRunning() {
        return running;
    }

    private void checkModule() {
        if (module == null || moduleId != config.GENERAL.CURRENT_MODULE)
            setModule(getModule(moduleId = config.GENERAL.CURRENT_MODULE));
    }

    private Module getModule(int id) {
        switch (id) {
            case 0: return new CollectorModule();
            case 1: return new LootModule();
            case 2: return new LootNCollectorModule();
            case 3: return new EventModule();
            case 4: {
                try {
                    Module m = getCustomModule();
                    if (m != null) {
                        popupMessage("Success", "Successfully loaded custom module",  JOptionPane.INFORMATION_MESSAGE);
                        return m;
                    }
                    popupMessage("Warning", "Couldn't load custom module, did you configure it correctly?", JOptionPane.WARNING_MESSAGE);
                } catch (Exception e) {
                    popupMessage("Error compiling module", e.getMessage(), JOptionPane.ERROR_MESSAGE);
                    e.printStackTrace();
                }
            }
            default: return new CollectorModule();
        }
    }

    private void popupMessage(String title, String content, int type) {
        SwingUtilities.invokeLater(() -> {
            JOptionPane pane = new JOptionPane(content, type);
            JDialog dialog = pane.createDialog(title);
            dialog.setAlwaysOnTop(true);
            dialog.setVisible(true);
        });
    }

    private Module getCustomModule() throws Exception {
        String customModule = config.GENERAL.CUSTOM_MODULE;
        if (customModule == null || customModule.isEmpty()) return null;
        File file = new File(customModule);
        if (!file.exists()) return null;
        Class<?> newModule = ReflectionUtils.compileModule(file);
        return (Module) ReflectionUtils.createInstance(newModule);
    }

    private void sleepMax(long time, int total) {
        time = System.currentTimeMillis() - time;

        if (time < total) {
            try {
                Thread.sleep(total - time);
            } catch (InterruptedException ignored) {
            }
        }
    }
}
