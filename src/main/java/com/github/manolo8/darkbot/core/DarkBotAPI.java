package com.github.manolo8.darkbot.core;

import com.github.manolo8.darkbot.config.Config;
import com.github.manolo8.darkbot.utils.Time;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class DarkBotAPI implements IDarkBotAPI {

    private static final User32 USER_32 = User32.INSTANCE;
    private volatile WinDef.HWND window;
    private final Config config;

    public DarkBotAPI(Config config) {
        this.config = config;
    }

    static {
        System.loadLibrary("DarkBot");
    }

    public void createWindow() {
        new Thread(() -> {
            createWindow0();
            System.exit(0);
        }, "BotBrowser").start();
        new Thread(() -> {
            while ((window = USER_32.FindWindow("DarkBrowser", "DarkBrowser")) == null || !USER_32.IsWindow(window)) Time.sleep(100);
        }).start();
    }

    private native void createWindow0();

    public native boolean isValid();

    public native void mousePress(int x, int y);

    public native void mouseMove(int x, int y);

    public native void mouseRelease(int x, int y);

    public native void mouseClick(int x, int y);

    public native void keyboardClick(char btn);

    public void keyboardClick(Character ch) {
        if (ch != null) keyboardClick((char) ch);
    }

    public native double readMemoryDouble(long address);

    public native long readMemoryLong(long address);

    public native int readMemoryInt(long address);

    public native boolean readMemoryBoolean(long address);

    public String readMemoryString(long address) {

        int flags = readMemoryInt(address + 36);
        int width = (flags & 0x00000001);
        int size = readMemoryInt(address + 32) << width;
        int type = (flags & 0x00000006) >> 1;

        if (size > 1024 || size < 0) return "ERROR";

        byte[] bytes;

        if (type == 2)
            bytes = readMemory(readMemoryLong(readMemoryLong(address + 24) + 16) + readMemoryInt(address + 16), size);
        else
            bytes = readMemory(readMemoryLong(address + 16), size);

        return width == 0 ? new String(bytes, StandardCharsets.ISO_8859_1) : new String(bytes, StandardCharsets.UTF_16LE);
    }

    public native byte[] readMemory(long address, int length);

    public native void writeMemoryDouble(long address, double value);

    public native void writeMemoryLong(long address, long value);

    public native void writeMemoryInt(long address, int value);

    public native long[] queryMemoryInt(int value, int maxQuantity);

    public native long[] queryMemoryLong(long value, int maxQuantity);

    public native long[] queryMemory(byte[] query, int maxQuantity);

    public native int[] pixels(int x, int y, int w, int h);

    private JFrame imgDisplay;
    public int[] pixelsAndDisplay(int x, int y, int w, int h) {
        int[] pix = pixels(x, y, w, h);

        if (config.MISCELLANEOUS.DEV_STUFF && pix != null) {
            if (imgDisplay == null) imgDisplay = new JFrame();
            if (!imgDisplay.isVisible()) imgDisplay.setVisible(true);
            BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
            for (int i = 0 ; i < pix.length ; i++ ) img.setRGB(i % w, i / w, pix[i]);
            imgDisplay.getContentPane().removeAll();
            imgDisplay.getContentPane().add(new JLabel(new ImageIcon(img)));
            imgDisplay.pack();
        }
        return pix;
    }

    private int x, y, w, h;
    public void setVisible(boolean visible) {
        if (!visible) {
            WinDef.RECT rect = new WinDef.RECT();
            USER_32.GetWindowRect(window, rect);
            x = rect.left;
            y = rect.top;
            w = Math.abs(rect.right - rect.left);
            h = Math.abs(rect.bottom - rect.top);
        }
        int minX = Arrays.stream(GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices())
                .mapToInt(g -> g.getDefaultConfiguration().getBounds().x).min().orElse(0);

        USER_32.MoveWindow(window, visible ? x : minX - w, y, w, h, true);
        if (visible) USER_32.SetForegroundWindow(window);
    }

    public void handleRefresh() {
        USER_32.SetForegroundWindow(window);
        refresh();
    }

    public native void refresh();

}
