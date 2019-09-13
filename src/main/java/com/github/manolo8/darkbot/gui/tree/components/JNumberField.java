package com.github.manolo8.darkbot.gui.tree.components;

import com.bulenkov.iconloader.util.Gray;
import com.github.manolo8.darkbot.config.tree.ConfigField;
import com.github.manolo8.darkbot.config.types.Num;
import com.github.manolo8.darkbot.gui.AdvancedConfig;
import com.github.manolo8.darkbot.gui.tree.OptionEditor;

import javax.swing.*;
import java.awt.*;

public class JNumberField extends JSpinner implements OptionEditor {

    private ConfigField field;

    public JNumberField() {
        putClientProperty("ConfigTree", true);
        setBorder(BorderFactory.createLineBorder(Gray._90));
        addChangeListener(e -> {
            if (field != null) field.set(getValue());
        });
    }

    @Override
    public JComponent getComponent() {
        return this;
    }

    @Override
    public void edit(ConfigField field) {
        this.field = null;
        Num number = field.field.getAnnotation(Num.class);
        Number value = field.get();
        if (value == null) {
            System.err.println("Null value in number config, using min as default: " + field.field);
            value = number.min();
        }

        SpinnerNumberModel model;
        try {
            model = new SpinnerNumberModel(value, number.min(), number.max(), number.step());
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            System.err.println("Failed to create editor for field, ignoring min & max: " + field.field);

            model = new SpinnerNumberModel(value, null, null, number.step());
        }
        setModel(model);
        setPreferredSize(new Dimension(25 + (String.valueOf(number.max()).length() * 9), AdvancedConfig.EDITOR_HEIGHT));
        this.field = field;
    }

    @Override
    public Dimension getPreferredSize() {
        return AdvancedConfig.forcePreferredHeight(super.getPreferredSize());
    }

}
