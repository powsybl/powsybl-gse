/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.gse.copypaste.afs;

import com.powsybl.afs.*;
import com.powsybl.computation.local.LocalComputationConfig;
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
public final class CopyModel {

    private final String storageDirectory;

    public CopyModel() {
        storageDirectory = getStorageDirectory();
    }

    /**
     * @param nodes the nodes to copy
     */
    public void copy(List<? extends AbstractNodeBase> nodes) {
        StringBuilder copyParameters = new StringBuilder();

        //Add a signature to discriminate the copies
        copyParameters.append(CopyServiceConstants.COPY_SIGNATURE).append(CopyServiceConstants.PATH_LIST_SEPARATOR);

        //copy single nodes
        for (AbstractNodeBase node : nodes) {
            if (node instanceof Folder && node.isInLocalFileSystem()) {
                String path = node.getPath().toString().replace("/:", "");
                copyParameters.append(path).append(CopyServiceConstants.PATH_LIST_SEPARATOR);
            } else {
                archiveAndCopy(copyParameters, node);
                copyParameters.append(CopyServiceConstants.PATH_LIST_SEPARATOR);
            }
        }
        setClipboardContent(copyParameters.toString());
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
        deleteOnExit(archiveRootFolder);
    }

    private static void deleteOnExit(File folder) {
        folder.deleteOnExit();
        for (File file : folder.listFiles()) {
            if (file.isDirectory()) {
                deleteOnExit(file);
            } else {
                file.deleteOnExit();
            }
        }
    }

    private static void setClipboardContent(String copyParameters) {
        final Clipboard clipboard = Clipboard.getSystemClipboard();
        final ClipboardContent content = new ClipboardContent();
        content.putString(copyParameters);
        clipboard.setContent(content);
    }

    private static String getStorageDirectory() {
        return LocalComputationConfig.load().getLocalDir() + CopyServiceConstants.PATH_SEPARATOR;
    }

}
