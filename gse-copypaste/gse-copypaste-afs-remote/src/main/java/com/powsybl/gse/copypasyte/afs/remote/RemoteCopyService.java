/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.gse.copypasyte.afs.remote;

import com.powsybl.afs.*;
import com.powsybl.gse.copypaste.afs.CopyModel;
import com.powsybl.gse.copypaste.afs.CopyService;
import com.powsybl.gse.copypaste.afs.CopyServiceConstants;

import java.util.List;


/**
 * @author Nassirou Nambiema <nassirou.nambiena at rte-france.com>
 */
public class RemoteCopyService implements CopyService {

    private CopyModel copyModel = new CopyModel(CopyServiceConstants.REMOTE_DIR);

    @Override
    public void copy(List<? extends AbstractNodeBase> nodes) {
        copyModel.copy(nodes);
    }

}
