/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.gse.copypaste.afs;

import com.powsybl.afs.AbstractNodeBase;
import com.powsybl.afs.FolderBase;

import java.util.List;

/**
 * @author Nassirou Nambiema <nassirou.nambiena at rte-france.com>
 */
public interface CopyService {

    void copy(List<? extends AbstractNodeBase> nodes);

    void paste(String nodeId, FolderBase folder);
}
