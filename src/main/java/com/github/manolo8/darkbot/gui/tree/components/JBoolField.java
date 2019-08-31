package com.github.manolo8.darkbot.gui.tree.components;

import com.github.manolo8.darkbot.config.tree.ConfigField;
import com.github.manolo8.darkbot.gui.AdvancedConfig;
import com.github.manolo8.darkbot.gui.tree.OptionEditor;

import javax.swing.*;
import java.awt.*;

public class JBoolField extends JCheckBox implements OptionEditor {

    private ConfigField field;

    public JBoolField() {
        putClientProperty("ConfigTree", true);
        super.addChangeListener(e -> {
            if (field != null) field.set(this.isSelected());
        });
    }

    @Override
    public JComponent getComponent() {
        return this;
    }

    @Override
    public void edit(ConfigField field) {
        this.field = null;
        setSelected(field.get());
        this.field = field;
    }

    @Override
    public Dimension getPreferredSize() {
        return AdvancedConfig.forcePreferredHeight(super.getPreferredSize());
    }

    @Override
    public Insets getInsets() {
        return new Insets(0, 0, 0, 0);
    }
}
