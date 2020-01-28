/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.gse.copy_paste.afs;

import com.powsybl.afs.*;
import com.powsybl.afs.storage.Utils;
import com.powsybl.commons.util.ServiceLoaderCache;
import com.powsybl.computation.local.LocalComputationManager;
import com.powsybl.gse.copy_paste.afs.exceptions.*;
import com.powsybl.gse.spi.ProjectFileExecutionTaskExtension;
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
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * @author Nassirou Nambiema <nassirou.nambiena at rte-france.com>
 */
public final class CopyManager {

    private static final String PROJECT_NODE_COPY_TYPE = "@PROJECT_NODE@";
    private static final String NODE_COPY_TYPE = "@NODE@";
    private static final String COPY_SIGNATURE = "@COPY_SIGNATURE@";
    private static final long COPY_EXPIRATION_TIME = 6;
    private static final long MIN_DISK_SPACE_THRESHOLD = 10;
    private static final Logger LOGGER = LoggerFactory.getLogger(CopyManager.class);
    private static final String TEMP_DIR_PREFIX = "powsybl_node_export";
    private static final long CLEANUP_DELAY = 36000;
    private static final long CLEANUP_PERIOD = 180000;
    private static final String COPY_INFO_SEPARATOR = "/";
    private static final String COPY_NODE_INFO_SEPARATOR = ";";
    private static final ServiceLoaderCache<ProjectFileExecutionTaskExtension> PROJECT_FILE_EXECUTION_TASK_EXTENSIONS = new ServiceLoaderCache<>(ProjectFileExecutionTaskExtension.class);

    private static CopyManager INSTANCE = null;

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

    public void copyPaste(List<? extends AbstractNodeBase> nodes, AbstractNodeBase targetFolder) throws CopyPasteException, IOException {
        String fileSystem = getCommonFileSystem(nodes);
        Optional<Project> targetProjectOpt = (targetFolder instanceof ProjectNode) ? Optional.of(((ProjectNode) targetFolder).getProject()) : Optional.empty();
        Optional<TaskMonitor.Task> task = targetProjectOpt.map(targetProject -> targetProject.getFileSystem().getTaskMonitor().startTask("Copying", targetProject));

        Consumer<String> logger = task
                .map(tsk -> (Consumer<String>) msg -> targetProjectOpt.get().getFileSystem().getTaskMonitor().updateTaskMessage(tsk.getId(), msg))
                .orElse(LOGGER::debug);
        try {
            copy(nodes, null, logger);
            paste(fileSystem, nodes.stream().map(AbstractNodeBase::getId).collect(Collectors.toList()), targetFolder, logger);
        } finally {
            task.ifPresent(taskEl -> targetProjectOpt.get().getFileSystem().getTaskMonitor().stopTask(taskEl.getId()));
        }

    }

    public Map<String, CopyInfo> copy(List<? extends AbstractNodeBase> nodes) throws CopyPasteException {
        return copy(nodes, null);
    }

    public Map<String, CopyInfo> copy(List<? extends AbstractNodeBase> nodes, @Nullable File targetDirectory) throws CopyPasteException {
        return copy(nodes, targetDirectory, LOGGER::debug);
    }

