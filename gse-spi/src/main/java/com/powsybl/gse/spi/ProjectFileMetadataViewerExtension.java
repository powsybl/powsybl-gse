/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 */

package com.powsybl.gse.spi;

import com.powsybl.afs.ProjectFile;

/**
 * @author Paul Bui-Quang <paul.buiquang at rte-france.com>
 */
public interface ProjectFileMetadataViewerExtension<T extends ProjectFile> {

    Class<T> getProjectFileType();

    String display(T projectFile);
}
