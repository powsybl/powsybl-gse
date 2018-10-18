/**
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.gse.app;

import com.powsybl.gse.spi.BrandingConfig;
import javafx.application.Preloader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class GsePreloader extends Preloader {

    private Stage stage;

    @Override
    public void start(Stage stage) {
        this.stage = stage;

        BrandingConfig brandingConfig = BrandingConfig.find();

        Node splashScreen = brandingConfig.getLogo();
        Pane pane = new Pane();
        pane.setStyle("-fx-background-color: transparent;");
        pane.getChildren().addAll(splashScreen);

        Scene scene = new Scene(pane, Color.TRANSPARENT);
        stage.setScene(scene);
        stage.initStyle(StageStyle.TRANSPARENT);
        stage.show();
    }

    @Override
    public void handleStateChangeNotification(StateChangeNotification evt) {
        if (evt.getType() == StateChangeNotification.Type.BEFORE_START) {
            stage.hide();
        }
    }

}
