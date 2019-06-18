/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.gse.spi;


import com.powsybl.afs.ProjectFolder;

/**
 * @author Nassirou Nambiema <nassirou.nambiena at rte-france.com>
 */
public interface ProjectFileBuilderExtension {

    Class<?> getProjectFileBuilderType();

    void buildFile(ProjectFolder foldr, String name, String primaryStorePath);

    void buildFile(ProjectFolder foldr, String name, String primaryStorePath, String secondaryStorePath);

    void buildFile(ProjectFolder foldr, String name, String primaryStorePath, String secondaryStorePath, String... additionalPaths);

}
