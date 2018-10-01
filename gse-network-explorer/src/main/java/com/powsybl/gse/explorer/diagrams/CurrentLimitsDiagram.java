/**
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.gse.explorer.diagrams;

import com.google.common.collect.ImmutableList;
import com.powsybl.gse.explorer.query.CurrentLimitsQueryResult;
import com.powsybl.gse.explorer.query.TemporaryLimitQueryResult;
import javafx.beans.property.ListProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.StackedBarChart;
import javafx.scene.chart.XYChart;
import javafx.scene.layout.Pane;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class CurrentLimitsDiagram extends Pane {

    private final CategoryAxis xAxis;

    private final StackedBarChart<String, Number> chart;

    private final ListProperty<CurrentLimitsQueryResult> limits = new SimpleListProperty<>();

    public CurrentLimitsDiagram() {
        xAxis = new CategoryAxis();
        xAxis.setLabel("Sides");

        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Value");

        chart = new StackedBarChart<>(xAxis, yAxis);

        getChildren().add(chart);

        limits.addListener((ListChangeListener<CurrentLimitsQueryResult>) c -> {
            xAxis.getCategories().clear();

            XYChart.Series<String, Number> permanentDataSeries = new XYChart.Series<>();
            permanentDataSeries.setName("Permanent");
            Map<Integer, XYChart.Series<String, Number>> temporaryDataSeries = new TreeMap<>();

            ObservableList<? extends CurrentLimitsQueryResult> results = c.getList();
            for (CurrentLimitsQueryResult result : results) {
                xAxis.getCategories().add(result.getSide());
                permanentDataSeries.getData().add(new XYChart.Data<>(result.getSide(), result.getPermanentLimit()));
                for (TemporaryLimitQueryResult tlResult : result.getTemporaryLimits()) {
                    temporaryDataSeries.computeIfAbsent(tlResult.getAcceptableDuration(), acceptableDuration -> {
                            XYChart.Series<String, Number> ds = new XYChart.Series<>();
                            ds.setName("Temporary " + acceptableDuration);
                            return ds;
                        }).getData().add(new XYChart.Data<>(result.getSide(), tlResult.getValue()));
                }
            }

            chart.getData().setAll(ImmutableList.<XYChart.Series<String, Number>>builder().add(permanentDataSeries)
                                                                                          .addAll(temporaryDataSeries.values())
                                                                                          .build());
        });
    }

    public ListProperty<CurrentLimitsQueryResult> limitsProperty() {
        return limits;
    }
}