    /**
     * @param nodes the nodes to copy
     */
    public Map<String, CopyInfo> copy(List<? extends AbstractNodeBase> nodes, @Nullable File targetDirectory, Consumer<String> logger) throws CopyPasteException {
        Objects.requireNonNull(nodes);

        try {
            for (AbstractNodeBase node : nodes) {
                if (currentCopies.containsKey(node.getId()) && (currentCopies.get(node.getId()).archiveSuccess == null || currentCopies.get(node.getId()).archiveSuccess)) {
                    LOGGER.info("Skipping archiving of already ongoing copy {}", currentCopies.get(node.getId()));
                    currentCopies.get(node.getId()).expirationDate = ZonedDateTime.now().plusHours(COPY_EXPIRATION_TIME);
                    continue;
                }

                CopyInfo copyInfo = new CopyInfo();
                copyInfo.archiveSuccess = null;
                copyInfo.nodeId = node.getId();
                copyInfo.node = node;
                copyInfo.archivePath = resolveArchiveTargetDirectory(targetDirectory);
                copyInfo.expirationDate = ZonedDateTime.now().plusHours(COPY_EXPIRATION_TIME);

                currentCopies.put(copyInfo.nodeId, copyInfo);

                LOGGER.info("Copying (archiving) node {} ({})", node.getName(), node.getId());
                logger.accept(String.format("Copying node %s", copyInfo.getNode().getName()));
                try {
                    Utils.checkDiskSpace(copyInfo.archivePath);
                    node.archive(copyInfo.archivePath);
                    copyInfo.archiveSuccess = true;
                    LOGGER.info("Copying (archiving) node {} ({}) is complete", node.getName(), node.getId());
                } catch (Exception e) {
                    copyInfo.archiveSuccess = false;
                    LOGGER.error("Copy has failed for node {}", node.getId(), e);
                }
            }
        } catch (IOException e) {
            throw new CopyFailedException(e);
        }
        return currentCopies;
    }

    public void paste(String fileSystemName, List<String> nodesIds, AbstractNodeBase folder) throws CopyPasteException, IOException {
        paste(fileSystemName, nodesIds, folder, LOGGER::debug);
    }

    /**
     * @param nodesIds the copied node's id
     * @param folder   the archive destination's folder
     */
    public void paste(String fileSystemName, List<String> nodesIds, AbstractNodeBase folder, Consumer<String> logger) throws CopyPasteException, IOException {
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

            logger.accept(String.format("Pasting node %s", copyInfo.getNode().getName()));
            if (children.stream().anyMatch(child -> copyInfo.node.getName().equals(child.getName()))) {
                nodeNewName = renameAndPaste(folder, children, copyInfo);
            } else {
                LOGGER.info("Pasting node {} with origin id {}", copyInfo.node.getName(), copyInfo.nodeId);
                folder.unarchive(copyInfo.archivePath.resolve(copyInfo.nodeId));
            }

            newNodeNames.add(nodeNewName);
        }

