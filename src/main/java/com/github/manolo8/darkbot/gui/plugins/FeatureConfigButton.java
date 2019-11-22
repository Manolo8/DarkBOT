package com.github.manolo8.darkbot.gui.plugins;

import com.github.manolo8.darkbot.config.Config;
import com.github.manolo8.darkbot.core.itf.Configurable;
import com.github.manolo8.darkbot.extensions.features.FeatureDefinition;
import com.github.manolo8.darkbot.gui.AdvancedConfig;
import com.github.manolo8.darkbot.gui.components.MainButton;
import com.github.manolo8.darkbot.gui.utils.Popups;
import com.github.manolo8.darkbot.gui.utils.UIUtils;
import com.github.manolo8.darkbot.utils.I18n;

import javax.swing.*;
import java.awt.event.ActionEvent;

public class FeatureConfigButton extends MainButton {

    private Config config;
    private FeatureDefinition<Configurable> feature;

    FeatureConfigButton(Config config, FeatureDefinition<Configurable> feature) {
        super(UIUtils.getIcon("config"));
        setDisabledIcon(UIUtils.getIcon("config_unloaded"));
        this.config = config;
        this.feature = feature;
        updateStatus(feature);
        feature.addStatusListener(this::updateStatus);
    }

    private void updateStatus(FeatureDefinition feature) {
        boolean enabled = feature.canLoad() && feature.getInstance() != null;

        this.setEnabled(enabled);
        setToolTipText(enabled ? I18n.get("gui.plugins.config_button.enabled.desc") : I18n.get("gui.plugins.config_button.disabled.desc"));
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (feature.getInstance() == null) {
            Popups.showMessageAsync(I18n.get("gui.plugins.config_button.popup"),
                    I18n.get("gui.plugins.config_button.popup.desc"), JOptionPane.INFORMATION_MESSAGE);
        } else {
            JOptionPane options = new JOptionPane(new AdvancedConfig(this.config.CUSTOM_CONFIGS.get(feature.getId())),
                    JOptionPane.PLAIN_MESSAGE, JOptionPane.DEFAULT_OPTION, null, new Object[]{}, null);
            options.setBorder(null);
            Popups.showMessageSync(feature.getName(), options);
            setBackground();
        }
    }

}
