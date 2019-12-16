/**
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.gse.afs.ext.base;

import com.powsybl.afs.ProjectFolder;
import com.powsybl.afs.contingency.ContingencyStoreBuilder;

import java.util.ResourceBundle;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class ContingencyStoreCreator extends AbstractModificationScriptCreator {

    private static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle.getBundle("lang.ContingencyStore");

    public ContingencyStoreCreator(ProjectFolder folder) {
        super(folder);
    }

    @Override
    public String getTitle() {
        return RESOURCE_BUNDLE.getString("CreateContingencies");
    }

    @Override
    protected void create(ProjectFolder folder, String name) {
        folder.fileBuilder(ContingencyStoreBuilder.class)
                .withName(name)
                .build();
    }
}