        // cleaning unwanted data paste (out of project links, previous execution results)
        newNodeNames.forEach(newNodeName -> {
            if (folder instanceof ProjectFolder) {
                ((ProjectFolder) folder).getChild(newNodeName).ifPresent(projectNode -> {
                    clearOutOfProjectDependencies(projectNode);
                    cleanPastedObject(projectNode);
                });
            }
        });
    }

    private void cleanPastedObject(ProjectNode copiedNode) {
        if (copiedNode instanceof ProjectFolder) {
            ((ProjectFolder) copiedNode).getChildren().forEach(this::cleanPastedObject);
        } else if (copiedNode instanceof ProjectFile) {
            List<ProjectFileExecutionTaskExtension> services = PROJECT_FILE_EXECUTION_TASK_EXTENSIONS.getServices();
            services
                    .stream()
                    .filter(service -> service.getProjectFileType().isAssignableFrom(copiedNode.getClass()) && (service.getAdditionalType() == null || service.getAdditionalType().isAssignableFrom(copiedNode.getClass())))
                    .forEach(service -> service.clearResults((ProjectFile) copiedNode));
        } else {
            throw new AssertionError();
        }
    }

    private void clearOutOfProjectDependencies(ProjectNode copiedNode) {
        if (copiedNode instanceof ProjectFolder) {
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
            return Files.createTempDirectory(LocalComputationManager.getDefault().getLocalDir(), TEMP_DIR_PREFIX);
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
        Timer timer = new Timer(true);
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

    private String renameAndPaste(AbstractNodeBase folder, List<? extends AbstractNodeBase> children, CopyInfo info) throws CopyPasteException, IOException {
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

    private String renameSameTypeNode(ProjectFolder projectFolder, AbstractNodeBase child, CopyInfo info) throws IOException {
        String name = info.node.getName();
        String copyDuplicated = " - " + "Copy";
        String copyNameBaseName = name + copyDuplicated;
        AtomicReference<String> copyName = new AtomicReference<>(copyNameBaseName);
        try {
            child.rename(name + UUID.randomUUID().toString());
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
            child.rename(name);
        }

        return copyName.get();
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
        boolean isProjectNode = node instanceof ProjectNode;
        String fileSystemName = isProjectNode ? ((ProjectNode) node).getFileSystem().getName() : ((Node) node).getFileSystem().getName();
        StringBuilder copyParameters = new StringBuilder();

        copyParameters.append(COPY_SIGNATURE)
                .append(isProjectNode ? PROJECT_NODE_COPY_TYPE : NODE_COPY_TYPE)
                .append(COPY_INFO_SEPARATOR)
                .append(fileSystemName)
                .append(COPY_INFO_SEPARATOR);
        nodes.forEach(nod -> copyParameters.append(nod.getId()).append(COPY_NODE_INFO_SEPARATOR).append(nod.getName().replaceAll(COPY_INFO_SEPARATOR, "").replaceAll(COPY_NODE_INFO_SEPARATOR, "")).append(COPY_INFO_SEPARATOR));
        return copyParameters;
    }

    public static Optional<CopyParams> getCopyInfo(Clipboard clipboard) {
        if (clipboard.hasString() && clipboard.getString() != null && clipboard.getString().contains(COPY_SIGNATURE)) {
            String[] copyInfoArray = clipboard.getString().split(COPY_INFO_SEPARATOR);
            List<CopyParams.NodeInfo> nodesInfos = Arrays.stream(ArrayUtils.removeAll(copyInfoArray, 0, 1))
                    .map(nodeInfo -> {
                        String[] itemInfo = nodeInfo.split(COPY_NODE_INFO_SEPARATOR);
                        return new CopyParams.NodeInfo(itemInfo[0], itemInfo.length > 1 ? itemInfo[1] : null);
                    })
                    .collect(Collectors.toList());
            String fileSystemName = copyInfoArray[1];
            return Optional.of(new CopyParams(fileSystemName, nodesInfos, clipboard.getString().contains(PROJECT_NODE_COPY_TYPE)));
        }
        return Optional.empty();
    }

    private static String getCommonFileSystem(List<? extends AbstractNodeBase> nodes) throws CopyPasteException {
        try {
            return nodes.stream().map(node -> {
                if (node instanceof ProjectNode) {
                    return ((ProjectNode) node).getFileSystem().getName();
                } else if (node instanceof Node) {
                    return ((Node) node).getFileSystem().getName();
                }
                return null;
            }).reduce((fs1, fs2) -> {
                if (Objects.equals(fs1, fs2)) {
                    return fs1;
                }
                throw new RuntimeException("Can't copy nodes from multiple filesystem");
            }).orElseThrow(() -> new CopyPasteException("Fail to retrieve copied node filesystem name"));
        } catch (Exception e) {
            throw new CopyPasteException("Fail to retrieve copied node filesystem name", e);
        }
    }

    public static class CopyParams {
        String fileSystem;
        List<NodeInfo> nodeInfos;
        boolean isProjectNodeType;

        public CopyParams(String fileSystem, List<NodeInfo> nodeInfos, boolean isProjectNodeType) {
            this.fileSystem = fileSystem;
            this.nodeInfos = nodeInfos;
            this.isProjectNodeType = isProjectNodeType;
        }

        public boolean getProjectNodeType() {
            return isProjectNodeType;
        }

        public void setProjectNodeType(boolean projectNodeType) {
            this.isProjectNodeType = projectNodeType;
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
