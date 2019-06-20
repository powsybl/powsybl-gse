package com.powsybl.gse.afs.ext.base;

import com.powsybl.afs.ProjectFolder;
import com.powsybl.afs.ext.base.VirtualCase;
import com.powsybl.gse.spi.ProjectFileBuilderExtension;

import java.util.List;

public class VirtualCaseBuilderExtension implements ProjectFileBuilderExtension {

    @Override
    public Class<?> getProjectFileBuilderType() {
        return VirtualCase.class;
    }

    @Override
    public void buildFile(ProjectFolder foldr, String name, String primaryStorePath, String secondaryStorePath, List<String> additionalPaths) {

    }
}
