/**
 * Copyright (c) 2017, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.gse.afs.ext.base;

import com.powsybl.afs.ProjectFile;
import com.powsybl.afs.ProjectFolder;
import com.powsybl.afs.ProjectNode;
import com.powsybl.afs.ext.base.Case;
import com.powsybl.afs.ext.base.ImportedCaseBuilder;
import com.powsybl.gse.spi.GseContext;
import com.powsybl.gse.spi.ProjectCreationTask;
import com.powsybl.gse.spi.ProjectFileCreator;
import com.powsybl.gse.util.GseAlerts;
import com.powsybl.gse.util.NodeSelectionPane;
import com.powsybl.gse.util.RenamePane;
import com.powsybl.iidm.parameters.Parameter;
import javafx.application.Platform;
import javafx.beans.binding.BooleanBinding;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class ImportedCaseCreator extends GridPane implements ProjectFileCreator {

    private static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle.getBundle("lang.ImportedCaseCreator");

    private static final String TEMPORARY_NAME = "temporaryName";

    private final ProjectFolder folder;

    private final VBox parametersBox = new VBox(10);

    private final NodeSelectionPane<Case> caseSelectionPane;

    private final Map<String, String> parametersValue = new HashMap<>();

    private final Scene scene;

    private List<ProjectFile> backwardDependencies = new ArrayList<>();

    ImportedCaseCreator(ProjectFolder folder, Scene scene, GseContext context) {
        this.folder = Objects.requireNonNull(folder);
        this.scene = Objects.requireNonNull(scene);
        caseSelectionPane = new NodeSelectionPane<>(folder.getFileSystem().getData(), RESOURCE_BUNDLE.getString("Case"),
                                                    scene.getWindow(), context, Case.class);
        setVgap(5);
        setHgap(5);
        setPrefWidth(450);
        ColumnConstraints column0 = new ColumnConstraints();
        ColumnConstraints column1 = new ColumnConstraints();
        column1.setHgrow(Priority.ALWAYS);
        getColumnConstraints().addAll(column0, column1);
        RowConstraints row0 = new RowConstraints();
        RowConstraints row1 = new RowConstraints();
        RowConstraints row2 = new RowConstraints();
        row2.setVgrow(Priority.ALWAYS);
        getRowConstraints().addAll(row0, row1, row2);
        add(caseSelectionPane.getLabel(), 0, 0);
        add(caseSelectionPane.getTextField(), 1, 0);
        add(caseSelectionPane.getButton(), 2, 0);
        add(new Label(RESOURCE_BUNDLE.getString("Parameters")), 0, 1, 1, 1);
        add(new Separator(), 1, 1, 2, 1);
        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setContent(parametersBox);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        parametersBox.setPrefHeight(250);
        parametersBox.setPadding(new Insets(5, 5, 5, 5));
        add(scrollPane, 0, 2, 3, 1);
        caseSelectionPane.nodeProperty().addListener((observable, oldCase, newCase) -> {
            if (newCase != null) {
                List<javafx.scene.Node> parameterNodes = new ArrayList<>();
                for (Parameter parameter : newCase.getImporter().getParameters()) {
                    switch (parameter.getType()) {
                        case BOOLEAN:
                            parameterNodes.add(createBooleanComponent(parameter));
                            break;
                        case STRING:
                            parameterNodes.add(createStringComponent(parameter));
                            break;
                        case STRING_LIST:
                            parameterNodes.add(createListStringComponent(parameter));
                            break;
                        default:
                            throw new AssertionError("TODO");
                    }
                }
                parametersBox.getChildren().setAll(parameterNodes);
            } else {
                parametersBox.getChildren().clear();
            }
        });
    }

    private javafx.scene.Node createBooleanComponent(Parameter parameter) {
        CheckBox checkBox = new CheckBox(parameter.getDescription());
        checkBox.setSelected((Boolean) parameter.getDefaultValue());
        checkBox.selectedProperty().addListener((observable, oldValue, newValue) -> parametersValue.put(parameter.getName(), newValue.toString()));
        return checkBox;
    }

    private javafx.scene.Node createStringComponent(Parameter parameter) {
        TextField textField = new TextField((String) parameter.getDefaultValue());
        textField.textProperty().addListener((observable, oldValue, newValue) -> parametersValue.put(parameter.getName(), newValue));
        return new VBox(5, new Label(parameter.getDescription()), textField);
    }

    private javafx.scene.Node createListStringComponent(Parameter parameter) {
        TextArea textArea = new TextArea(((List<String>) parameter.getDefaultValue()).stream().collect(Collectors.joining(System.lineSeparator())));
        textArea.setPrefColumnCount(20);
        textArea.setPrefRowCount(5);
        textArea.textProperty().addListener((observable, oldValue, newValue) -> parametersValue.put(parameter.getName(), newValue.replace(System.lineSeparator(), ",")));
        return new VBox(5, new Label(parameter.getDescription()), textArea);
    }

    @Override
    public String getTitle() {
        return RESOURCE_BUNDLE.getString("ImportCase");
    }

    @Override
    public ProjectCreationTask createTask() {
        Case aCase = caseSelectionPane.nodeProperty().getValue();
        return new ProjectCreationTask() {

            @Override
            public String getNamePreview() {
                return aCase.getName();
            }

            @Override
            public void run() {
                Optional<ProjectNode> child = folder.getChild(aCase.getName());
                if (child.isPresent()) {
                    Platform.runLater(() -> replaceNode(folder, aCase, scene));
                } else {
                    buildFile(aCase, folder, null);
                }
            }

            @Override
            public void undo() {
                throw new AssertionError("TODO"); // TODO
            }

            @Override
            public void redo() {
                throw new AssertionError("TODO"); // TODO
            }
        };
    }

    @Override
    public Node getContent() {
        caseSelectionPane.nodeProperty().setValue(null);
        return this;
    }

    @Override
    public BooleanBinding okProperty() {
        return caseSelectionPane.nodeProperty().isNotNull();
    }

    @Override
    public void dispose() {
    }

    private void replaceNode(ProjectFolder folder, Case aCase, Scene scene) {
        String name = aCase.getName();
        Optional<ButtonType> result = GseAlerts.showReplaceAndQuitDialog(folder.getName(), name);
        result.ifPresent(buttonType -> {
            if (buttonType.getButtonData() == ButtonBar.ButtonData.YES) {
                folder.getChild(name).ifPresent(projectNode -> {
                    backwardDependencies = projectNode.getBackwardDependencies();
                    projectNode.rename(TEMPORARY_NAME);
                    buildFile(aCase, folder, null);
                    folder.getChild(name).ifPresent(importedNode ->
                            backwardDependencies.forEach(projectFile -> projectFile.replaceDependencies(projectNode.getId(), importedNode)));
                    projectNode.delete();
                    backwardDependencies.clear();
                });
            } else if (buttonType.getButtonData() == ButtonBar.ButtonData.OTHER) {
                folder.getChild(name).ifPresent(projectNode -> {
                    Optional<String> text = RenamePane.showAndWaitDialog(scene.getWindow(), projectNode);
                    text.ifPresent(newName -> {
                        if (!folder.getChild(newName).isPresent()) {
                            buildFile(aCase, folder, newName);
                        } else {
                            replaceNode(folder, aCase, scene);
                        }
                    });
                });
            }
        });
    }

    private void buildFile(Case aCase, ProjectFolder folder, String name) {
        ImportedCaseBuilder importedCaseBuilder = folder.fileBuilder(ImportedCaseBuilder.class)
                .withCase(aCase)
                .withParameters(parametersValue);
        if (name != null) {
            importedCaseBuilder.withName(name);
        }
        importedCaseBuilder.build();
    }
}
