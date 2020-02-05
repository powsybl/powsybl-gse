/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.gse.app;

import com.powsybl.afs.AfsException;
import com.powsybl.afs.Project;
import com.powsybl.afs.ProjectNode;
import com.powsybl.afs.TaskMonitor;
import com.powsybl.afs.storage.Utils;
import com.powsybl.gse.spi.GseContext;
import com.powsybl.gse.util.GseAlerts;
import com.powsybl.gse.util.NameTextField;
import javafx.application.Platform;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.stage.DirectoryChooser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.ResourceBundle;

/**
 * @author Valentin Berthault <valentin.berthault at rte-france.com>
 */
public class ArchivePane <T extends ProjectNode> extends GridPane {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProjectPane.class);

    private static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle.getBundle("lang.ArchivePane");

    protected final T node;

    protected NameTextField nameTextField;

    private final TextField directoryTextField = new TextField();

    private final Button directoryButton = new Button("...");

    private final SimpleObjectProperty<File> directoryProperty = new SimpleObjectProperty<>();

    private final CheckBox dependenciesCheck = new CheckBox(RESOURCE_BUNDLE.getString("DependenciesCheck"));

    public ArchivePane(T node, Scene scene, GseContext context) {
        this.node = Objects.requireNonNull(node);

        Project project = node.getProject();
        nameTextField = NameTextField.create(node);
        setVgap(5);
        setHgap(5);
        setPrefSize(600, 150);
        ColumnConstraints column0 = new ColumnConstraints();
        ColumnConstraints column1 = new ColumnConstraints();
        column1.setHgrow(Priority.ALWAYS);
        getColumnConstraints().addAll(column0, column1);
        add(nameTextField.getNameLabel(), 0, 0);
        add(nameTextField.getInputField(), 1, 0, 3, 1);
        add(new Label(RESOURCE_BUNDLE.getString("Directory")), 0, 1);
        add(directoryTextField, 1, 1);
        add(directoryButton, 2, 1);
        directoryTextField.setDisable(true);
        directoryButton.setOnAction(event -> {
            DirectoryChooser directoryChooser = new DirectoryChooser();
            directoryChooser.setTitle(RESOURCE_BUNDLE.getString("Archive"));
            File file = directoryChooser.showDialog(scene.getWindow());
            if (file != null) {
                directoryTextField.setText(file.toString());
                directoryProperty.setValue(file);
            }
        });
        addRow(2, dependenciesCheck);
        add(nameTextField.getFileAlreadyExistsLabel(), 0, 5, 3, 1);
        Platform.runLater(nameTextField.getInputField()::requestFocus);
    }

    public String getTitle() {
        return RESOURCE_BUNDLE.getString("Archive");
    }

    public Node getContent() {
        return this;
    }

    public BooleanBinding okProperty() {
        return directoryProperty.isNotNull()
                .and(nameTextField.okProperty());
    }

    public void run(Project project) {
        File directory = directoryProperty.getValue();
        Path directoryPath = directory.toPath().resolve(nameTextField.getText());
        TaskMonitor.Task task = project.getFileSystem().getTaskMonitor().startTask(String.format(RESOURCE_BUNDLE.getString("ArchiveTask"), directory.getName()), project);
        try {
            Files.createDirectory(directoryPath);
            Utils.checkDiskSpace(directory.toPath());
            node.archive(directoryPath, true, dependenciesCheck.isSelected());
            LOGGER.info("Archiving node {} ({}) is complete", node.getName(), node.getId());
        } catch (AfsException | IOException e) {
            GseAlerts.showDialogError(e.getMessage());
            LOGGER.error("Archiving has failed for node {}", node.getId(), e);
        } finally {
            project.getFileSystem().getTaskMonitor().stopTask(task.getId());
        }
    }
}
