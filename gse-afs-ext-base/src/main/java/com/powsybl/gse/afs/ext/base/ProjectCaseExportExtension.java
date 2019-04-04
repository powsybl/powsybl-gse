/**
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.gse.afs.ext.base;

import com.google.auto.service.AutoService;
import com.powsybl.afs.ProjectFile;
import com.powsybl.afs.TaskMonitor;
import com.powsybl.afs.ext.base.ProjectCase;
import com.powsybl.gse.spi.ExecutionTaskConfigurator;
import com.powsybl.gse.spi.GseContext;
import com.powsybl.gse.spi.ProjectFileExecutionTaskExtension;
import com.powsybl.gse.util.Glyph;
import com.powsybl.iidm.xml.NetworkXml;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.stage.FileChooser;

import java.io.*;
import java.nio.file.Files;
import java.text.MessageFormat;
import java.util.ResourceBundle;
import java.util.UUID;
import java.util.prefs.Preferences;
import java.util.zip.GZIPOutputStream;

/**
 * @author Marianne Funfrock <marianne.funfrock at rte-france.com>
 */
@AutoService(ProjectFileExecutionTaskExtension.class)
public class ProjectCaseExportExtension implements ProjectFileExecutionTaskExtension<ProjectFile, ProjectCaseExportParameters> {

    private static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle.getBundle("lang.ProjectCaseExport");

    private final Preferences preferences = Preferences.userNodeForPackage(getClass());

    @Override
    public Class<ProjectFile> getProjectFileType() {
        return ProjectFile.class;
    }

    @Override
    public Class<?> getAdditionalType() {
        return ProjectCase.class;
    }

    @Override
    public String getMenuText(ProjectFile projectCase) {
        return RESOURCE_BUNDLE.getString("CaseExport");
    }

    @Override
    public Node getMenuGraphic(ProjectFile file) {
        return Glyph.createAwesomeFont('\uf093').size("1em");
    }

    @Override
    public ExecutionTaskConfigurator<ProjectCaseExportParameters> createConfigurator(ProjectFile projectCase, Scene scene, GseContext context) {
        return new ExecutionTaskConfigurator<ProjectCaseExportParameters>() {

            private final GridPane main = new GridPane();

            private final ObjectProperty<ProjectCaseExportParameters> configProperty = new SimpleObjectProperty<>();

            private final TextField fileTextField = new TextField();

            private final Button fileButton = new Button("...");

            private final CheckBox zipCheckBox = new CheckBox(RESOURCE_BUNDLE.getString("CompressedFile"));

            private ProjectCaseExportParameters config = new ProjectCaseExportParameters();

            {
                main.setVgap(5);
                main.setHgap(5);
                main.setPrefWidth(400);
                ColumnConstraints column0 = new ColumnConstraints();
                ColumnConstraints column1 = new ColumnConstraints();
                column1.setHgrow(Priority.ALWAYS);
                fileTextField.setText(projectCase.getName() + ".xiidm");
                main.getColumnConstraints().addAll(column0, column1);
                main.add(new Label(RESOURCE_BUNDLE.getString("DestinationFile") + ":"), 0, 0);
                main.add(fileTextField, 1, 0);
                main.add(fileButton, 2, 0);
                main.add(zipCheckBox, 1, 1);

                // Add action events
                fileButton.setOnAction(event -> {
                    FileChooser fileChooser = new FileChooser();
                    openLastDirectory(fileChooser);
                    fileChooser.setTitle(RESOURCE_BUNDLE.getString("SelectTargetFile"));
                    fileChooser.setInitialFileName(fileTextField.getText());
                    File file = fileChooser.showSaveDialog(scene.getWindow());
                    if (file != null) {
                        fileTextField.setText(file.toString());
                        config.setFilePath(file.toString());
                        configProperty.set(config);
                        preferences.put("projectCaseLastSelectedExportedPath", file.getParent());
                    }
                });

                zipCheckBox.setSelected(config.isZipped());
                zipCheckBox.setOnAction(event -> config.setZipped(zipCheckBox.isSelected()));
            }

            @Override
            public String getTitle() {
                return RESOURCE_BUNDLE.getString("CaseExportParameters");
            }

            @Override
            public Node getContent() {
                return main;
            }

            @Override
            public ObjectProperty<ProjectCaseExportParameters> configProperty() {
                return configProperty;
            }

            public void dispose() {
                // nothing to dispose
            }
        };
    }

    private void openLastDirectory(FileChooser fileChooser) {
        String lastPath = preferences.get("projectCaseLastSelectedExportedPath", "");
        File lastPathFile = new File(lastPath);
        fileChooser.setInitialDirectory(!lastPath.isEmpty() && lastPathFile.exists() ? lastPathFile : new File(System.getProperty("user.home")));
    }

    private static OutputStream createOutputStream(ProjectCaseExportParameters config) throws IOException {
        return config.isZipped() ? new GZIPOutputStream(Files.newOutputStream(config.getFilePath()))
                : new BufferedOutputStream(Files.newOutputStream(config.getFilePath()));
    }

    @Override
    public void execute(ProjectFile projectCase, ProjectCaseExportParameters config) {
        TaskMonitor monitor = projectCase.getFileSystem().getTaskMonitor();
        UUID taskId = monitor.startTask(projectCase).getId();
        try (OutputStream outputStream = createOutputStream(config)) {
            monitor.updateTaskMessage(taskId, MessageFormat.format(RESOURCE_BUNDLE.getString("ExportCaseToIIDM"), config.getFilePath()));
            NetworkXml.write(((ProjectCase) projectCase).getNetwork(), outputStream);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            monitor.stopTask(taskId);
        }
    }
}
