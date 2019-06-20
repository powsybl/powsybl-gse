/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.gse.copypaste.afs;

import com.powsybl.afs.*;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;

/**
 * @author Nassirou Nambiema <nassirou.nambiena at rte-france.com>
 */
public final class CopyModel {

    private final String storageDirectory;

    public CopyModel(String storageDirectory) {
        this.storageDirectory = Objects.requireNonNull(storageDirectory);
    }

    /**
     *
     * @param nodes the nodes to copy
     */
    public void copy(List<? extends AbstractNodeBase> nodes) {
        StringBuilder copyParameters = new StringBuilder();

        //add the copy informations
        addRootId(nodes, copyParameters);
        copyParameters.append(CopyServiceConstants.COPY_SIGNATURE).append(CopyServiceConstants.PATH_LIST_SEPARATOR);

        //copy single nodes
        for (AbstractNodeBase node : nodes) {
            if (node instanceof Folder && node.isInLocalFileSystem()) {
                String path = node.getPath().toString().replace("/:", "");
                copyParameters.append(path).append(CopyServiceConstants.PATH_LIST_SEPARATOR);
            } else if (!isAdependent(node)) {
                archiveAndCopy(copyParameters, node);
                copyParameters.append(CopyServiceConstants.PATH_LIST_SEPARATOR);
            }
        }

        //copy nodes with dependencies
        for (AbstractNodeBase node : nodes) {
            if (isAdependent(node)) {
                List<ProjectDependency<ProjectNode>> dependencies = ((ProjectFile) node).getDependencies();
                archiveAndCopy(copyParameters, node);

                copyParameters.append(CopyServiceConstants.DEPENDENCY_SEPARATOR);
                copyParameters.append(node.getName()).append(CopyServiceConstants.PATH_SEPARATOR).append(node.getClass().getName());
                dependencies.forEach(dependence -> copyParameters.append(CopyServiceConstants.PATH_SEPARATOR).append(dependence.getProjectNode().getName()));
                copyParameters.append(CopyServiceConstants.PATH_LIST_SEPARATOR);
            }
        }
        setClipboardContent(copyParameters);
    }

    /**
     * copy specified nodes with their dependencies
     *
     * @param nodes the nodes to copy
     */
    public void deepCopy(List<? extends AbstractNodeBase> nodes) {
        StringBuilder copyParameters = new StringBuilder();
        for (AbstractNodeBase node : nodes) {
            if (isAdependent(node)) {

                List<ProjectDependency<ProjectNode>> dependencies = ((ProjectFile) node).getDependencies();
                dependencies.forEach(dependence -> {
                    ProjectNode projectNode = dependence.getProjectNode();
                    archiveAndCopy(copyParameters, projectNode);
                    copyParameters.append(CopyServiceConstants.PATH_LIST_SEPARATOR);
                });

                copyParameters.append(node.getName()).append(CopyServiceConstants.DEPENDENCY_SEPARATOR).append(node.getClass().getName());
                dependencies.forEach(dep -> copyParameters.append(CopyServiceConstants.DEPENDENCY_SEPARATOR).append(dep.getProjectNode().getName()));
                copyParameters.append(CopyServiceConstants.PATH_LIST_SEPARATOR);
            } else {
                archiveAndCopy(copyParameters, node);
            }

        }
        setClipboardContent(copyParameters);
    }

    private static void addRootId(List<? extends AbstractNodeBase> nodes, StringBuilder copyPaths) {
        AbstractNodeBase absNode = nodes.get(0);
        if (absNode instanceof ProjectNode) {
            ProjectFolder rootFolder = ((ProjectNode) absNode).getProject().getRootFolder();
            copyPaths.append(rootFolder.getId()).append(CopyServiceConstants.PATH_LIST_SEPARATOR);
        } else {
            Folder rootFolder = ((Node) absNode).getFileSystem().getRootFolder();
            copyPaths.append(rootFolder.getId()).append(CopyServiceConstants.PATH_LIST_SEPARATOR);
        }
    }

    private static boolean isAdependent(AbstractNodeBase node) {
        return node instanceof ProjectFile && !((ProjectFile) node).getDependencies().isEmpty();
    }

    private void archiveAndCopy(StringBuilder copyPaths, AbstractNodeBase node) {
        String nodeId = node.getId();
        String archiveDirectory = storageDirectory + node.getName() + node.getId();
        archiveNode(node, nodeId, archiveDirectory);
        copyPaths.append(archiveDirectory).append(CopyServiceConstants.PATH_SEPARATOR).append(nodeId);
    }

    private static void archiveNode(AbstractNodeBase node, String nodeId, String parentPath) {
        java.io.File archiveRootFolder = new java.io.File(parentPath);
        if (!archiveRootFolder.exists()) {
            archiveRootFolder.mkdir();
        }
        java.io.File archiveDestinationFolder = new File(parentPath, nodeId);
        if (archiveDestinationFolder.exists()) {
            try {
                FileUtils.deleteDirectory(archiveDestinationFolder);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
        if (node instanceof ProjectNode) {
            ((ProjectNode) node).archive(Paths.get(parentPath));
        } else if (node instanceof Node) {
            ((Node) node).archive(Paths.get(parentPath));
        }
    }

    private static void setClipboardContent(StringBuilder copyParameters) {
        final Clipboard clipboard = Clipboard.getSystemClipboard();
        final ClipboardContent content = new ClipboardContent();
        content.putString(copyParameters.toString());
        clipboard.setContent(content);
    }

}
