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
import com.powsybl.gse.util.*;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import org.controlsfx.control.textfield.CustomTextField;
import org.controlsfx.control.textfield.TextFields;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
class NetworkExplorer extends BorderPane implements ProjectFileViewer, ProjectCaseListener {

    private static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle.getBundle("lang.NetworkExplorer");

    private static final IdAndName LIST_BUSY = new IdAndName("...", "...");
    private static final EquipmentInfo TREEVIEW_BUSY = new EquipmentInfo(new IdAndName("...", "..."), null);

    private static final Color ICON_COLOR = Color.BLACK;
    private static final double ICON_THICKNESS = 1;

    private static class VoltageLevelQueryResult {

        private IdAndName idAndName;

        private List<EquipmentInfo> equipments;

        public IdAndName getIdAndName() {
            return idAndName;
        }

        public void setIdAndName(IdAndName idAndName) {
            this.idAndName = idAndName;
        }

        public List<EquipmentInfo> getEquipments() {
            return equipments;
        }

        public void setEquipments(List<EquipmentInfo> equipments) {
            this.equipments = equipments;
        }
    }

    private final LastTaskOnlyExecutor substationExecutor;

    private final LastTaskOnlyExecutor substationDetailsExecutor;

    private final ObservableList<IdAndName> substationIds = FXCollections.observableArrayList();
    private final FilteredList<IdAndName> filteredSubstationIds = substationIds.filtered(s -> true);
    private final ListView<IdAndName> substationsView = new ListView<>(filteredSubstationIds);
    private final TextField substationFilterInput = TextFields.createClearableTextField();
    private final TreeView<EquipmentInfo> substationDetailedView = new TreeView<>();
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

        substationExecutor = new LastTaskOnlyExecutor(context.getExecutor());
        substationDetailsExecutor = new LastTaskOnlyExecutor(context.getExecutor());

        splitPane = new SplitPane(substationsView, substationDetailedView, equipmentView);
        splitPane.setDividerPositions(0.2, 0.6);

        FlowPane toolBar = new FlowPane(5, 0, substationFilterInput, showName);
        toolBar.setPadding(new Insets(5, 5, 5, 5));

        setCenter(splitPane);
        setTop(toolBar);

        Function<IdAndName, String> listViewToString = idAndName -> showName.isSelected() ? idAndName.getName()
                                                                                          : idAndName.getId();
        Function<EquipmentInfo, String> treeViewToString = equipmentInfo -> showName.isSelected() ? equipmentInfo.getIdAndName().getName()
                                                                                                  : equipmentInfo.getIdAndName().getId();
        GseUtil.setWaitingCellFactory(substationsView, LIST_BUSY, listViewToString);
        GseUtil.setWaitingCellFactory(substationDetailedView, TREEVIEW_BUSY, treeViewToString);

        showName.selectedProperty().addListener((observable, oldValue, newValue) -> {
            substationsView.refresh();
            substationDetailedView.refresh();
        });

        substationFilterInput.textProperty().addListener(obs -> {
            String filter = substationFilterInput.getText();
            if (filter == null || filter.length() == 0) {
                filteredSubstationIds.setPredicate(s -> true);
            } else {
                filteredSubstationIds.setPredicate(s -> s.getId().toLowerCase().contains(filter.toLowerCase()));
            }

            // select first
            if (substationsView.getItems().size() > 0) {
                substationsView.getSelectionModel().selectFirst();
            }
        });
        Text searchGlyph = Glyph.createAwesomeFont('\uf002').size("1.4em");
        ((CustomTextField) substationFilterInput).setLeft(searchGlyph);

