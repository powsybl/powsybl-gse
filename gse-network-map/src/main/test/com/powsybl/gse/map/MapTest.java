/**
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.gse.map;

import com.powsybl.afs.ext.base.ProjectCase;
import com.powsybl.gse.spi.GseContext;
import com.powsybl.gse.util.DefaultFontLoader;
import com.powsybl.iidm.network.Network;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import org.mockito.Mockito;

import java.util.Collections;
import java.util.concurrent.ForkJoinPool;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class MapTest extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        new GluonHqMapsInitializer().load();
        new DefaultFontLoader().load();
        BorderPane root = new BorderPane();
        ProjectCase projectCase = Mockito.mock(ProjectCase.class);
        Network network = Mockito.mock(Network.class);
        Mockito.when(projectCase.getNetwork()).thenReturn(network);
        Mockito.when(network.getSubstations()).thenReturn(Collections.emptyList());
        Mockito.when(network.getSubstationCount()).thenReturn(0);
        Mockito.when(network.getLines()).thenReturn(Collections.emptyList());
        Mockito.when(network.getLineCount()).thenReturn(0);
        NetworkMap map = new NetworkMap(projectCase, new GseContext(ForkJoinPool.commonPool()));
        root.setCenter(map.getContent());
        map.view();
        Scene scene = new Scene(root, 1000, 800);
        primaryStage.setScene(scene);
        primaryStage.setTitle("Map test");
        primaryStage.show();
    }
}


