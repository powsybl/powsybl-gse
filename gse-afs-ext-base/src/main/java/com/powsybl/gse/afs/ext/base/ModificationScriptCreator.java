/**
 * Copyright (c) 2017, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.gse.afs.ext.base;

import com.powsybl.afs.ProjectFolder;
import com.powsybl.afs.ext.base.ModificationScriptBuilder;
import com.powsybl.afs.ext.base.ScriptType;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class ModificationScriptCreator extends AbstractModificationScriptCreator {

    public ModificationScriptCreator(ProjectFolder folder) {
        super(folder);
    }

    @Override
    protected void create(ProjectFolder folder, String name) {
        folder.fileBuilder(ModificationScriptBuilder.class)
                .withName(name)
                .withType(ScriptType.GROOVY)
                .withContent("")
                .build();
    }
}
