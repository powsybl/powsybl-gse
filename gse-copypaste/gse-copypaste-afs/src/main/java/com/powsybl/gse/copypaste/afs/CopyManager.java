/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.gse.copypaste.afs;

import com.powsybl.afs.*;
import com.powsybl.computation.local.LocalComputationConfig;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * @author Nassirou Nambiema <nassirou.nambiena at rte-france.com>
 */
public final class CopyManager {

    private static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle.getBundle("lang.CopyManager");

    private static final Logger LOGGER = LoggerFactory.getLogger(CopyManager.class);

    private final String storageDirectory;

    private final Object copyLock = new Object();
    private ExecutorService tPool = Executors.newFixedThreadPool(3);
    private Map<String, CopyInfo> currentCopies = new HashMap<>();

    public CopyManager() {
        init();
        storageDirectory = getStorageDirectory();
    }

    /**
     * @param nodes the nodes to copy
     */
    public Map<String, CopyInfo> copy(List<? extends AbstractNodeBase> nodes) {

        for (AbstractNodeBase node : nodes) {
            CopyInfo copyInfo = new CopyInfo();
            copyInfo.archiveSuccess = false;

            tPool.execute(() -> {
                // create task

                synchronized (copyLock) {
                    try {
                        archiveAndCopy(node);
                    } catch (Exception e) {
                        copyInfo.archiveSuccess = null;
                        LOGGER.error(e.toString(), e);
                    }

                    // change completion status + end task
                    copyInfo.archiveSuccess = true;
                }

            });

            copyInfo.nodeId = node.getId();
            copyInfo.node = node;
            copyInfo.archivePath = Paths.get(storageDirectory + node.getName() + node.getId());
            copyInfo.expirationDate = ZonedDateTime.now().plusHours(CopyServiceConstants.COPY_EXPIRATION_TIME);
            currentCopies.put(copyInfo.nodeId, copyInfo);
        }
        return currentCopies;
    }

    /**
     * @param nodeId the copied node's id
     * @param folder the archive destination's folder
     */
    public void paste(String nodeId, FolderBase folder) {

        boolean nodeNameAlreadyExists = false;
        CopyInfo copyInfo = currentCopies.get(nodeId);

        synchronized (copyLock) {
            if (copyInfo != null && copyInfo.archiveSuccess) {
                List children = folder.getChildren();
                nodeNameAlreadyExists = ((List<AbstractNodeBase>) children).stream().anyMatch(child -> copyInfo.node.getName().equals(child.getName()));

                if (nodeNameAlreadyExists) {
                    renameAndPaste(folder, copyInfo);
                } else {
                    ((AbstractNodeBase) folder).unarchive(copyInfo.archivePath.resolve(copyInfo.nodeId));
                }
            }
        }
    }

    private void init() {
        Timer timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                synchronized (copyLock) {
                    for (String nodeId : new ArrayList<>(currentCopies.keySet())) {
                        CopyInfo copyInfo = currentCopies.get(nodeId);
                        if (copyInfo.expirationDate.isAfter(ZonedDateTime.now())) {
                            currentCopies.remove(nodeId);
                            try {
                                FileUtils.deleteDirectory(new File(copyInfo.archivePath.toString()));
                            } catch (IOException e) {
                                throw new UncheckedIOException(e);
                            }
                        }
                    }
                }
            }
        }, CopyServiceConstants.CLEANUP_DELAY, CopyServiceConstants.CLEANUP_PERIOD);
    }

    private void renameAndPaste(FolderBase folder, CopyInfo info) {

        if (folder instanceof ProjectFolder) {
            //Rename and Paste into a ProjectFolder
            List children =  folder.getChildren();
            for (AbstractNodeBase child : (List<AbstractNodeBase>) children) {
                String name = child.getName();
                if (info.node.getName().equals(name)) {
                    if (info.node.getClass().equals(child.getClass())) {
                        renameSameTypeNode((ProjectFolder) folder, child, info);
                    } else {
                        //GseAlerts.showDraggingError();
                    }
                    break;
                }
            }
        } else {
            //Rename and Paste into a Folder
        }
    }

    private void renameSameTypeNode(ProjectFolder projectFolder, AbstractNodeBase child, CopyInfo info) {
        child.rename(CopyServiceConstants.TEMPORARY_NAME);

        String copyDuplicated = " - " + RESOURCE_BUNDLE.getString("Copy");
        String name = info.node.getName();
        projectFolder.unarchive(info.archivePath.resolve(info.nodeId));
        projectFolder.getChild(name).ifPresent(pNode -> {
            if (!name.contains(copyDuplicated) && !projectFolder.getChild(name + copyDuplicated).isPresent()) {
                pNode.rename(name + copyDuplicated);
                child.rename(name);
            } else {
                if (!name.contains(copyDuplicated)) {
                    renameCopiedNode(projectFolder, copyDuplicated, child, name, pNode);
                } else {
                    renameCopiedNode(projectFolder, "", child, name, pNode);
                }
            }
        });
    }

    private static void renameCopiedNode(ProjectFolder projectFolder, String copy, AbstractNodeBase child, String name, ProjectNode projectNode) {
        String lastCopiedNode = projectFolder.getChildren().stream()
                .map(ProjectNode::getName)
                .filter(n -> n.startsWith(name + copy))
                .sorted(Comparator.reverseOrder())
                .collect(Collectors.toList())
                .get(0);

        String lastCopiedNodeNumber = lastCopiedNode.substring(lastCopiedNode.indexOf(name + copy) + name.length() + copy.length());
        if (lastCopiedNodeNumber.isEmpty()) {
            projectNode.rename(name + copy + 2);
            child.rename(name);
        } else {
            int numberOfCopies = Integer.parseInt(lastCopiedNodeNumber) + 1;
            projectNode.rename(name + copy + numberOfCopies);
            child.rename(name);
        }
    }

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
        node.archive(Paths.get(parentPath));
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

    private static String getStorageDirectory() {
        return LocalComputationConfig.load().getLocalDir() + CopyServiceConstants.PATH_SEPARATOR;
    }

    public static class CopyInfo {
        String nodeId;
        AbstractNodeBase node;
        Path archivePath;
        Boolean archiveSuccess;
        ZonedDateTime expirationDate;
    }
}
