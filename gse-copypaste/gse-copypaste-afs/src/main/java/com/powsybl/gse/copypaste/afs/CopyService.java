/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.gse.copypaste.afs;

import com.powsybl.afs.AbstractNodeBase;

/**
 * @author Nassirou Nambiema <nassirou.nambiena at rte-france.com>
 */
public interface CopyService {
    String PATH_SEPARATOR = "/";
    String LOCAL_DIR = System.getProperty("user.home") + "/Documents/ArchiveFolder2/";
    String REMOTE_DIR = "";

    void copy(AbstractNodeBase projectNode);

}
