/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.gse.copypaste.afs;

import java.nio.file.FileSystems;

/**
 * @author Nassirou Nambiema <nassirou.nambiena at rte-france.com>
 */
public final class CopyServiceConstants {

    public static final String PATH_SEPARATOR = FileSystems.getDefault().getSeparator();
    public static final String PATH_LIST_SEPARATOR = "@PATH_LIST_SEPARATOR@";
    public static final String COPY_SIGNATURE = "@info.json@";

    private CopyServiceConstants() {
    }
}
