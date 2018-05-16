/**
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.gse.app;

import com.powsybl.gse.spi.BrandingConfig;
import com.powsybl.gse.util.Glyph;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.text.Text;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class GseAppBar extends HBox {

    private final Button createButton;

    private final Button openButton;

    private final Button helpButton;

    public GseAppBar(BrandingConfig brandingConfig) {
        getStyleClass().add("gse-app-bar");
        setPadding(new Insets(3, 5, 3, 5));
        setAlignment(Pos.CENTER_LEFT);

        Region logo = brandingConfig.getLogo();
        logo.setPrefSize(32, 32);

        createButton = createButton("Create", null);
        openButton = createButton("Open", null);

        Text questionGlyph = Glyph.createAwesomeFont('\uf059').size("1.7em");
        questionGlyph.getStyleClass().add("gse-app-bar-icon");

        helpButton = createButton("", questionGlyph);

        Pane gluePanel = new Pane();
        setHgrow(gluePanel, Priority.ALWAYS);

        getChildren().addAll(logo, createButton, openButton, gluePanel, helpButton);
    }

    private Button createButton(String text, Node graphic) {
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

    public Button getHelpButton() {
        return helpButton;
    }
}
