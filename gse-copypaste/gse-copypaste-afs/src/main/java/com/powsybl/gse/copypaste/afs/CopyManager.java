/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.gse.copypaste.afs;

import com.powsybl.afs.*;
import com.powsybl.commons.PowsyblException;
import com.powsybl.computation.local.LocalComputationConfig;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Paths;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author Nassirou Nambiema <nassirou.nambiena at rte-france.com>
 */
public final class CopyManager {

    private final String storageDirectory;

    private static final int COPY_EXPIRATION_TIME = 12;

    private ExecutorService tPool = Executors.newFixedThreadPool(3);
    private Map<String, CopyInfo> copy =new HashMap<>();

    public CopyManager() {
        storageDirectory = getStorageDirectory();
    }

 /*   *//**
     * @param nodes the nodes to copy
     *//*
    public void copy(List<? extends AbstractNodeBase> nodes) {
        StringBuilder copyParameters = new StringBuilder();

        //Add a signature to discriminate the copies
        copyParameters.append(CopyServiceConstants.COPY_SIGNATURE).append(CopyServiceConstants.PATH_LIST_SEPARATOR);

        //copy single nodes
        for (AbstractNodeBase node : nodes) {
            archiveAndCopy(copyParameters, node);
            copyParameters.append(CopyServiceConstants.PATH_LIST_SEPARATOR);
        }
        setClipboardContent(copyParameters.toString());
    }*/

    /**
     * @param nodes the nodes to copy
     */
    public Map<String, CopyInfo> copy(List<? extends AbstractNodeBase> nodes) {

        for (AbstractNodeBase node : nodes) {
            CopyInfo copyInfo = new CopyInfo();
            copyInfo.nodeId = node.getId();
            copyInfo.nodeType = node.getClass().getSimpleName();
            copyInfo.archivePath = storageDirectory + node.getName() + node.getId();
            copyInfo.archiveComplete = false;
            tPool.execute(() -> {
                // create task
                try {
                    archiveAndCopy(node);
                } catch (Exception e) {
                    throw new PowsyblException("The node " + node.getName() +" copy have encountered some problems! ");
                }
                // change completion status + end task
                copyInfo.archiveComplete = true;
            });
            //copyInfo.expirationDate =
            copy.put(copyInfo.nodeId, copyInfo);
        }
        return copy;
    }

    public void paste(String nodeid, AbstractNodeBase folder) {
        throwNodeIsNotAFolderException(folder);
        CopyInfo copyInfo = copy.get(nodeid);
        if (copyInfo != null && copyInfo.archiveComplete) {

        } else {

        }

    }

   /* private void archiveAndCopy(StringBuilder copyPaths, AbstractNodeBase node) {
        String nodeId = node.getId();
        String archiveDirectory = storageDirectory + node.getName() + node.getId();
        archiveNode(node, nodeId, archiveDirectory);
        copyPaths.append(archiveDirectory).append(CopyServiceConstants.PATH_SEPARATOR).append(nodeId).append(CopyServiceConstants.PATH_SEPARATOR).append(node.getClass());
    }*/

    private void archiveAndCopy(AbstractNodeBase node) {
        String nodeId = node.getId();
        String archiveDirectory = storageDirectory + node.getName() + node.getId();
        archiveNode(node, nodeId, archiveDirectory);
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

    private static void throwNodeIsNotAFolderException(AbstractNodeBase folder) {
        if (!(folder instanceof ProjectFolder || folder instanceof Folder)) {
            throw new IllegalArgumentException("the parameter might be a folder");
        }
    }

    private class CopyInfo {
        //String uuid;
        String nodeId;
        String nodeType;
        String archivePath;
        boolean archiveComplete;
        Date expirationDate;

        public String getNodeId() {
            return nodeId;
        }

        public String getNodeType() {
            return nodeType;
        }

        public String getArchivePath() {
            return archivePath;
        }

        public boolean isArchiveComplete() {
            return archiveComplete;
        }

        public void setArchiveComplete(boolean archiveComplete) {
            this.archiveComplete = archiveComplete;
        }

        public Date getExpirationDate() {
            return expirationDate;
        }

        public void setExpirationDate(Date expirationDate) {
            this.expirationDate = expirationDate;
        }
    }

    private enum CopyType {

    }
}
