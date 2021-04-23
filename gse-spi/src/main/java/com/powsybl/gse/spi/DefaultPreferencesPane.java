/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.gse.spi;

import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.GridPane;
import org.controlsfx.control.ToggleSwitch;

import java.util.ResourceBundle;
import java.util.prefs.Preferences;

/**
 * @author Valentin Berthault <valentin.berthault at rte-france.com>
 */
public class DefaultPreferencesPane extends TitledPane {

    private final ToggleSwitch styleSwitch;

    private static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle.getBundle("lang.GseAppBar");

    public DefaultPreferencesPane(Scene scene, Preferences preferences) {
        this.setText("Preferences");
        this.setCollapsible(false);
        this.setExpanded(true);
        this.setPadding(new Insets(10));

        GridPane gridPane = new GridPane();
        Label affichageLabel = new Label("Affichage");
        gridPane.add(affichageLabel, 0, 0);

        styleSwitch = new ToggleSwitch();
        styleSwitch.setText(RESOURCE_BUNDLE.getString("StyleModeDark"));
        styleSwitch.getStyleClass().add("gse-app-bar-text");
        styleSwitch.setPadding(new Insets(10));

        gridPane.add(styleSwitch, 1, 1);

        if (scene.getStylesheets().contains("/css/gse-dark-theme.css")) {
            styleSwitch.setSelected(true);
        }

        styleSwitch.selectedProperty().addListener((observable, oldValue, newValue) -> {
            activeDarkMode(newValue, scene, preferences);
            if (newValue) {
                this.getScene().getStylesheets().add("/css/gse-dark-theme.css");
            } else {
                this.getScene().getStylesheets().remove("/css/gse-dark-theme.css");
            }
        });

        this.setContent(gridPane);
    }

    public static void activeDarkMode(boolean active, Scene scene, Preferences preferences) {
        if (active) {
            scene.getStylesheets().add("/css/gse-dark-theme.css");
            preferences.remove("darkMode");
            preferences.put("darkMode", "true");
        } else {
            scene.getStylesheets().remove("/css/gse-dark-theme.css");
            preferences.remove("darkMode");
            preferences.put("darkMode", "false");
        }
    }
}