        substationsView.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> refreshSubstationDetailView(newValue));

        substationDetailedView.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<TreeItem<EquipmentInfo>>() {
            @Override
            public void changed(ObservableValue<? extends TreeItem<EquipmentInfo>> observable, TreeItem<EquipmentInfo> oldValue, TreeItem<EquipmentInfo> newValue) {
                refreshEquipmentView(newValue);
            }
        });
        substationDetailedView.setOnDragDetected(event -> {
            EquipmentInfo selectedEquipmentInfo = substationDetailedView.getSelectionModel().getSelectedItem().getValue();

            Dragboard db = substationDetailedView.startDragAndDrop(TransferMode.ANY);

            ClipboardContent content = new ClipboardContent();
            content.put(EquipmentInfo.DATA_FORMAT, selectedEquipmentInfo);
            db.setContent(content);

            event.consume();
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
                    T obj = mapper.readValue(json, valueType);
                    Platform.runLater(() -> updater.accept(obj));
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

            case "SWITCH":
                icon = new SwitchIcon(ICON_COLOR, ICON_THICKNESS, rem);
                break;

            default:
                break;
        }
        return icon;
    }

    private Comparator<IdAndName> getIdAndNameComparator() {
        boolean selected = showName.isSelected();
        return (o1, o2) -> {
            if (selected) {
                return Objects.compare(o1.getName(), o2.getName(), String::compareTo);
            } else {
                return o1.getId().compareTo(o2.getId());
            }
        };
    }

    private void refreshSubstationsView() {
        substationIds.setAll(LIST_BUSY);
        String query = "network.substations.collect { [id: it.id, name: it.name] }";
        queryNetwork(query, idAndNameListType, (List<IdAndName> ids) -> {
            if (ids == null) {
                substationIds.clear();
            } else {
                substationIds.setAll(ids.stream().sorted(getIdAndNameComparator()).collect(Collectors.toList()));
            }
        }, substationExecutor);
    }

    private void fillSubstationDetailViewWithQueryResults(IdAndName substationIdAndName, List<VoltageLevelQueryResult> voltageLevelQueryResults) {
        if (voltageLevelQueryResults != null) {
            TreeItem<EquipmentInfo> substationItem = new TreeItem<>(new EquipmentInfo(substationIdAndName, "SUBSTATION"));
            substationItem.setExpanded(true);
            for (VoltageLevelQueryResult voltageLevelQueryResult : voltageLevelQueryResults) {
                TreeItem<EquipmentInfo> voltageLevelItem = new TreeItem<>(new EquipmentInfo(voltageLevelQueryResult.getIdAndName(), "VOLTAGE_LEVEL"));
                substationItem.getChildren().add(voltageLevelItem);
                for (EquipmentInfo equipment : voltageLevelQueryResult.getEquipments()) {
                    Node icon = getIcon(equipment.getType());
                    TreeItem<EquipmentInfo> equipmentItem = new TreeItem<>(equipment, icon);
                    voltageLevelItem.getChildren().add(equipmentItem);
                }
                voltageLevelItem.setExpanded(true);
            }
            substationDetailedView.setRoot(substationItem);
        } else {
            substationDetailedView.setRoot(null);
        }
    }

    private void refreshSubstationDetailView(IdAndName substationIdAndName) {
        if (substationIdAndName != null && substationIdAndName != LIST_BUSY) {
            substationDetailedView.setRoot(new TreeItem<>(TREEVIEW_BUSY));
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
                    "                    +",
                    "                    it.switches.collect {",
                    "                        [",
                    "                            type: 'SWITCH',",
                    "                            idAndName: [",
                    "                                           id: it.id,",
                    "                                           name: it.name",
                    "                                       ]",
                    "                        ]",
                    "                    }",
                    "    ]",
                    "}",
                    "");
            queryNetwork(query, voltageLevelQueryResultListType,
                (List<VoltageLevelQueryResult> voltageLevelQueryResults) -> fillSubstationDetailViewWithQueryResults(substationIdAndName, voltageLevelQueryResults),
                substationDetailsExecutor);
        } else {
            substationDetailedView.setRoot(null);
        }
    }

    private void refreshEquipmentView(TreeItem<EquipmentInfo> item) {
        //TODO to be used to fill per equipment details
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
