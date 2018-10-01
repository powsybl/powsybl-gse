/**
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.gse.explorer.diagrams;

import com.powsybl.gse.explorer.symbols.CapacitorSymbol;
import com.powsybl.gse.explorer.symbols.GroundSymbol;
import com.powsybl.gse.explorer.symbols.InductorSymbol;
import com.powsybl.gse.explorer.symbols.ResistanceSymbol;
import javafx.beans.binding.Bindings;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class LinePiModelDiagram extends Pane {

    private final DoubleProperty r = new SimpleDoubleProperty(Double.NaN);

    private final DoubleProperty x = new SimpleDoubleProperty(Double.NaN);

    private final DoubleProperty g1 = new SimpleDoubleProperty(Double.NaN);

    private final DoubleProperty b1 = new SimpleDoubleProperty(Double.NaN);

    private final DoubleProperty g2 = new SimpleDoubleProperty(Double.NaN);

    private final DoubleProperty b2 = new SimpleDoubleProperty(Double.NaN);

    private final StringProperty voltageLevel1 = new SimpleStringProperty();

    private final StringProperty voltageLevel2 = new SimpleStringProperty();

    public LinePiModelDiagram(Color stroke, double strokeWidth) {
        setMinSize(400, 300);

        // create symbols
        ResistanceSymbol rSymbol = new ResistanceSymbol(stroke, strokeWidth, 50);
        rSymbol.setTranslateX(130);
        rSymbol.setTranslateY(50);
        rSymbol.setRotate(90);

        InductorSymbol xSymbol = new InductorSymbol(stroke, strokeWidth, 50, 4);
        xSymbol.setTranslateX(210);
        xSymbol.setTranslateY(50);
        xSymbol.setRotate(-90);

        ResistanceSymbol g1Symbol = new ResistanceSymbol(stroke, strokeWidth, 50);
        g1Symbol.setTranslateX(60);
        g1Symbol.setTranslateY(100);

        CapacitorSymbol b1Symbol = new CapacitorSymbol(stroke, strokeWidth, 50);
        b1Symbol.setTranslateX(60);
        b1Symbol.setTranslateY(170);

        ResistanceSymbol g2Symbol = new ResistanceSymbol(stroke, strokeWidth, 50);
        g2Symbol.setTranslateX(280);
        g2Symbol.setTranslateY(100);

        CapacitorSymbol b2Symbol = new CapacitorSymbol(stroke, strokeWidth, 50);
        b2Symbol.setTranslateX(280);
        b2Symbol.setTranslateY(170);

        // grounds
        GroundSymbol ground1 = new GroundSymbol(stroke, strokeWidth, 20);
        ground1.setTranslateX(75);
        ground1.setTranslateY(230);

        GroundSymbol ground2 = new GroundSymbol(stroke, strokeWidth, 20);
        ground2.setTranslateX(295);
        ground2.setTranslateY(230);

        // pins
        Circle pin1 = new Circle(20, 75, 5, stroke);
        pin1.setStrokeWidth(strokeWidth);
        Text pinLabel1 = new Text(15, 65, "1");

        Circle pin2 = new Circle(370, 75, 5, stroke);
        pin2.setStrokeWidth(strokeWidth);
        Text pinLabel2 = new Text(365, 65, "2");

        // the wire everything

        // pin1 <-> r
        Line wire1 = new Line(20, 75, 130, 75);
        wire1.setStroke(stroke);
        wire1.setStrokeWidth(2);

        // r <-> x
        Line wire2 = new Line(180, 75, 210, 75);
        wire2.setStroke(stroke);
        wire2.setStrokeWidth(2);

        // x <-> pin2
        Line wire3 = new Line(260, 75, 370, 75);
        wire3.setStroke(stroke);
        wire3.setStrokeWidth(2);

        // ? <-> g1
        Line wire4 = new Line(85, 75, 85, 100);
        wire4.setStroke(stroke);
        wire4.setStrokeWidth(2);

        // g1 <-> b1
        Line wire5 = new Line(85, 150, 85, 175);
        wire5.setStroke(stroke);
        wire5.setStrokeWidth(2);

        // ? <-> g2
        Line wire6 = new Line(305, 75, 305, 100);
        wire6.setStroke(stroke);
        wire6.setStrokeWidth(2);

        // g2 <-> b2
        Line wire7 = new Line(305, 150, 305, 175);
        wire7.setStroke(stroke);
        wire7.setStrokeWidth(2);

        // b1 <-> ground1
        Line wire8 = new Line(85, 215, 85, 230);
        wire8.setStroke(stroke);
        wire8.setStrokeWidth(2);

        // b2 <-> ground
        Line wire9 = new Line(305, 215, 305, 230);
        wire9.setStroke(stroke);
        wire9.setStrokeWidth(2);

        // labels
        Text rText = new Text(140, 20, "");
        rText.textProperty().bind(Bindings.createStringBinding(() -> "r=\n" + formatOhm(this.r) + "\n\u2126", this.r));
        rText.setTextAlignment(TextAlignment.CENTER);

        Text xText = new Text(210, 20, "");
        xText.textProperty().bind(Bindings.createStringBinding(() -> "x=\n" + formatOhm(this.x) + "\n\u2126", this.x));
        xText.setTextAlignment(TextAlignment.CENTER);

        Text g1Text = new Text(110, 110, "");
        g1Text.textProperty().bind(Bindings.createStringBinding(() -> "g1=\n" + formatSiemens(this.g1) + "\nS", this.g1));
        g1Text.setTextAlignment(TextAlignment.CENTER);

        Text g2Text = new Text(330, 110, "");
        g2Text.textProperty().bind(Bindings.createStringBinding(() -> "g2=\n" + formatSiemens(this.g2) + "\nS", this.g2));
        g2Text.setTextAlignment(TextAlignment.CENTER);

        Text b1Text = new Text(110, 180, "");
        b1Text.textProperty().bind(Bindings.createStringBinding(() -> "b1=\n" + formatSiemens(this.b1) + "\nS", this.b1));
        b1Text.setTextAlignment(TextAlignment.CENTER);

        Text b2Text = new Text(330, 180, "");
        b2Text.textProperty().bind(Bindings.createStringBinding(() -> "b2=\n" + formatSiemens(this.b2) + "\nS", this.b2));
        b2Text.setTextAlignment(TextAlignment.CENTER);

        Text voltageLevel1Text = new Text(15, 40, "");
        voltageLevel1Text.textProperty().bind(voltageLevel1);

        Text voltageLevel2Text = new Text(365, 40, "");
        voltageLevel2Text.textProperty().bind(voltageLevel2);

        getChildren().addAll(rSymbol, xSymbol, g1Symbol, b1Symbol, g2Symbol, b2Symbol,
                ground1, ground2,
                pin1, pin2, pinLabel1, pinLabel2,
                wire1, wire2, wire3, wire4, wire5, wire6, wire7, wire8, wire9,
                rText, xText, g1Text, g2Text, b1Text, b2Text,
                voltageLevel1Text, voltageLevel2Text);
    }

    private static String formatOhm(DoubleProperty d) {
        return Double.isNaN(d.get()) ? "?" : String.format("%.3f", d.get());
    }

    private static String formatSiemens(DoubleProperty d) {
        return Double.isNaN(d.get()) ? "?" : String.format("%.3e", d.get());
    }

    public DoubleProperty rProperty() {
        return r;
    }

    public DoubleProperty xProperty() {
        return x;
    }

    public DoubleProperty g1Property() {
        return g1;
    }

    public DoubleProperty g2Property() {
        return g2;
    }

    public DoubleProperty b1Property() {
        return b1;
    }

    public DoubleProperty b2Property() {
        return b2;
    }

    public StringProperty voltageLevel1Property() {
        return voltageLevel1;
    }

    public StringProperty voltageLevel2Property() {
        return voltageLevel2;
    }
}
