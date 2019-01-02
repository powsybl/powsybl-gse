/**
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.gse.util;

import javafx.geometry.Side;
import javafx.scene.control.*;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import org.controlsfx.control.textfield.CustomTextField;
import org.controlsfx.control.textfield.TextFields;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Nassirou Nambiema <nassirou.nambiena at rte-france.com>
 */
public class SearchBox extends VBox {

    private static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle.getBundle("lang.SearchBox");

    private Label label = new Label(RESOURCE_BUNDLE.getString("EnterFileName"));

    private CustomTextField searchField = (CustomTextField) TextFields.createClearableTextField();

    private ContextMenu contextMenu;

    private final Dialog<String> dialog = new Dialog<>();

    public SearchBox(ContextMenu contextMenuArg) {
        super(8);
        Text searchGlyph = Glyph.createAwesomeFont('\uf002').size("1.4em");
        setPrefSize(460, 30);
        searchField.setLeft(searchGlyph);
        searchField.setPrefWidth(450);
        label.setFont(Font.font("verdana", FontWeight.BLACK, 13));
        getChildren().addAll(label, searchField);
        this.contextMenu = contextMenuArg;
        dialog.getDialogPane().setContent(this);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialog.getDialogPane().lookupButton(ButtonType.OK).setVisible(false);
        dialog.getDialogPane().lookupButton(ButtonType.CANCEL).setVisible(false);
        Set<MenuItem> menuItems = new HashSet<>(contextMenu.getItems());
        searchField.textProperty().addListener((observable, oldvalue, newvalue) -> {
            if (!newvalue.isEmpty()) {
                Set<MenuItem> collect = menuItems.stream()
                        .filter(menuItem -> menuItem.getText().contains(newvalue))
                        .limit(12)
                        .collect(Collectors.toSet());
                contextMenu.getItems().clear();
                contextMenu.getItems().addAll(collect);
                contextMenu.show(searchField, Side.BOTTOM, 0, 0);
                if (!contextMenu.getItems().isEmpty()) {
                    contextMenu.getSkin().getNode().lookup(".menu-item").requestFocus();
                }
            } else {
                contextMenu.hide();
            }
        });
    }

    public void showDialog() {
        dialog.show();
    }

    public void closeDialog() {
        dialog.close();
    }

}
