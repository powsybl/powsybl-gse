/**
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.gse.map;

import com.gluonhq.maps.MapView;
import com.powsybl.afs.ext.base.ProjectCase;
import com.powsybl.gse.spi.GseContext;
import com.powsybl.gse.spi.ProjectFileViewer;
import com.powsybl.gse.util.Glyph;
import com.powsybl.gse.util.GseUtil;
import com.powsybl.iidm.network.Line;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.Substation;
import javafx.application.Platform;
import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.ToolBar;
import javafx.scene.input.ZoomEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class NetworkMap extends StackPane implements ProjectFileViewer {

    private static final Logger LOGGER = LoggerFactory.getLogger(NetworkMap.class);

    private static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle.getBundle("lang.NetworkMap");

    /**
     * Hack to fix layer refreshing issue
     */
    private class MapView2 extends MapView {
        @Override
        public void markDirty() {
            super.markDirty();
            taskQueue.reset();
        }
    }

    private final ProjectCase projectCase;

    private final GseContext context;

    private final ToolBar toolBar;

    private final Button zoomInButton;

    private final Button zoomOutButton;

    private final CheckBox showPylons = new CheckBox(RESOURCE_BUNDLE.getString("ShowPylons"));

    private final MapView2 view;

    private final BorderPane mainPane;

    private final ProgressIndicator progressIndicator = new ProgressIndicator();

    private final CancellableGraphicTaskQueue taskQueue;

    private final NetworkMapConfig config = new NetworkMapConfig();

    public NetworkMap(ProjectCase projectCase, GseContext context) {
        this.projectCase = Objects.requireNonNull(projectCase);
        this.context = Objects.requireNonNull(context);
        taskQueue = new CancellableGraphicTaskQueue(context.getExecutor());

        view = new MapView2();
        mainPane = new BorderPane();
        getChildren().addAll(mainPane, new Group(progressIndicator));
        mainPane.setCenter(view);

        zoomInButton = new Button("", Glyph.createAwesomeFont('\uf00e').size("1.2em"));
        zoomInButton.getStyleClass().add("gse-toolbar-button");
        zoomInButton.setOnAction(event -> fireZoomEvent(2));

        zoomOutButton = new Button("", Glyph.createAwesomeFont('\uf010').size("1.2em"));
        zoomOutButton.getStyleClass().add("gse-toolbar-button");
        zoomOutButton.setOnAction(event -> fireZoomEvent(0));

        toolBar = new ToolBar(zoomInButton, zoomOutButton, showPylons);
        mainPane.setTop(toolBar);

        showPylons.selectedProperty().bindBidirectional(config.isShowPylons());
        showPylons.selectedProperty().addListener((observable, oldValue, newValue) -> view.markDirty());
    }

    private void fireZoomEvent(double zoom) {
        double width = view.getWidth();
        double height = view.getHeight();
        double localX = width / 2;
        double localY = height / 2;
        Point2D sceneCoord = view.localToScene(localX, localY);
        Point2D screenCoord = view.localToScreen(localX, localY);
        ZoomEvent evt = new ZoomEvent(
                ZoomEvent.ZOOM,
                sceneCoord.getX(), sceneCoord.getY(),
                screenCoord.getX(), screenCoord.getY(),
                false, false, false, false, false, false, zoom, zoom, null);
        ZoomEvent.fireEvent(view, evt);
        view.markDirty();
    }

    @Override
    public Node getContent() {
        return this;
    }

    private void mapModelToGraphic(Map<String, SubstationGraphic> substations, Map<String, LineGraphic> lines) {
        int mappedSubstations = 0;
        Network network = projectCase.getNetwork();
        for (Substation substation : network.getSubstations()) {
            SubstationGraphic graphic = substations.get(substation.getId());
            if (graphic == null && !substation.getId().equals(substation.getName())) {
                graphic = substations.get(substation.getName());
            }
            if (graphic != null) {
                graphic.setModel(substation);
                mappedSubstations++;
            }
        }
        LOGGER.info("{}/{} substations mapped to graphic object", mappedSubstations, network.getSubstationCount());

        int mappedLines = 0;
        for (Line line : network.getLines()) {
            LineGraphic graphic = lines.get(line.getId());
            if (graphic == null && !line.getId().equals(line.getName())) {
                graphic = lines.get(line.getName());
            }
            if (graphic != null) {
                graphic.setModel(line);
                mappedLines++;
            }
        }
        LOGGER.info("{}/{} lines mapped to graphic object", mappedLines, network.getLineCount());
    }

    @Override
    public void view() {
        view.setZoom(6);
        view.setCenter(47, 3);
        progressIndicator.setVisible(true);
        mainPane.setDisable(true);
        GseUtil.execute(context.getExecutor(), () -> {
            // load french data from CSV
            Map<String, SubstationGraphic> substations = RteOpenData.parseSubstations();
            Map<String, LineGraphic> lines = RteOpenData.parseLines();
            for (LineGraphic line : lines.values()) {
                line.updateBranches();
            }
            Collection<BranchGraphic> branches = lines.entrySet().stream()
                    .map(Map.Entry::getValue)
                    .flatMap(line -> line.getBranches().stream())
                    .collect(Collectors.toList());

            // one layer per base voltage, so split line segments per base voltage
            Map<Integer, List<BranchGraphic>> orderedBranches = new TreeMap<>();
            for (BranchGraphic branch : branches) {
                orderedBranches.computeIfAbsent(branch.getLine().getDrawOrder(), k -> new ArrayList<>())
                            .add(branch);
            }

            // map model to graphic
            mapModelToGraphic(substations, lines);

            // build indexes
            SubstationGraphicIndex substationIndex = SubstationGraphicIndex.build(substations.values());
            SortedMap<Integer, BranchGraphicIndex> branchesIndexes = orderedBranches.entrySet()
                    .stream()
                    .collect(Collectors.toMap(Map.Entry::getKey,
                        e -> BranchGraphicIndex.build(e.getValue()),
                        (v1, v2) -> {
                            throw new AssertionError();
                        },
                        TreeMap::new));

            Platform.runLater(() -> {
                view.addLayer(new SubstationLayer(view, substationIndex));
                view.addLayer(new LineLayer(view, branchesIndexes, taskQueue, config));
                view.markDirty();
                progressIndicator.setVisible(false);
                mainPane.setDisable(false);
            });
        });
    }

    @Override
    public void dispose() {
        // nothing to dispose
    }

    @Override
    public boolean isClosable() {
        return true;
    }
}
