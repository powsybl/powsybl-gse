/**
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.gse.spi;

import com.powsybl.commons.Version;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.TextArea;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import org.apache.commons.lang3.SystemUtils;

import java.util.Collections;
import java.util.List;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class DefaultAboutPane extends BorderPane {

    protected static final double LOGO_SIZE = 128;

    protected List<Node> getAdditionalLogos() {
        return Collections.emptyList();
    }

    protected List<String> getAdditionalInfos() {
        return Collections.emptyList();
    }

    public DefaultAboutPane() {
        setBackground(new Background(new BackgroundFill(Color.WHITE, null, null)));

        PowsyblLogo powsyblLogo = new PowsyblLogo(LOGO_SIZE, LOGO_SIZE);

        HBox logoPane = new HBox(5, powsyblLogo);
        logoPane.getChildren().addAll(getAdditionalLogos());
        logoPane.setPadding(new Insets(5, 5, 5, 5));
        setTop(logoPane);
        TextArea infos = new TextArea();
        infos.setEditable(false);
        infos.setWrapText(true);
        infos.setPrefRowCount(20);
        infos.setText(String.join(System.lineSeparator() + System.lineSeparator(),
                                  "Java version: " + SystemUtils.JAVA_VERSION,
                                  "Powsybl core version: " + Version.VERSION,
                                  "Powsybl GSE version: " + com.powsybl.gse.spi.PowsyblGseVersion.INSTANCE) +
                      System.lineSeparator() + System.lineSeparator() +
                      String.join(System.lineSeparator() + System.lineSeparator(), getAdditionalInfos()));
        setCenter(infos);
    }
}
