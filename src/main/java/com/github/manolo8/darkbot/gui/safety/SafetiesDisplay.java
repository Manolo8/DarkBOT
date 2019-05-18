package com.github.manolo8.darkbot.gui.safety;

import com.github.manolo8.darkbot.Main;
import com.github.manolo8.darkbot.config.SafetyInfo;
import com.github.manolo8.darkbot.gui.MapDrawer;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Comparator;

class SafetiesDisplay extends MapDrawer {

    private Color ZONE = new Color(0, 255, 128, 32);
    private Color ZONE_HIGHLIGHT = new Color(0, 255, 128, 96);
    private Color ZONE_SELECTED = new Color(0, 255, 128, 192);

    private SafetiesEditor editor;
    private SafetyInfo closest;
    private boolean hovering;

    SafetiesDisplay(SafetiesEditor editor) {
        this.editor = editor;
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                updateClosest(e);
                if (closest != null) editor.edit(closest);
            }
            @Override
            public void mouseEntered(MouseEvent e) {
                hovering = true;
                updateClosest(e);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                hovering = false;
                updateClosest(e);
            }
        });
        addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                updateClosest(e);
                repaint();
            }
        });
    }

    private void updateClosest(MouseEvent e) {
        double x = undoTranslateX(e.getX()), y = undoTranslateY(e.getY());
        closest = editor.safetyInfos.stream()
                .filter(s -> s.entity != null && !s.entity.removed)
                .min(Comparator.comparingDouble(s -> Math.pow(s.x - x, 2) + Math.pow(s.y - y, 2)))// squared distance
                .orElse(null);
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = setupDraw(g);
        synchronized (Main.UPDATE_LOCKER) {
            drawZones(g2);
            drawStaticEntities(g2);
            drawMap(g2);
            if (editor.safetyInfos == null) return;
            drawCustomZones(g2);
        }
    }

    @Override
    protected void drawMap(Graphics2D g2) {
        g2.setColor(TEXT_DARK);
        g2.setFont(FONT_BIG);
        drawString(g2, hero.map.name, mid, (height / 2) + 12, Align.MID);
    }

    @Override
    protected void drawCustomZones(Graphics2D g2) {
        g2.setColor(ZONE);
        for (SafetyInfo safetyInfo : editor.safetyInfos) {
            if (safetyInfo == editor.editing || (hovering && safetyInfo == closest)
                    || safetyInfo.runMode == SafetyInfo.RunMode.NEVER
                    || safetyInfo.entity == null || safetyInfo.entity.removed) continue;
            drawSafeZone(g2, safetyInfo);
        }
        if (hovering && closest != null && closest != editor.editing) {
            g2.setColor(ZONE_HIGHLIGHT);
            drawSafeZone(g2, closest);
        }
        if (editor.editing != null) {
            g2.setColor(ZONE_SELECTED);
            drawSafeZone(g2, editor.editing);
        }
    }

}
