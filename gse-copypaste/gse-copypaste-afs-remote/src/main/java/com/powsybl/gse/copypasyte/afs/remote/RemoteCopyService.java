/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.gse.copypasyte.afs.remote;

import com.powsybl.afs.*;
import com.powsybl.gse.copypaste.afs.CopyService;
import com.powsybl.gse.copypaste.afs.CopyServiceConstants;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Paths;
import java.util.List;


/**
 * @author Nassirou Nambiema <nassirou.nambiena at rte-france.com>
 */
public class RemoteCopyService implements CopyService {

    @Override
    public void copy(List<? extends AbstractNodeBase> nodes) {
        StringBuilder copyPaths = new StringBuilder();
        addRootId(nodes, copyPaths);
        copyPaths.append(CopyServiceConstants.COPY_SIGNATURE).append(CopyServiceConstants.PATH_LIST_SEPARATOR);

        //single nodes
        for (AbstractNodeBase node : nodes) {
            if (node instanceof Folder && node.isInLocalFileSystem()) {
                String path = node.getPath().toString().replace("/:", "");
                copyPaths.append(path).append(CopyServiceConstants.PATH_LIST_SEPARATOR);
            } else if (!isDependent(node)) {
                archiveAndCopy(copyPaths, node);
                copyPaths.append(CopyServiceConstants.PATH_LIST_SEPARATOR);
            }
        }

        //nodes with dependencies
        for (AbstractNodeBase node : nodes) {
            if (isDependent(node)) {
                List<ProjectDependency<ProjectNode>> dependencies = ((ProjectFile) node).getDependencies();
                archiveAndCopy(copyPaths, node);

                copyPaths.append(CopyServiceConstants.DEPENDENCY_SEPARATOR);
                copyPaths.append(node.getName()).append(CopyServiceConstants.PATH_SEPARATOR).append(node.getClass().getName());
                dependencies.forEach(dependence -> copyPaths.append(CopyServiceConstants.PATH_SEPARATOR).append(dependence.getProjectNode().getName()));
                copyPaths.append(CopyServiceConstants.PATH_LIST_SEPARATOR);
            }
        }
        setClipboardContent(copyPaths);
    }

    @Override
    public void deepCopy(List<? extends AbstractNodeBase> nodes) {
        // To implement
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

    private static boolean isDependent(AbstractNodeBase node) {
        return node instanceof ProjectFile && !((ProjectFile) node).getDependencies().isEmpty();
    }

    private static void archiveAndCopy(StringBuilder copyPaths, AbstractNodeBase node) {
        String nodeId = node.getId();
        String archiveDirectory = nodeArchiveDirectory(node);
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

    private static void setClipboardContent(StringBuilder copyPaths) {
        final Clipboard clipboard = Clipboard.getSystemClipboard();
        final ClipboardContent content = new ClipboardContent();
        content.putString(copyPaths.toString());
        clipboard.setContent(content);
    }

    private static String nodeArchiveDirectory(AbstractNodeBase node) {
        return CopyServiceConstants.REMOTE_DIR + node.getName() + node.getId();
    }

}
