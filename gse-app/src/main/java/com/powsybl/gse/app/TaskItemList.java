/**
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.gse.app;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.powsybl.afs.*;
import com.powsybl.gse.spi.GseContext;
import com.powsybl.gse.util.GseUtil;
import javafx.application.Platform;
import javafx.beans.Observable;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class TaskItemList {

    private static final Logger LOGGER = LoggerFactory.getLogger(TaskItemList.class);

    private final TaskMonitor taskMonitor;

    private final TaskListener taskListener;

    private final Deque<TaskEvent> eventBuffer = new ArrayDeque<>();

    private long initialRevision = 0;

    private final String projectId;

    private final Lock initLock = new ReentrantLock();

    private final ObservableList<TaskItem> items = FXCollections.observableArrayList(task -> new Observable[]{task.getMessage()});

    private final ObjectProperty<FilteredList<TaskItem>> displayItems = new SimpleObjectProperty<>();

    private final IntegerProperty hiddenTaskCount = new SimpleIntegerProperty();

    private final Config config;

    public TaskItemList(Project project, GseContext context) {
        Objects.requireNonNull(project);
        projectId = project.getId();
        config = retrieveConfig();
        displayItems.set(items.filtered(el -> !config.getHiddenTasks().contains(el.getId().toString())));
        taskMonitor = Objects.requireNonNull(project.getFileSystem().getTaskMonitor());
        taskListener = new TaskListener() {
            @Override
            public String getProjectId() {
                return project.getId();
            }

            @Override
            public void onEvent(TaskEvent event) {
                addEvent(event);
            }
        };

        GseUtil.execute(context.getExecutor(), () -> {
            // start recording events
            taskMonitor.addListener(taskListener);

            // initialize task list with current state
            reset();
        });
    }

    private void reset() {
        TaskMonitor.Snapshot snapshot = taskMonitor.takeSnapshot(projectId);
        initialRevision = snapshot.getRevision();
        initLock.lock();
        try {
            items.clear();
            for (TaskMonitor.Task task : snapshot.getTasks()) {
                items.add(new TaskItem(task.getId(), task.getName(), task.getMessage()));
            }
        } finally {
            initLock.unlock();
        }

        cleanupHiddenTaskConfig();
        processEvents();
    }

    public ObjectProperty<FilteredList<TaskItem>> getDisplayItems() {
        return displayItems;
    }

    private void addEvent(TaskEvent event) {
        initLock.lock();
        try {
            eventBuffer.add(event);
            processEvents();
        } finally {
            initLock.unlock();
        }
    }

    private void processEvents() {
        for (TaskEvent event : eventBuffer) {
            if (event.getRevision() > initialRevision) {
                if (event instanceof StartTaskEvent) {
                    StartTaskEvent startEvent = (StartTaskEvent) event;
                    Platform.runLater(() -> items.add(new TaskItem(startEvent.getTaskId(), startEvent.getName(), "")));
                } else if (event instanceof StopTaskEvent) {
                    Platform.runLater(() -> {
                        items.removeIf(taskItem -> taskItem.getId().equals(event.getTaskId()));
                        cleanupHiddenTaskConfig();
                    });
                } else if (event instanceof UpdateTaskMessageEvent) {
                    Platform.runLater(() -> items.stream()
                            .filter(task -> task.getId().equals(event.getTaskId()))
                            .findFirst()
                            .ifPresent(task -> task.getMessage().set(((UpdateTaskMessageEvent) event).getMessage())));
                } else {
                    throw new AssertionError();
                }
            }
        }
        eventBuffer.clear();
    }

    public void dispose() {
        try {
            Preferences.userNodeForPackage(getClass()).put("config", new ObjectMapper().writeValueAsString(config));
        } catch (JsonProcessingException e) {
            LOGGER.warn("Failed to save {} preferences", getClass(), e);
        }
        taskMonitor.removeListener(taskListener);
    }

    public IntegerProperty hiddenTaskCountProperty() {
        return hiddenTaskCount;
    }

    public void showAll() {
        config.setHiddenTasks(new ArrayList<>());
        reset();
    }

    public void hideTask(TaskItem taskItem) {
        String taskId = taskItem.getId().toString();
        if (!config.getHiddenTasks().contains(taskId)) {
            config.getHiddenTasks().add(taskId);
            hiddenTaskCount.set(config.getHiddenTasks().size());
        }
        displayItems.get().setPredicate(el -> !config.getHiddenTasks().contains(el.getId().toString()));
    }

    private Config retrieveConfig() {
        String serializedConfig = Preferences.userNodeForPackage(getClass()).get("config", null);
        if (serializedConfig != null) {
            try {
                return new ObjectMapper().readValue(serializedConfig, Config.class);
            } catch (IOException e) {
                LOGGER.error("Failed to retrieve {} config", getClass(), e);
            }
        }
        return new Config();
    }

    private void cleanupHiddenTaskConfig() {
        List<String> itemIds = items.stream().map(item -> item.getId().toString()).collect(Collectors.toList());
        config.setHiddenTasks(config.getHiddenTasks().stream().filter(itemIds::contains).collect(Collectors.toList()));
        hiddenTaskCount.set(config.getHiddenTasks().size());
    }

    public static class Config {
        private List<String> hiddenTasks = new ArrayList<>();

        public List<String> getHiddenTasks() {
            return hiddenTasks;
        }

        public void setHiddenTasks(List<String> hiddenTasks) {
            this.hiddenTasks = hiddenTasks;
        }
    }
}
