/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.gse.copy_paste.afs;

import com.powsybl.afs.*;
import com.powsybl.gse.copy_paste.afs.exceptions.*;
import javafx.scene.input.Clipboard;
import javafx.util.Pair;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
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
    private static final String TEMP_DIR_PREFIX = "powsybl_node_export";
    private static final long CLEANUP_DELAY = 36000;
    private static final long CLEANUP_PERIOD = 180000;

    private ExecutorService tPool = Executors.newCachedThreadPool();
    private Map<String, CopyInfo> currentCopies = new HashMap<>();

    public CopyManager() {
        init();
    }

    /**
     * @param nodes the nodes to copy
     */
    public Map<String, CopyInfo> copy(List<? extends AbstractNodeBase> nodes) throws CopyFailedException {
        Objects.requireNonNull(nodes);

        try {
            for (AbstractNodeBase node : nodes) {
                if (currentCopies.containsKey(node.getId()) && (currentCopies.get(node.getId()).archiveSuccess == null || currentCopies.get(node.getId()).archiveSuccess)) {
                    LOGGER.info("Skipping archiving of already ongoing copy {}", currentCopies.get(node.getId()));
                    currentCopies.get(node.getId()).expirationDate  = ZonedDateTime.now().plusHours(CopyServiceConstants.COPY_EXPIRATION_TIME);
                    continue;
                }

                CopyInfo copyInfo = new CopyInfo();
                copyInfo.archiveSuccess = null;
                copyInfo.nodeId = node.getId();
                copyInfo.node = node;
                copyInfo.archivePath = Files.createTempDirectory(TEMP_DIR_PREFIX);
                copyInfo.expirationDate = ZonedDateTime.now().plusHours(CopyServiceConstants.COPY_EXPIRATION_TIME);

                // create task
                Optional<ProjectNode> projectNode = (node instanceof ProjectNode) ? Optional.of((ProjectNode) node) : Optional.empty();
                Optional<TaskMonitor.Task> task = projectNode.map(pn -> pn.getFileSystem().getTaskMonitor().startTask(node.getName(), pn.getProject()));
                task.ifPresent(t -> copyInfo.taskId = t.getId());

                currentCopies.put(copyInfo.nodeId, copyInfo);

                tPool.execute(() -> {
                    LOGGER.info("Copying (archiving) node {} ({})", node.getName(), node.getId());

                    try {
                        node.archive(copyInfo.archivePath);
                        copyInfo.archiveSuccess = true;
                        LOGGER.info("Copying (archiving) node {} ({}) is complete", node.getName(), node.getId());
                    } catch (Exception e) {
                        copyInfo.archiveSuccess = false;
                        LOGGER.error("Copy has failed for node {}", node.getId(), e);
                    }

                    // end task
                    task.ifPresent(t -> projectNode.ifPresent(pn -> pn.getFileSystem().getTaskMonitor().stopTask(t.getId())));
                });

                deleteOnExit(copyInfo.archivePath.toFile());
            }
        } catch (IOException e) {
            throw new CopyFailedException(e);
        }
        return currentCopies;
    }

    /**
     * @param nodesIds the copied node's id
     * @param folder   the archive destination's folder
     */
    public void paste(String fileSystemName, List<String> nodesIds, AbstractNodeBase folder) throws CopyPasteException {
        Objects.requireNonNull(nodesIds);
        throwBadArgumentException(fileSystemName, folder);

        Map<Boolean, List<CopyInfo>> copyInfos = nodesIds
                .stream()
                .map(nodeId -> currentCopies.getOrDefault(nodeId, null))
                .filter(Objects::nonNull)
                .filter(copyInfo -> copyInfo.archiveSuccess != null)
                .collect(Collectors.groupingBy(copyInfo -> copyInfo.archiveSuccess));

        if (copyInfos.containsKey(false)) {
            throw new CopyPasteException(String.format("Failed to copy nodes %s", copyInfos.get(false).stream().map(CopyInfo::getNodeId).collect(Collectors.joining(","))));
        } else if (!copyInfos.containsKey(true) || copyInfos.get(true).size() != nodesIds.size()) {
            throw new CopyNotFinishedException();
        }

        for (CopyInfo copyInfo : copyInfos.get(true)) {
            List<? extends AbstractNodeBase> children;

            if (folder instanceof ProjectFolder) {
                children = ((ProjectFolder) folder).getChildren();
            } else {
                children = ((Folder) folder).getChildren();
            }

            if (children.stream().anyMatch(child -> copyInfo.node.getName().equals(child.getName()))) {
                // TODO fix name
                throw new CopyPasteFileAlreadyExistException();
            } else {
                LOGGER.info("Pasting node {} with origin id {}", copyInfo.node.getName(), copyInfo.nodeId);
                folder.unarchive(copyInfo.archivePath.resolve(copyInfo.nodeId));
            }
        }
    }

    private void init() {
        Timer timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                for (String nodeId : new ArrayList<>(currentCopies.keySet())) {
                    CopyInfo copyInfo = currentCopies.get(nodeId);
                    if (copyInfo.expirationDate.isBefore(ZonedDateTime.now())) {
                        currentCopies.remove(nodeId);
                        try {
                            FileUtils.deleteDirectory(new File(copyInfo.archivePath.toString()));
                        } catch (IOException e) {
                            LOGGER.error("Failed to delete temp directory archive {}", copyInfo.archivePath, e);
                        }
                    }
                }
            }
        }, CLEANUP_DELAY, CLEANUP_PERIOD);
    }

    private void renameAndPaste(AbstractNodeBase folder, List<? extends AbstractNodeBase> children, CopyInfo info) throws CopyPasteException {

        for (AbstractNodeBase child : children) {
            String name = child.getName();
            if (info.node.getName().equals(name)) {
                if (info.node.getClass().equals(child.getClass())) {
                    renameSameTypeNode((ProjectFolder) folder, child, info);
                } else {
                    throw new CopyPasteFileAlreadyExistException();
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
        UUID taskId;

        public UUID getTaskId() {
            return taskId;
        }

        public void setTaskId(UUID taskId) {
            this.taskId = taskId;
        }

        public String getNodeId() {
            return nodeId;
        }

        public void setNodeId(String nodeId) {
            this.nodeId = nodeId;
        }

        public AbstractNodeBase getNode() {
            return node;
        }

        public void setNode(AbstractNodeBase node) {
            this.node = node;
        }

        public Path getArchivePath() {
            return archivePath;
        }

        public void setArchivePath(Path archivePath) {
            this.archivePath = archivePath;
        }

        public Boolean getArchiveSuccess() {
            return archiveSuccess;
        }

        public void setArchiveSuccess(Boolean archiveSuccess) {
            this.archiveSuccess = archiveSuccess;
        }

        public ZonedDateTime getExpirationDate() {
            return expirationDate;
        }

        public void setExpirationDate(ZonedDateTime expirationDate) {
            this.expirationDate = expirationDate;
        }

        @Override
        public String toString() {
            return "CopyInfo{" +
                    "nodeId='" + nodeId + '\'' +
                    ", node=" + node +
                    ", archivePath=" + archivePath +
                    ", archiveSuccess=" + archiveSuccess +
                    ", expirationDate=" + expirationDate +
                    ", taskId=" + taskId +
                    '}';
        }
    }
}
