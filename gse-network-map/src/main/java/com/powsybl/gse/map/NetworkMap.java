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
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ToolBar;
import javafx.scene.input.ZoomEvent;
import javafx.scene.layout.BorderPane;

import java.util.Objects;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class NetworkMap extends BorderPane implements ProjectFileViewer {

    private final ProjectCase projectCase;

    private final GseContext context;

    private final ToolBar toolBar;

    private final Button zoomInButton;

    private final Button zoomOutButton;

    private final MapView view;

    public NetworkMap(ProjectCase projectCase, GseContext context) {
        this.projectCase = Objects.requireNonNull(projectCase);
        this.context = Objects.requireNonNull(context);

        view = new MapView();
        setCenter(view);

        zoomInButton = new Button("", Glyph.createAwesomeFont('\uf00e').size("1.2em"));
        zoomInButton.getStyleClass().add("gse-toolbar-button");
        zoomInButton.setOnAction(event -> fireZoomEvent(1.5));

        zoomOutButton = new Button("", Glyph.createAwesomeFont('\uf010').size("1.2em"));
        zoomOutButton.getStyleClass().add("gse-toolbar-button");
        zoomOutButton.setOnAction(event -> fireZoomEvent(0.5));

        toolBar = new ToolBar(zoomInButton, zoomOutButton);
        setTop(toolBar);
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
    }

    @Override
    public Node getContent() {
        return this;
    }

    @Override
    public void view() {
        view.setZoom(5);
        view.setCenter(50, 4);
    }

    @Override
    public void dispose() {
    }
}
