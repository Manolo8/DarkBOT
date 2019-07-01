package com.github.manolo8.darkbot.gui.titlebar;

import com.github.manolo8.darkbot.Main;
import com.github.manolo8.darkbot.gui.utils.PopupMenuListenerAdapter;
import com.github.manolo8.darkbot.gui.utils.UIUtils;
import com.github.manolo8.darkbot.utils.SystemUtils;

import javax.swing.*;
import javax.swing.event.PopupMenuEvent;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;

import static com.github.manolo8.darkbot.Main.API;

public class ExtraButton extends TitleBarButton<JFrame> {

    private JPopupMenu extraOptions = new JPopupMenu("Extra Options");
    private long keepClosed;

    ExtraButton(Main main, JFrame frame) {
        super(UIUtils.getIcon("hamburger"), frame);

        JMenuItem home = new JMenuItem("HOME"),
                reload = new JMenuItem("RELOAD"),
                discord = new JMenuItem("DISCORD"),//, UIUtils.getIcon("discord")),
                copySid = new JMenuItem("COPY SID");

        extraOptions.add(home);
        extraOptions.add(reload);
        extraOptions.add(copySid);
        extraOptions.add(discord);
        home.addActionListener(e -> {
            String sid = main.statsManager.sid, instance = main.statsManager.instance;
            if (sid == null || sid.isEmpty() || instance == null || instance.isEmpty()) return;
            String url = instance + "?dosid=" + sid;
            if ((e.getModifiers() & InputEvent.BUTTON3_MASK) != 0) SystemUtils.toClipboard(url);
            else SystemUtils.openUrl(url);
        });
        reload.addActionListener(e -> API.handleRefresh());
        discord.addActionListener(e -> SystemUtils.openUrl("https://discord.gg/KFd8vZT"));
        copySid.addActionListener(e -> SystemUtils.toClipboard(main.statsManager.sid));

        extraOptions.setBorder(UIUtils.getBorder());

        extraOptions.addPopupMenuListener(new PopupMenuListenerAdapter() {
            @Override
            public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
                keepClosed = System.currentTimeMillis() + 100;
            }
        });
    }

    @Override
    public void mousePressed(MouseEvent e) {
        super.mousePressed(e);
        if (keepClosed > System.currentTimeMillis()) keepClosed = Long.MAX_VALUE;
    }

    @Override
    public void mouseExited(MouseEvent e) {
        super.mouseExited(e);
        keepClosed = 0;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (keepClosed < System.currentTimeMillis()) extraOptions.show(this, getX(), getY() + getHeight() - 1);
        else keepClosed = 0;
    }

}
