package com.powsybl.gse.security;

import javafx.geometry.Side;
import javafx.scene.control.TableColumn;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.BorderPane;
import org.controlsfx.control.HiddenSidesPane;

import java.util.ResourceBundle;

abstract class AbstractContingencyResultPane extends BorderPane {

    static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle.getBundle("lang.SecurityAnalysis");

    static void onMouseClickedOnFilter(HiddenSidesPane hiddenSidesPane, ToggleButton filterButton) {
        if (hiddenSidesPane.getPinnedSide() != null) {
            hiddenSidesPane.setPinnedSide(null);
            filterButton.setStyle("-fx-background-color: transparent;");
        } else {
            hiddenSidesPane.setPinnedSide(Side.RIGHT);
            filterButton.setStyle("-fx-background-color: #A3A3A4");
        }
    }

    static <S, T> TableColumn<S, T> createColumn(String type) {
        TableColumn<S, T> column = new TableColumn<>(RESOURCE_BUNDLE.getString(type));
        column.setUserData(type);
        return column;
    }

}
