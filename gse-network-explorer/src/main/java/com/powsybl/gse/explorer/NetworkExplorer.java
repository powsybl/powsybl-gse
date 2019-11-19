/**
 * Copyright (c) 2017, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.gse.explorer;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.powsybl.afs.ext.base.ProjectCase;
import com.powsybl.afs.ext.base.ProjectCaseListener;
import com.powsybl.afs.ext.base.ScriptType;
import com.powsybl.commons.json.JsonUtil;
import com.powsybl.gse.explorer.diagrams.LinePiModelDiagram;
import com.powsybl.gse.explorer.query.LineQueryResult;
import com.powsybl.gse.explorer.query.VoltageLevelQueryResult;
import com.powsybl.gse.explorer.symbols.*;
import com.powsybl.gse.spi.GseContext;
import com.powsybl.gse.spi.GseException;
import com.powsybl.gse.spi.ProjectFileViewer;
import com.powsybl.gse.util.*;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.Version;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import org.controlsfx.control.textfield.CustomTextField;
import org.controlsfx.control.textfield.TextFields;

import java.io.IOException;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
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

    private static final String GENERATOR = "GENERATOR";
    private static final String LOAD = "LOAD";
    private static final String TWO_WINDINGS_TRANSFORMER = "TWO_WINDINGS_TRANSFORMER";
    private static final String CAPACITOR = "CAPACITOR";
    private static final String INDUCTOR = "INDUCTOR";
    private static final String LINE = "LINE";
    private static final String SWITCH = "SWITCH";

    private final LastTaskOnlyExecutor substationExecutor;
    private final LastTaskOnlyExecutor substationDetailsExecutor;
    private final LastTaskOnlyExecutor equipmentExecutor;

    private final ObservableList<IdAndName> substationIds = FXCollections.observableArrayList();
    private final FilteredList<IdAndName> filteredSubstationIds = substationIds.filtered(s -> true);
    private final ListView<IdAndName> substationsView = new ListView<>(filteredSubstationIds);
    private final TextField substationFilterInput = TextFields.createClearableTextField();
    private final TreeView<EquipmentInfo> substationDetailedView = new TreeView<>();
    private final TabPane equipmentTabs = new TabPane();
    private final SplitPane splitPane;
    private final CheckBox showName = new CheckBox(RESOURCE_BUNDLE.getString("ShowNames"));

    // json
    private final ObjectMapper mapper = JsonUtil.createObjectMapper();
    private final JavaType voltageLevelQueryResultListType;
    private final JavaType idAndNameListType;
    private final JavaType lineType;

    private final double rem;

    private final Configuration cfg;
    private final Template substationTemplate;
    private final Template voltageLevelTemplate;
    private final Template lineTemplate;

    private final LinePiModelDiagram linePiModelDiagram;

    private final Tab linePiModelTab;

    private ProjectCase projectCase;

    NetworkExplorer(ProjectCase projectCase, GseContext context) {
        this.projectCase = Objects.requireNonNull(projectCase);

        substationExecutor = new LastTaskOnlyExecutor(context.getExecutor());
        substationDetailsExecutor = new LastTaskOnlyExecutor(context.getExecutor());
        equipmentExecutor = new LastTaskOnlyExecutor(context.getExecutor());

        equipmentTabs.setStyle("-fx-background-color: white;");
        linePiModelDiagram = new LinePiModelDiagram(Color.BLACK, 2);
        linePiModelTab = new Tab(RESOURCE_BUNDLE.getString("LinePiModel"), linePiModelDiagram);

        splitPane = new SplitPane(substationsView, substationDetailedView, equipmentTabs);
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
            refreshEquipmentView();
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

        substationDetailedView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        substationDetailedView.getSelectionModel().getSelectedItems().addListener((ListChangeListener<TreeItem<EquipmentInfo>>) change -> refreshEquipmentView());

        substationDetailedView.setOnDragDetected(this::onDragDetected);

        voltageLevelQueryResultListType = mapper.getTypeFactory().constructCollectionType(List.class, VoltageLevelQueryResult.class);
        idAndNameListType = mapper.getTypeFactory().constructCollectionType(List.class, IdAndName.class);
        lineType = mapper.getTypeFactory().constructType(LineQueryResult.class);

        rem = Math.rint(new Text("").getLayoutBounds().getHeight());

        cfg = new Configuration(new Version(2, 3, 28));
        cfg.setClassForTemplateLoading(NetworkExplorer.class, "/ftl");
        cfg.setDefaultEncoding(StandardCharsets.UTF_8.name());
        try {
            substationTemplate = cfg.getTemplate("substationQuery.ftl");
            voltageLevelTemplate = cfg.getTemplate("voltageLevelQuery.ftl");
            lineTemplate = cfg.getTemplate("lineQuery.ftl");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        projectCase.addListener(this);
    }

    @Override
    public Node getContent() {
        return this;
    }

    private void onDragDetected(MouseEvent event) {
        Dragboard db = substationDetailedView.startDragAndDrop(TransferMode.ANY);
        ClipboardContent content = new ClipboardContent();
        List<EquipmentInfo> listInfo = substationDetailedView.getSelectionModel().getSelectedItems().stream().map(TreeItem :: getValue).collect(Collectors.toList());
        content.put(EquipmentInfo.DATA_FORMAT, listInfo);

        db.setContent(content);

        event.consume();
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

    private Node getSymbol(String type) {
        Node symbol = null;
        switch (type) {
            case GENERATOR:
                symbol = new GeneratorSymbol(ICON_COLOR, ICON_THICKNESS, rem);
                break;

            case LOAD:
                symbol = new LoadSymbol(ICON_COLOR, ICON_THICKNESS, rem);
                break;

            case TWO_WINDINGS_TRANSFORMER:
                symbol = new TransformerSymbol(ICON_COLOR, ICON_THICKNESS, rem);
                break;

            case CAPACITOR:
                symbol = new CapacitorSymbol(ICON_COLOR, ICON_THICKNESS, rem);
                break;

            case INDUCTOR:
                symbol = new InductorSymbol(ICON_COLOR, ICON_THICKNESS, rem);
                break;

            case LINE:
                symbol = new LineSymbol(ICON_COLOR, ICON_THICKNESS, rem);
                break;

            case SWITCH:
                symbol = new SwitchSymbol(ICON_COLOR, ICON_THICKNESS, rem);
                break;

            default:
                break;
        }
        return symbol;
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

    private static String processTemplate(Template template, Map<String, Object> templateData) {
        try (StringWriter out = new StringWriter()) {
            template.process(templateData, out);
            out.flush();
            return out.toString();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (TemplateException e) {
            throw new GseException(e);
        }
    }

    private void refreshSubstationsView() {
        substationIds.setAll(LIST_BUSY);
        String query = processTemplate(substationTemplate, Collections.emptyMap());
        queryNetwork(query, idAndNameListType, (List<IdAndName> ids) -> {
            if (ids == null) {
                substationIds.clear();
            } else {
                substationIds.setAll(ids.stream().sorted(getIdAndNameComparator()).collect(Collectors.toList()));
            }
        }, substationExecutor);
    }

    private void fillSubstationDetailViewWithQueryResults(IdAndName substationIdAndName, List<VoltageLevelQueryResult> voltageLevelQueryResults) {
        equipmentTabs.getTabs().clear();
        if (voltageLevelQueryResults != null) {
            TreeItem<EquipmentInfo> substationItem = new TreeItem<>(new EquipmentInfo(substationIdAndName, "SUBSTATION"));
            substationItem.setExpanded(true);
            for (VoltageLevelQueryResult voltageLevelQueryResult : voltageLevelQueryResults) {
                TreeItem<EquipmentInfo> voltageLevelItem = new TreeItem<>(new EquipmentInfo(voltageLevelQueryResult.getIdAndName(), "VOLTAGE_LEVEL"));
                substationItem.getChildren().add(voltageLevelItem);
                for (EquipmentInfo equipment : voltageLevelQueryResult.getEquipments()) {
                    Node icon = getSymbol(equipment.getType());
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
            String query = processTemplate(voltageLevelTemplate, ImmutableMap.of("substationId", substationIdAndName.getId().replace("'","\\'")));
            queryNetwork(query, voltageLevelQueryResultListType,
                (List<VoltageLevelQueryResult> voltageLevelQueryResults) -> fillSubstationDetailViewWithQueryResults(substationIdAndName, voltageLevelQueryResults),
                substationDetailsExecutor);
        } else {
            substationDetailedView.setRoot(null);
        }
    }

    private void refreshLineView(EquipmentInfo equipment) {
        equipmentTabs.getTabs().setAll(linePiModelTab);

        String lineQuery = processTemplate(lineTemplate, ImmutableMap.of("lineId", equipment.getIdAndName().getId().replace("'","\\'")));
        queryNetwork(lineQuery, lineType, (LineQueryResult result) -> {
            linePiModelDiagram.rProperty().set(result.getR());
            linePiModelDiagram.xProperty().set(result.getX());
            linePiModelDiagram.g1Property().set(result.getG1());
            linePiModelDiagram.g2Property().set(result.getG2());
            linePiModelDiagram.b1Property().set(result.getB1());
            linePiModelDiagram.b2Property().set(result.getB2());
            linePiModelDiagram.voltageLevel1Property().set(showName.isSelected() ? result.getNameVoltageLevel1() : result.getIdVoltageLevel1());
            linePiModelDiagram.voltageLevel2Property().set(showName.isSelected() ? result.getNameVoltageLevel2() : result.getIdVoltageLevel2());
        }, equipmentExecutor);
    }

    private void refreshEquipmentView() {
        if (substationDetailedView.getSelectionModel().getSelectedItems().size() == 1) {
            substationDetailedView.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
            EquipmentInfo equipment = substationDetailedView.getSelectionModel().getSelectedItem().getValue();
            switch (equipment.getType()) {
                case LINE:
                    refreshLineView(equipment);
                    substationDetailedView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
                    break;

                default:
                    equipmentTabs.getTabs().clear();
                    substationDetailedView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
                    break;
            }
        } else {
            equipmentTabs.getTabs().clear();
        }
    }

    @Override
    public void networkUpdated() {
        Platform.runLater(this::refreshSubstationsView);
    }

    @Override
    public void dispose() {
        projectCase.removeListener(this);
    }

    @Override
    public boolean isClosable() {
        return true;
    }

}
