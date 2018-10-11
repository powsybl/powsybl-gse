/**
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.gse.explorer.symbols;

import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class ResistanceSymbol extends Pane {

    private final int peaks;

    private final Line l1;
    private final Line[] peakLine1;
    private final Line[] peakLine2;
    private final Line[] peakLine3;
    private final Line l2;

    public ResistanceSymbol(Color stroke, double strokeWidth, double size) {
        this(stroke, strokeWidth, size, 3);
    }

    public ResistanceSymbol(Color stroke, double strokeWidth, double size, int peaks) {
        this.peaks = peaks;

        setPrefSize(size, size);

        l1 = new Line();
        l1.setStroke(stroke);
        l1.setStrokeWidth(strokeWidth);
        l2 = new Line();
        l2.setStroke(stroke);
        l2.setStrokeWidth(strokeWidth);
        getChildren().addAll(l1, l2);
        peakLine1 = new Line[peaks];
        peakLine2 = new Line[peaks];
        peakLine3 = new Line[peaks];
        for (int i = 0; i < peaks; i++) {
            peakLine1[i] = new Line();
            peakLine1[i].setStroke(stroke);
            peakLine1[i].setStrokeWidth(strokeWidth);
            peakLine2[i] = new Line();
            peakLine2[i].setStroke(stroke);
            peakLine2[i].setStrokeWidth(strokeWidth);
            peakLine3[i] = new Line();
            peakLine3[i].setStroke(stroke);
            peakLine3[i].setStrokeWidth(strokeWidth);
            getChildren().addAll(peakLine1[i], peakLine2[i], peakLine3[i]);
        }
    }

    @Override
    protected void layoutChildren() {
        double size = Math.min(getWidth(), getHeight());
        double marginY = size / 10;

        l1.setStartX(size / 2);
        l1.setStartY(0);
        l1.setEndX(size / 2);
        l1.setEndY(marginY);

        double dy = (size - (2 * marginY)) / (peaks * 4);
        double dx = dy * 2;

        for (int i = 0; i < peaks; i++) {
            peakLine1[i].setStartX(size / 2);
            peakLine1[i].setStartY(marginY + i * dy * 4);
            peakLine1[i].setEndX(size / 2 + dx);
            peakLine1[i].setEndY(marginY + dy + i * dy * 4);

            peakLine2[i].setStartX(size / 2 + dx);
            peakLine2[i].setStartY(marginY + dy + i * dy * 4);
            peakLine2[i].setEndX(size / 2 - dx);
            peakLine2[i].setEndY(marginY + dy * 3 + i * dy * 4);

            peakLine3[i].setStartX(size / 2 - dx);
            peakLine3[i].setStartY(marginY + dy * 3 + i * dy * 4);
            peakLine3[i].setEndX(size / 2);
            peakLine3[i].setEndY(marginY + dy * 4 + i * dy * 4);
        }

        l2.setStartX(size / 2);
        l2.setStartY(size - marginY);
        l2.setEndX(size / 2);
        l2.setEndY(size);
    }
}
