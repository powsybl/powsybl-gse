/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.gse.copy_paste.afs;

import com.powsybl.afs.*;
import com.powsybl.computation.local.LocalComputationConfig;
import com.powsybl.gse.copy_paste.afs.exceptions.CopyDifferentFileSystemNameException;
import javafx.scene.control.Alert;
import javafx.scene.input.Clipboard;
import javafx.util.Pair;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileSystems;
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

    private static final Logger LOGGER = LoggerFactory.getLogger(CopyManager.class);

    private final String storageDirectory;

    private final Object copyLock = new Object();
    private ExecutorService tPool = Executors.newCachedThreadPool();
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
            copyInfo.archiveSuccess = null;

            tPool.execute(() -> {
                // create task
                TaskMonitor.Task task;
                if (node instanceof ProjectNode) {
                    task = ((ProjectNode) node).getFileSystem().getTaskMonitor().startTask(node.getName(), ((ProjectNode) node).getProject());
                } else {
                    task = null;
                }
                synchronized (copyLock) {
                    try {
                        archiveAndCopy(node);
                        copyInfo.archiveSuccess = true;
                    } catch (Exception e) {
                        copyInfo.archiveSuccess = false;
                        LOGGER.error("copy has failed", e);
                    }

                    // end task
                    if (node instanceof ProjectNode) {
                        ((ProjectNode) node).getFileSystem().getTaskMonitor().stopTask(task.getId());
                    }
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
     * @param nodesIds the copied node's id
     * @param folder   the archive destination's folder
     */
    public void paste(String fileSystemName, List<String> nodesIds, AbstractNodeBase folder) throws CopyPasteException {
        throwBadArgumentException(fileSystemName, folder);

        nodesIds.forEach(nodeId -> {
            CopyInfo copyInfo = currentCopies.get(nodeId);
            List children;
            synchronized (copyLock) {
                if (copyInfo != null && copyInfo.archiveSuccess) {

                    if (folder instanceof ProjectFolder) {
                        children = ((ProjectFolder) folder).getChildren();
                    } else {
                        children = ((Folder) folder).getChildren();
                    }
                    boolean nodeNameAlreadyExists = ((List<AbstractNodeBase>) children).stream().anyMatch(child -> copyInfo.node.getName().equals(child.getName()));

                    if (nodeNameAlreadyExists) {
                        renameAndPaste(folder, children, copyInfo);
                    } else {
                        folder.unarchive(copyInfo.archivePath.resolve(copyInfo.nodeId));
                    }
                }
            }
        });
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

    private void renameAndPaste(AbstractNodeBase folder, List<AbstractNodeBase> children, CopyInfo info) {

        for (AbstractNodeBase child : children) {
            String name = child.getName();
            if (info.node.getName().equals(name)) {
                if (info.node.getClass().equals(child.getClass())) {
                    renameSameTypeNode((ProjectFolder) folder, child, info);
                } else {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("Copy Error");
                    alert.setHeaderText(null);
                    alert.setContentText("File Already Exists");
                    alert.showAndWait();
                }
                break;
            }
        }
    }

    private void renameSameTypeNode(ProjectFolder projectFolder, AbstractNodeBase child, CopyInfo info) {
        child.rename(CopyServiceConstants.TEMPORARY_NAME);

        String copyDuplicated = " - " + "Copy";
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
        return LocalComputationConfig.load().getLocalDir() + FileSystems.getDefault().getSeparator();
    }

    private static void throwBadArgumentException(String fileSystemName, AbstractNodeBase folder) throws CopyDifferentFileSystemNameException {
        if (!(folder instanceof ProjectFolder || folder instanceof Folder)) {
            throw new IllegalArgumentException("the parameter must be a folder");
        }
        AppFileSystem destinationFileSystem = (folder instanceof Folder) ? ((Folder) folder).getFileSystem() : ((ProjectFolder) folder).getFileSystem();
        String destinationFileSystemName = destinationFileSystem.getName();

        if (!destinationFileSystemName.equals(fileSystemName)) {
            throw new CopyDifferentFileSystemNameException();
        }
    }

    public static StringBuilder copyParameters(List<? extends AbstractNodeBase> nodes) {
        AbstractNodeBase node = nodes.get(0);
        String fileSystemName = (node instanceof Node) ? ((Node) node).getFileSystem().getName() : ((ProjectNode) node).getFileSystem().getName();
        String pathSeparator = FileSystems.getDefault().getSeparator();
        StringBuilder copyParameters = new StringBuilder();

        copyParameters.append(CopyServiceConstants.COPY_SIGNATURE)
                .append(pathSeparator)
                .append(fileSystemName)
                .append(pathSeparator);
        nodes.forEach(nod -> copyParameters.append(nod.getId()).append(pathSeparator));
        return copyParameters;
    }

    public static Optional<Pair<List<String>, String>> getCopyInfo(Clipboard clipboard, String copyInfo) {
        if (clipboard.hasString() && copyInfo.contains(CopyServiceConstants.COPY_SIGNATURE)) {
            String[] copyInfoArray = copyInfo.split(FileSystems.getDefault().getSeparator());
            List<String> nodesIds = Arrays.asList(ArrayUtils.removeAll(copyInfoArray, 0, 1));
            String fileSystemName = copyInfoArray[1];
            return Optional.of(new Pair<>(nodesIds, fileSystemName));
        }
        return Optional.empty();
    }

    public static class CopyInfo {
        String nodeId;
        AbstractNodeBase node;
        Path archivePath;
        Boolean archiveSuccess;
        ZonedDateTime expirationDate;
    }
}
