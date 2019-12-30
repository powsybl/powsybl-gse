/**
 * Copyright (c) 2017, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.gse.util;

import com.powsybl.afs.Project;
import com.powsybl.afs.ProjectFile;
import com.powsybl.afs.ProjectFolder;
import com.powsybl.afs.ProjectNode;
import com.powsybl.gse.spi.GseContext;
import javafx.stage.Window;

import java.util.function.BiPredicate;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class ProjectNodeSelectionPane<T extends ProjectNode> extends AbstractNodeSelectionPane<T> {

    public ProjectNodeSelectionPane(Project project, String label, Window window, GseContext context, Class<T> filter, Class<?>... otherFilters) {
        this(project, label, false, window, context, filter, otherFilters);
    }

    public ProjectNodeSelectionPane(Project project, String label, boolean mandatory, Window window, GseContext context, Class<T> filter, Class<?>... otherFilters) {
        super(label, () -> NodeChooser.showAndWaitDialog(project, window, context, filter, otherFilters), mandatory, context);
    }

    public ProjectNodeSelectionPane(Project project, String label, boolean mandatory, Window window, GseContext context, BiPredicate<ProjectNode, NodeChooser.TreeModel<ProjectNode, ProjectFile, ProjectFolder>> filter) {
        super(label, () -> NodeChooser.showAndWaitDialog(project, window, context, filter), mandatory, context);
    }
}

