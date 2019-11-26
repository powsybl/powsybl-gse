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
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

/**
 * @author Nassirou Nambiema <nassirou.nambiena at rte-france.com>
 */
public final class CopyManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(CopyManager.class);
    private static final String TEMP_DIR_PREFIX = "powsybl_node_export";
    private static final long CLEANUP_DELAY = 36000;
    private static final long CLEANUP_PERIOD = 180000;
    private static final String COPY_INFO_SEPARATOR = "/";
    private static final String COPY_NODE_INFO_SEPARATOR = ";";

    private static CopyManager INSTANCE = null;

    private ExecutorService tPool = Executors.newCachedThreadPool();
    private Map<String, CopyInfo> currentCopies = new HashMap<>();

    public static CopyManager getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new CopyManager();
        }
        return INSTANCE;
    }

    private CopyManager() {
        init();
    }

    public Map<String, CopyInfo> copy(List<? extends AbstractNodeBase> nodes) throws CopyPasteException {
        return copy(nodes, null);
    }

    /**
     * @param nodes the nodes to copy
     */
    public Map<String, CopyInfo> copy(List<? extends AbstractNodeBase> nodes, @Nullable File targetDirectory) throws CopyPasteException {
        Objects.requireNonNull(nodes);

        try {
            for (AbstractNodeBase node : nodes) {
                if (currentCopies.containsKey(node.getId()) && (currentCopies.get(node.getId()).archiveSuccess == null || currentCopies.get(node.getId()).archiveSuccess)) {
                    LOGGER.info("Skipping archiving of already ongoing copy {}", currentCopies.get(node.getId()));
                    currentCopies.get(node.getId()).expirationDate = ZonedDateTime.now().plusHours(CopyServiceConstants.COPY_EXPIRATION_TIME);
                    continue;
                }

                CopyInfo copyInfo = new CopyInfo();
                copyInfo.archiveSuccess = null;
                copyInfo.nodeId = node.getId();
                copyInfo.node = node;
                copyInfo.archivePath = resolveArchiveTargetDirectory(targetDirectory);
                copyInfo.expirationDate = ZonedDateTime.now().plusHours(CopyServiceConstants.COPY_EXPIRATION_TIME);

                currentCopies.put(copyInfo.nodeId, copyInfo);

                LOGGER.info("Copying (archiving) node {} ({})", node.getName(), node.getId());

                try {
                    node.archive(copyInfo.archivePath);
                    copyInfo.archiveSuccess = true;
                    LOGGER.info("Copying (archiving) node {} ({}) is complete", node.getName(), node.getId());
                } catch (Exception e) {
                    copyInfo.archiveSuccess = false;
                    LOGGER.error("Copy has failed for node {}", node.getId(), e);
                }

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

        List<String> newNodeNames = new ArrayList<>();
        for (CopyInfo copyInfo : copyInfos.get(true)) {
            List<? extends AbstractNodeBase> children;
            String nodeNewName = copyInfo.getNode().getName();

            if (folder instanceof ProjectFolder) {
                children = ((ProjectFolder) folder).getChildren();
            } else {
                children = ((Folder) folder).getChildren();
            }

            if (children.stream().anyMatch(child -> copyInfo.node.getName().equals(child.getName()))) {
                nodeNewName = renameAndPaste(folder, children, copyInfo);
            } else {
                LOGGER.info("Pasting node {} with origin id {}", copyInfo.node.getName(), copyInfo.nodeId);
                folder.unarchive(copyInfo.archivePath.resolve(copyInfo.nodeId));
            }

            newNodeNames.add(nodeNewName);
        }

        newNodeNames.forEach(newNodeName -> {
            if (folder instanceof ProjectFolder) {
                ((ProjectFolder) folder).getChild(newNodeName).ifPresent(this::clearOutOfProjectDependencies);
            }
        });
    }

    private void clearOutOfProjectDependencies(ProjectNode copiedNode) {
        if (copiedNode.isFolder()) {
            ((ProjectFolder) copiedNode).getChildren().forEach(this::clearOutOfProjectDependencies);
        } else if (copiedNode instanceof ProjectFile) {
            ProjectFile projectFile = (ProjectFile) copiedNode;
            List<ProjectDependency<ProjectNode>> deps = ((ProjectFile) copiedNode).getDependencies();
            deps.stream()
                    .filter(dep -> {
                        if (dep.getProjectNode() instanceof ProjectFile) {
                            ProjectFile realDep = projectFile.getProject().getFileSystem().findProjectFile(dep.getProjectNode().getId(), ((ProjectFile) dep.getProjectNode()).getClass());
                            return !Objects.equals(realDep.getProject().getId(), projectFile.getProject().getId());
                        } else {
                            LOGGER.warn("Dependency that is not a project file found when clearing out dep after pasting");
                        }
                        return false;
                    })
                    .forEach(dep -> projectFile.removeDependencies(dep.getName()));
        }
    }

    private Path resolveArchiveTargetDirectory(File directoryProp) throws IOException, CopyNotEmptyArchiveDirectoryException {
        if (directoryProp == null) {
            return Files.createTempDirectory(TEMP_DIR_PREFIX);
        }

        if (!directoryProp.exists() || !directoryProp.canWrite() || !directoryProp.isDirectory()) {
            throw new IOException();
        }

        File[] children = directoryProp.listFiles();
        if (children != null && children.length != 0) {
            throw new CopyNotEmptyArchiveDirectoryException();
        }

        return directoryProp.toPath();
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

    private String renameAndPaste(AbstractNodeBase folder, List<? extends AbstractNodeBase> children, CopyInfo info) throws CopyPasteException {

        for (AbstractNodeBase child : children) {
            String name = child.getName();
            if (info.node.getName().equals(name)) {
                if (info.node.getClass().equals(child.getClass()) && folder instanceof ProjectFolder) {
                    return renameSameTypeNode((ProjectFolder) folder, child, info);
                }
                throw new CopyPasteFileAlreadyExistException();
            }
        }
        throw new CopyPasteException("Failed to rename new node");
    }

    private String renameSameTypeNode(ProjectFolder projectFolder, AbstractNodeBase child, CopyInfo info) throws CopyPasteFileAlreadyExistException {
        String name = info.node.getName();
        String copyDuplicated = " - " + "Copy";
        String copyNameBaseName = name + copyDuplicated;
        AtomicReference<String> copyName = new AtomicReference<>(copyNameBaseName);
        try {
            info.node.rename(name + UUID.randomUUID().toString());
            projectFolder.unarchive(info.archivePath.resolve(info.nodeId));

            projectFolder.getChild(name).ifPresent(newNode -> {
                AbstractNodeBase childWithSameName = child;
                int maxLoop = 100;
                int curLoop = 1;
                while (childWithSameName != null && curLoop < maxLoop) {
                    if (curLoop != 1) {
                        copyName.set(copyNameBaseName + " (" + curLoop + ")");
                    }
                    childWithSameName = projectFolder.getChild(copyName.get()).orElse(null);
                    curLoop++;
                }

                if (childWithSameName != null) {
                    LOGGER.error("Failed to resolve copy name for node {}", newNode);
                    copyName.set(copyNameBaseName + " (" + UUID.randomUUID().toString() + ")");
                }

                newNode.rename(copyName.get());
            });

        } finally {
            info.node.rename(name);
        }

        return copyName.get();
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
        StringBuilder copyParameters = new StringBuilder();

        copyParameters.append(CopyServiceConstants.COPY_SIGNATURE)
                .append(COPY_INFO_SEPARATOR)
                .append(fileSystemName)
                .append(COPY_INFO_SEPARATOR);
        nodes.forEach(nod -> copyParameters.append(nod.getId()).append(COPY_NODE_INFO_SEPARATOR).append(node.getName().replaceAll(COPY_INFO_SEPARATOR, "").replaceAll(COPY_NODE_INFO_SEPARATOR, "")).append(COPY_INFO_SEPARATOR));
        return copyParameters;
    }

    public static Optional<CopyParams> getCopyInfo(Clipboard clipboard, String copyInfo) {
        if (clipboard.hasString() && copyInfo.contains(CopyServiceConstants.COPY_SIGNATURE)) {
            String[] copyInfoArray = copyInfo.split(COPY_INFO_SEPARATOR);
            List<CopyParams.NodeInfo> nodesInfos = Arrays.stream(ArrayUtils.removeAll(copyInfoArray, 0, 1))
                    .map(nodeInfo -> {
                        String[] itemInfo = nodeInfo.split(COPY_NODE_INFO_SEPARATOR);
                        return new CopyParams.NodeInfo(itemInfo[0], itemInfo.length > 1 ? itemInfo[1] : null);
                    })
                    .collect(Collectors.toList());
            String fileSystemName = copyInfoArray[1];
            return Optional.of(new CopyParams(fileSystemName, nodesInfos));
        }
        return Optional.empty();
    }

    public static class CopyParams {
        String fileSystem;
        List<NodeInfo> nodeInfos;

        public CopyParams(String fileSystem, List<NodeInfo> nodeInfos) {
            this.fileSystem = fileSystem;
            this.nodeInfos = nodeInfos;
        }

        public String getFileSystem() {
            return fileSystem;
        }

        public void setFileSystem(String fileSystem) {
            this.fileSystem = fileSystem;
        }

        public List<NodeInfo> getNodeInfos() {
            return nodeInfos;
        }

        public void setNodeInfos(List<NodeInfo> nodeInfos) {
            this.nodeInfos = nodeInfos;
        }

        public static class NodeInfo {
            String id;
            String name;

            public NodeInfo(String id, String name) {
                this.id = id;
                this.name = name;
            }

            public String getId() {
                return id;
            }

            public void setId(String id) {
                this.id = id;
            }

            public String getName() {
                return name;
            }

            public void setName(String name) {
                this.name = name;
            }
        }
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
