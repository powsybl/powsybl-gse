/**
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.gse.spi;

import com.powsybl.tools.Version;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import org.apache.commons.lang3.SystemUtils;
import org.joda.time.DateTime;

import java.util.Collections;
import java.util.List;
import java.util.ResourceBundle;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class DefaultAboutPane extends BorderPane {

    private static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle.getBundle("lang.DefaultAboutPane");

    protected static final double LOGO_SIZE = 128;

    protected static final double PADDING = 5;

    protected final VBox main;

    protected List<Node> getAdditionalLogos() {
        return Collections.emptyList();
    }

    @Deprecated
    /**
     * @deprecated directly add additional components to main pane.
     */
    protected List<String> getAdditionalInfos() {
        return Collections.emptyList();
    }

    public DefaultAboutPane() {
        setBackground(new Background(new BackgroundFill(Color.WHITE, null, null)));
        setPrefWidth(600);

        PowsyblLogo powsyblLogo = new PowsyblLogo(LOGO_SIZE, LOGO_SIZE);

        HBox logoPane = new HBox(PADDING, powsyblLogo);
        logoPane.getChildren().addAll(getAdditionalLogos());
        logoPane.setPadding(new Insets(PADDING, PADDING, PADDING, PADDING));
        setTop(logoPane);

        main = new VBox(PADDING,
                        new Label(RESOURCE_BUNDLE.getString("JavaVersion") + ": " + SystemUtils.JAVA_VERSION),
                        new Label(RESOURCE_BUNDLE.getString("RepositoryVersions") + ": "),
                        createVersionTable());
        main.setPadding(new Insets(PADDING, PADDING, PADDING, PADDING));
        setCenter(main);
    }

    private static TableView<Version> createVersionTable() {
        TableView<Version> versionTable = new TableView<>();
        TableColumn<Version, String> repositoryNameCol = new TableColumn<>(RESOURCE_BUNDLE.getString("RepositoryName"));
        repositoryNameCol.setCellValueFactory(new PropertyValueFactory<>("repositoryName"));
        TableColumn<Version, String> mavenProjectVersionCol = new TableColumn<>(RESOURCE_BUNDLE.getString("MavenProjectVersion"));
        mavenProjectVersionCol.setCellValueFactory(new PropertyValueFactory<>("mavenProjectVersion"));
        TableColumn<Version, String> gitVersionCol = new TableColumn<>(RESOURCE_BUNDLE.getString("GitVersion"));
        gitVersionCol.setCellValueFactory(new PropertyValueFactory<>("gitVersion"));
        TableColumn<Version, String> gitBranchCol = new TableColumn<>(RESOURCE_BUNDLE.getString("GitBranch"));
        gitBranchCol.setCellValueFactory(new PropertyValueFactory<>("gitBranch"));
        TableColumn<Version, String> buildTimestampCol = new TableColumn<>(RESOURCE_BUNDLE.getString("BuildTimestamp"));
        buildTimestampCol.setCellValueFactory(param -> new SimpleStringProperty(new DateTime(param.getValue().getBuildTimestamp()).toString()));
        versionTable.getColumns().addAll(repositoryNameCol, mavenProjectVersionCol, gitVersionCol, gitBranchCol, buildTimestampCol);
        versionTable.getItems().addAll(Version.list());
        return versionTable;
    }
}
