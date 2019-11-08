/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.gse.copypaste.afs.local;

import com.powsybl.afs.*;
import com.powsybl.gse.copypaste.afs.CopyManager;
import com.powsybl.gse.copypaste.afs.CopyService;

import java.util.List;
import java.util.Map;


/**
 * @author Nassirou Nambiema <nassirou.nambiena at rte-france.com>
 */
public class LocalCopyService implements CopyService {

    private final CopyManager copyManager = new CopyManager();

    @Override
    public void copy(List<? extends AbstractNodeBase> nodes) {
        copyManager.copy(nodes);
    }

    @Override
    public void paste(String fileSystemName, String nodeId, AbstractNodeBase folder) {
        copyManager.paste(fileSystemName, nodeId, folder);
    }

}
