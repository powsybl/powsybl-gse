/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.gse.copypaste.afs;

/**
 * @author Nassirou Nambiema <nassirou.nambiena at rte-france.com>
 */
public final class CopyServiceConstants {

    public static final String PATH_SEPARATOR = "/";
    public static final String PATH_LIST_SEPARATOR = "@copy";
    public static final String LOCAL_DIR = System.getProperty("user.home") + "/Documents/ArchiveFolder2/";
    public static final String DEPENDENCY_SEPARATOR = "@Dependency";
    public static final String REMOTE_DIR = "";

    private CopyServiceConstants() {
    }
}
