package com.powsybl.gse.app;

import com.powsybl.afs.ProjectNode;
import com.powsybl.gse.spi.ProjectCreationTask;
import com.powsybl.gse.spi.ProjectFileCreator;
import com.powsybl.gse.util.GseUtil;
import com.powsybl.gse.util.NameTextField;
import javafx.application.Platform;
import javafx.beans.binding.BooleanBinding;
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
import javafx.stage.DirectoryChooser;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.ResourceBundle;

public class GseArchiveDialog<T extends ProjectNode>  extends GridPane implements ProjectFileCreator {

    private static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle.getBundle("lang.GseArchiveDialog");
    private final T node;
    private NameTextField nameArchive;
    private File archiveLocationSelection;
    private final Button selectionFileButton = new Button("...");
    private final TextField selectionFileTextField = new TextField();
    private final CheckBox dependenciesArchive;
    private final SimpleObjectProperty<File> directoryProperty = new SimpleObjectProperty<>();

    public GseArchiveDialog(T node, Scene scene) {
        this.node = Objects.requireNonNull(node);

        nameArchive = NameTextField.create(node);
        dependenciesArchive  = new CheckBox(RESOURCE_BUNDLE.getString("Dependencies"));
        setVgap(5);
        setHgap(5);
        setPrefSize(800, 300);
        ColumnConstraints column0 = new ColumnConstraints();
        ColumnConstraints column1 = new ColumnConstraints();
        column1.setHgrow(Priority.ALWAYS);
        getColumnConstraints().addAll(column0, column1);
        add(new Label(RESOURCE_BUNDLE.getString("Dossier") + ":"), 0, 0);
        add(selectionFileTextField, 1, 0);
        add(selectionFileButton, 2, 0);
        add(nameArchive.getNameLabel(), 0, 1);
        add(nameArchive.getInputField(), 1, 1, 3, 1);
        add(dependenciesArchive, 1, 2);
        selectionFileTextField.setDisable(true);
        selectionFileButton.setOnAction(event -> {
            DirectoryChooser archiveLocationSelectionDirectory = new DirectoryChooser();
            archiveLocationSelectionDirectory.setTitle(RESOURCE_BUNDLE.getString("SelectDirectory"));
            archiveLocationSelection = archiveLocationSelectionDirectory.showDialog(scene.getWindow());
            selectionFileTextField.setText(archiveLocationSelection.toString());
            directoryProperty.setValue(archiveLocationSelection);
        });
        Platform.runLater(nameArchive.getInputField()::requestFocus);
    }

    @Override
    public String getTitle() {
        return RESOURCE_BUNDLE.getString("ArchiveDialogTitle");
    }
    @Override
    public Node getContent() {
        return this;
    }

    @Override
    public BooleanBinding okProperty() {
        return directoryProperty.isNotNull().and(nameArchive.okProperty());
    }

    @Override
    public ProjectCreationTask createTask() {
        String name = nameArchive.getText();
        return new ProjectCreationTask() {
            @Override
            public String getNamePreview() {
                return name;
            }

            @Override
            public void run() {
                Path newDirectory = archiveLocationSelection.toPath().resolve(nameArchive.getText());
                try {
                    Files.createDirectory(newDirectory);
                } catch (IOException e) {
                    GseUtil.showDialogError(e);
                }
                node.archive(newDirectory);
            }

            @Override
            public void undo() {
                // TODO

            }

            @Override
            public void redo() {
                // TODO
            }
        };
    }

    @Override
    public void dispose() {
        //Nothing to dispose
    }
}
