/**
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.gse.spi;

import javafx.application.Application;

import java.util.Objects;
import java.util.concurrent.ExecutorService;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class GseContext {

    private final Application app;

    private final ExecutorService executor;

    public GseContext(ExecutorService executor, Application app) {
        this.executor = Objects.requireNonNull(executor);
        this.app = app;
    }

    public ExecutorService getExecutor() {
        return executor;
    }

    public Application getApp() {
        return app;
    }
}
