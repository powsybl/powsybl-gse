/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.gse.copy_paste.afs;

/**
 * @author Nassirou Nambiema <nassirou.nambiena at rte-france.com>
 */
public final class CopyServiceConstants {

    public static final String COPY_SIGNATURE = "@COPY_SIGNATURE@";
    public static final String PROJECT_TYPE = "com.powsybl.afs.Project";
    public static final String FOLDER_TYPE = "com.powsybl.afs.Folder";
    public static final long COPY_EXPIRATION_TIME = 12;
    public static final String TEMPORARY_NAME = "temporaryName";

    private CopyServiceConstants() {
    }
}