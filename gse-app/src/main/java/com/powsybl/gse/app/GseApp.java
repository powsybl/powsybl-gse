/**
 * Copyright (c) 2017, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.gse.app;

import com.powsybl.afs.AppData;
import com.powsybl.commons.util.ServiceLoaderCache;
import com.powsybl.computation.local.LocalComputationManager;
import com.powsybl.gse.spi.FontLoader;
import com.powsybl.gse.spi.GseContext;
import com.powsybl.gse.spi.StyleSheetLoader;
import com.powsybl.gse.util.GseUtil;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class GseApp extends Application {

    private static final Logger LOGGER = LoggerFactory.getLogger(GseApp.class);

    private ExecutorService executor;

    private LocalComputationManager computationManager;

    private AppData appData;

    @Override
    public void start(Stage stage) {
        GsePane gsePane = new GsePane(new GseContext(executor), appData, this);
        stage.setTitle(gsePane.getTitle());
        stage.getIcons().addAll(gsePane.getIcons());
        Scene scene = new Scene(gsePane, 1200, 800);
        stage.setScene(scene);

        GseUtil.registerAccelerators(scene);

        scene.getStylesheets().add("/com/panemu/tiwulfx/res/tiwulfx.css");
        scene.getStylesheets().add("/css/gse.css");

        for (StyleSheetLoader loader : new ServiceLoaderCache<>(StyleSheetLoader.class).getServices()) {
            loader.load(scene);
        }

        stage.show();

        stage.setOnCloseRequest(event -> {
            gsePane.dispose();
            if (!gsePane.isClosable()) {
                event.consume();
            }
        });
    }

    @Override
    public void init() throws Exception {
        super.init();
        for (FontLoader loader : new ServiceLoaderCache<>(FontLoader.class).getServices()) {
            loader.load();
        }
        Thread.setDefaultUncaughtExceptionHandler((t, e) -> {
            LOGGER.error("Uncaught exception in thread {}", t.getName());
            LOGGER.error(e.toString(), e);
        });
        executor = Executors.newCachedThreadPool(new GseThreadFactory());
        computationManager = new LocalComputationManager(executor);
        appData = new AppData(computationManager, computationManager);
    }

    @Override
    public void stop() throws Exception {
        super.stop();
        appData.close();
        computationManager.close();
        executor.shutdownNow();
        executor.shutdown();
        executor.awaitTermination(1, TimeUnit.MINUTES);
    }

    public static void main(String[] args) {
        GseUtil.setProxy();
        launch(GseApp.class, args);
    }
}
