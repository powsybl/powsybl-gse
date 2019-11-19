/**
 * Copyright (c) 2017, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.gse.afs.ext.base;

import com.google.common.io.Files;
import com.powsybl.afs.AfsException;
import com.powsybl.afs.ProjectFolder;
import com.powsybl.afs.ext.base.ImportedCaseBuilder;
import com.powsybl.commons.datasource.FileDataSource;
import com.powsybl.gse.spi.GseContext;
import com.powsybl.gse.spi.ProjectCreationTask;
import com.powsybl.gse.spi.ProjectFileCreator;
import com.powsybl.gse.util.GseUtil;
import com.powsybl.iidm.parameters.Parameter;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;

import java.io.File;
import java.util.*;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class ImportedCaseCreator extends GridPane implements ProjectFileCreator {

    private static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle.getBundle("lang.ImportedCaseCreator");

    private final ProjectFolder folder;

    private final VBox parametersBox = new VBox(10);

    private final Preferences preferences;

    private final TextField caseFileTextField = new TextField();

    private final Button caseFileButton = new Button("...");

    private final SimpleObjectProperty<File> caseFileProperty = new SimpleObjectProperty<>();

    private final Map<String, String> parametersValue = new HashMap<>();

    ImportedCaseCreator(ProjectFolder folder, Scene scene, GseContext context) {
        this.folder = Objects.requireNonNull(folder);
        Objects.requireNonNull(scene);
        preferences = Preferences.userNodeForPackage(getClass());

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
        add(new Label(RESOURCE_BUNDLE.getString("XiidmFile") + ":"), 0, 0);
        add(caseFileTextField, 1, 0);
        add(caseFileButton, 2, 0);
        add(new Label(RESOURCE_BUNDLE.getString("Parameters")), 0, 1, 1, 1);
        add(new Separator(), 1, 1, 2, 1);
        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setContent(parametersBox);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        parametersBox.setPrefHeight(250);
        parametersBox.setPadding(new Insets(5, 5, 5, 5));
        add(scrollPane, 0, 2, 3, 1);
        caseFileButton.setOnAction(event -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle(RESOURCE_BUNDLE.getString("SelectXiidmFile"));
            String lastPath = preferences.get("caseLastSelectedPath", "");
            File lastPathFile = new File(lastPath);
            if (!lastPath.isEmpty() && lastPathFile.exists()) {
                fileChooser.setInitialDirectory(lastPathFile);
            }
            File file = fileChooser.showOpenDialog(scene.getWindow());
            if (file != null) {
                caseFileTextField.setText(file.toString());
                caseFileProperty.setValue(file);

                context.getExecutor().execute(() -> {
                    String name = Files.getNameWithoutExtension(caseFileProperty.getValue().toString());

                    if (folder.getChild(name).isPresent()) {
                        GseUtil.showDialogError(new AfsException(String.format(RESOURCE_BUNDLE.getString("NodeAlreadyExists"), name)));
                    }
                });
                preferences.put("caseLastSelectedPath", file.getParent());
            }
        });
        /*caseSelectionPane.nodeProperty().addListener((observable, oldCase, newCase) -> {
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
        });*/
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
        String name = Files.getNameWithoutExtension(caseFileProperty.getValue().toString());
        FileDataSource fileDataSource = new FileDataSource(caseFileProperty.getValue().toPath().getParent(), name);

        return new ProjectCreationTask() {

            @Override
            public String getNamePreview() {
                return name;
            }

            @Override
            public void run() {
                folder.fileBuilder(ImportedCaseBuilder.class)
                        .withDatasource(fileDataSource)
                        .withName(name)
                        .withParameters(parametersValue)
                        .build();
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
        caseFileProperty.setValue(null);
        return this;
    }

    @Override
    public BooleanBinding okProperty() {
        return caseFileProperty.isNotNull();
    }

    @Override
    public void dispose() {
    }
}
