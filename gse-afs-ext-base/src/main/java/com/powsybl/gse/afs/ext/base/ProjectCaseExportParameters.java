/**
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.gse.afs.ext.base;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * @author Marianne Funfrock <marianne.funfrock at rte-france.com>
 */
public class ProjectCaseExportParameters {

    private static final String GZ_EXT = ".gz";

    private String filePath;
    private boolean zipped;

    public ProjectCaseExportParameters() {
        zipped = false;
    }

    public boolean isZipped() {
        return zipped;
    }

    public void setZipped(boolean zipped) {
        this.zipped = zipped;
    }

    public Path getFilePath() {
        if (zipped && !filePath.toLowerCase().endsWith(GZ_EXT)) {
            filePath += GZ_EXT;
        }
        return Paths.get(filePath);
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }
}
