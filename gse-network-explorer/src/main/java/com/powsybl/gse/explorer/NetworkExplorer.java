/**
 * Copyright (c) 2017, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.gse.explorer;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.powsybl.afs.ext.base.ProjectCase;
import com.powsybl.afs.ext.base.ProjectCaseListener;
import com.powsybl.afs.ext.base.ScriptType;
import com.powsybl.commons.json.JsonUtil;
import com.powsybl.gse.explorer.icons.*;
import com.powsybl.gse.spi.GseContext;
import com.powsybl.gse.spi.ProjectFileViewer;
import com.powsybl.gse.util.GseUtil;
import com.powsybl.gse.util.LastTaskOnlyExecutor;
import com.powsybl.iidm.network.Identifiable;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
class NetworkExplorer extends BorderPane implements ProjectFileViewer, ProjectCaseListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(NetworkExplorer.class);
    private static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle.getBundle("lang.NetworkExplorer");

    private static final IdAndName BUSY = new IdAndName("...", "...");

    private static final Color ICON_COLOR = Color.BLACK;
    private static final double ICON_THICKNESS = 1;

    private static class IdAndName {

        private String id;
        private String name;

        public IdAndName() {
        }

        public IdAndName(String id, String name) {
            this.id = Objects.requireNonNull(id);
            this.name = Objects.requireNonNull(name);
        }

        public IdAndName(Identifiable identifiable) {
            this(identifiable.getId(), identifiable.getName());
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

        @Override
        public String toString() {
            return id;
        }
    }

    private static class EquipmentQueryResult {

        private IdAndName idAndName;

        private String type;

        public EquipmentQueryResult() {
        }

        public IdAndName getIdAndName() {
            return idAndName;
        }

        public void setIdAndName(IdAndName idAndName) {
            this.idAndName = idAndName;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }
    }

    private static class VoltageLevelQueryResult {

        private IdAndName idAndName;

        private List<EquipmentQueryResult> equipments;

        public VoltageLevelQueryResult() {
        }

        public IdAndName getIdAndName() {
            return idAndName;
        }

        public void setIdAndName(IdAndName idAndName) {
            this.idAndName = idAndName;
        }

        public List<EquipmentQueryResult> getEquipments() {
            return equipments;
        }

        public void setEquipments(List<EquipmentQueryResult> equipments) {
            this.equipments = equipments;
        }
    }

    private final GseContext context;

    private final LastTaskOnlyExecutor substationExecutor;

    private final LastTaskOnlyExecutor substationDetailsExecutor;

    private final ListView<IdAndName> substationsView = new ListView<>();
    private final TreeView<IdAndName> substationDetailedView = new TreeView<>();
    private final FlowPane equipmentView = new FlowPane();
    private final SplitPane splitPane;
    private final CheckBox showName = new CheckBox(RESOURCE_BUNDLE.getString("ShowNames"));

    // json
    private final ObjectMapper mapper = JsonUtil.createObjectMapper();
    private final JavaType voltageLevelQueryResultListType;
    private final JavaType idAndNameListType;

    private final double rem;

    private ProjectCase projectCase;

    NetworkExplorer(ProjectCase projectCase, GseContext context) {
        this.projectCase = Objects.requireNonNull(projectCase);
        this.context = Objects.requireNonNull(context);

        substationExecutor = new LastTaskOnlyExecutor(context.getExecutor());
        substationDetailsExecutor = new LastTaskOnlyExecutor(context.getExecutor());

        splitPane = new SplitPane(substationsView, substationDetailedView, equipmentView);
        splitPane.setDividerPositions(0.2, 0.6);

        FlowPane toolBar = new FlowPane(showName);
        toolBar.setPadding(new Insets(5, 5, 5, 5));

        setCenter(splitPane);
        setTop(toolBar);

        Function<IdAndName, String> toString = idAndName -> showName.isSelected() ? idAndName.getName() : idAndName.getId();
        GseUtil.setWaitingCellFactory(substationsView, BUSY, toString);
        GseUtil.setWaitingCellFactory(substationDetailedView, BUSY, toString);

        showName.selectedProperty().addListener((observable, oldValue, newValue) -> {
            substationsView.refresh();
            substationDetailedView.refresh();
        });

        substationsView.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            refreshSubstationDetailView(newValue);
        });

        substationDetailedView.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<TreeItem<IdAndName>>() {
            @Override
            public void changed(ObservableValue<? extends TreeItem<IdAndName>> observable, TreeItem<IdAndName> oldValue, TreeItem<IdAndName> newValue) {
                refreshEquipmentView(newValue);
            }
        });

        voltageLevelQueryResultListType = mapper.getTypeFactory().constructCollectionType(List.class, VoltageLevelQueryResult.class);
        idAndNameListType = mapper.getTypeFactory().constructCollectionType(List.class, IdAndName.class);

        rem = Math.rint(new Text("").getLayoutBounds().getHeight());

        projectCase.addListener(this);
    }

    @Override
    public Node getContent() {
        return this;
    }

    private <T> void queryNetwork(String groovyScript, JavaType valueType, Consumer<T> updater, LastTaskOnlyExecutor lastTaskOnlyExecutor) {
        lastTaskOnlyExecutor.execute(() -> {
            try {
                String json = projectCase.queryNetwork(ScriptType.GROOVY, groovyScript);
                if (json != null) {
                    try {
                        T obj = mapper.readValue(json, valueType);
                        Platform.runLater(() -> updater.accept(obj));
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                }
            } catch (Exception e) {
                Platform.runLater(() -> {
                    updater.accept(null);
                    GseUtil.showDialogError(e);
                });
            }
        });
    }

    @Override
    public void view() {
        refreshSubstationsView();
    }

    private Node getIcon(String type) {
        Node icon = null;
        switch (type) {
            case "GENERATOR":
                icon = new GeneratorIcon(ICON_COLOR, ICON_THICKNESS, rem);
                break;

            case "LOAD":
                icon = new LoadIcon(ICON_COLOR, ICON_THICKNESS, rem);
                break;

            case "TWO_WINDINGS_TRANSFORMER":
                icon = new TransformerIcon(ICON_COLOR, ICON_THICKNESS, rem);
                break;

            case "CAPACITOR":
                icon = new CapacitorIcon(ICON_COLOR, ICON_THICKNESS, rem);
                break;

            case "INDUCTOR":
                icon = new InductorIcon(ICON_COLOR, ICON_THICKNESS, rem);
                break;

            case "LINE":
                icon = new LineIcon(ICON_COLOR, ICON_THICKNESS, rem);
                break;

            default:
                break;
        }
        return icon;
    }

    private void refreshSubstationsView() {
        substationsView.getItems().setAll(BUSY);
        String query = "network.substations.collect { [id: it.id, name: it.name] }";
        queryNetwork(query, idAndNameListType, (List<IdAndName> substationIds) -> {
            if (substationIds == null) {
                substationsView.getItems().clear();
            } else {
                substationsView.getItems().setAll(substationIds);
                if (substationsView.getItems().size() > 0) {
                    substationsView.getSelectionModel().selectFirst();
                }
            }
        }, substationExecutor);
    }

    private void refreshSubstationDetailView(IdAndName substationIdAndName) {
        if (substationIdAndName != null && substationIdAndName != BUSY) {
            substationDetailedView.setRoot(new TreeItem<>(BUSY));
            String query = String.join(System.lineSeparator(),
                    "def s = network.getSubstation('" + substationIdAndName.getId() + "')",
                    "s.voltageLevels.collect {",
                    "    [",
                    "        idAndName: [",
                    "                       id: it.id,",
                    "                       name: it.name",
                    "                    ],",
                    "        equipments: it.connectables.collect {",
                    "                        [",
                    "                            type: it.type.name() == 'SHUNT_COMPENSATOR' ? (it.getbPerSection() > 0 ? 'CAPACITOR' : 'INDUCTOR'): it.type,",
                    "                            idAndName: [",
                    "                                           id: it.id,",
                    "                                           name: it.name",
                    "                                       ]",
                    "                        ]",
                    "                    }",
                    "    ]",
                    "}",
                    "");
            queryNetwork(query, voltageLevelQueryResultListType, (List<VoltageLevelQueryResult> voltageLevelQueryResults) -> {
                if (voltageLevelQueryResults != null) {
                    TreeItem<IdAndName> substationItem = new TreeItem<>(substationIdAndName);
                    substationItem.setExpanded(true);
                    for (VoltageLevelQueryResult voltageLevelQueryResult : voltageLevelQueryResults) {
                        TreeItem<IdAndName> voltageLevelItem = new TreeItem<>(voltageLevelQueryResult.getIdAndName());
                        substationItem.getChildren().add(voltageLevelItem);
                        for (EquipmentQueryResult equipmentQueryResult : voltageLevelQueryResult.getEquipments()) {
                            Node icon = getIcon(equipmentQueryResult.getType());
                            TreeItem<IdAndName> equipmentItem = new TreeItem<>(equipmentQueryResult.getIdAndName(), icon);
                            voltageLevelItem.getChildren().add(equipmentItem);
                        }
                        voltageLevelItem.setExpanded(true);
                    }
                    substationDetailedView.setRoot(substationItem);
                } else {
                    substationDetailedView.setRoot(null);
                }
            }, substationDetailsExecutor);
        } else {
            substationDetailedView.setRoot(null);
        }
    }

    private void refreshEquipmentView(TreeItem<IdAndName> equipmentIdAndName) {
    }

    @Override
    public void networkUpdated() {
        Platform.runLater(this::refreshSubstationsView);
    }

    @Override
    public void dispose() {
        projectCase.removeListener(this);
    }

}
