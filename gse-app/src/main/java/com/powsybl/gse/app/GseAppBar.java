/**
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.gse.app;

import com.powsybl.gse.spi.BrandingConfig;
import com.powsybl.gse.spi.GseAuthenticator;
import com.powsybl.gse.spi.GseContext;
import com.powsybl.gse.util.Glyph;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.text.Text;
import org.controlsfx.control.ToggleSwitch;

import java.util.ResourceBundle;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class GseAppBar extends HBox {

    private static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle.getBundle("lang.GseAppBar");

    private final Button createButton;

    private final Button openButton;

    private final Button helpButton;

    private final ToggleSwitch styleSwitch;

    private UserSessionPane userSessionPane;

    public GseAppBar(GseContext context, BrandingConfig brandingConfig) {
        getStyleClass().add("gse-app-bar");
        setPadding(new Insets(3, 5, 3, 5));
        setAlignment(Pos.CENTER_LEFT);

        Region logo = brandingConfig.getLogo();
        logo.setPrefSize(32, 32);

        createButton = createButton(RESOURCE_BUNDLE.getString("Create"), null);
        createButton.getStyleClass().add("gse-app-bar-text");
        openButton = createButton(RESOURCE_BUNDLE.getString("Open"), null);
        openButton.getStyleClass().add("gse-app-bar-text");
        styleSwitch = new ToggleSwitch();
        Tooltip tooltipStyleSwitch = new Tooltip(RESOURCE_BUNDLE.getString("StyleModeDark"));
        styleSwitch.setTooltip(tooltipStyleSwitch);
        styleSwitch.getStyleClass().add("gse-app-bar-text");

        Text questionGlyph = Glyph.createAwesomeFont('\uf059');
        questionGlyph.getStyleClass().add("gse-app-bar-icon");

        helpButton = createButton("", questionGlyph);

        Pane gluePanel = new Pane();
        setHgrow(gluePanel, Priority.ALWAYS);

        getChildren().addAll(logo, createButton, openButton, gluePanel, styleSwitch);
        GseAuthenticator.find().ifPresent(authenticator -> {
            userSessionPane = new UserSessionPane(context, authenticator);
            getChildren().add(userSessionPane);
        });
        getChildren().add(helpButton);
    }

    private static Button createButton(String text, Node graphic) {
        Button createButton = new Button(text, graphic);
        createButton.getStyleClass().add("gse-app-bar-button");
        return createButton;
    }

    public Button getCreateButton() {
        return createButton;
    }

    public Button getOpenButton() {
        return openButton;
    }

    public ToggleSwitch getToggleSwitch() {
        return styleSwitch;
    }

    public Button getHelpButton() {
        return helpButton;
    }

    public UserSessionPane getUserSessionPane() {
        return userSessionPane;
    }
}
