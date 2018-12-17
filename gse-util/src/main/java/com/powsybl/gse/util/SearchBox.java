/**
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.gse.util;

import javafx.geometry.Side;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import org.controlsfx.control.textfield.CustomTextField;
import org.controlsfx.control.textfield.TextFields;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Nassirou Nambiema <nassirou.nambiena at rte-france.com>
 */
public class SearchBox extends VBox {

    private Label label = new Label("Enter a file name");

    private CustomTextField searchField = (CustomTextField) TextFields.createClearableTextField();

    private ContextMenu contextMenu;

    public SearchBox(ContextMenu contextMenu) {
        super(8);
        Text searchGlyph = Glyph.createAwesomeFont('\uf002').size("1.4em");
        setPrefSize(460, 30);
        searchField.setLeft(searchGlyph);
        searchField.setPrefWidth(450);
        getChildren().addAll(label, searchField);
        this.contextMenu = contextMenu;
        Set<MenuItem> menuItems = new HashSet<>(contextMenu.getItems());
        searchField.textProperty().addListener((observable, oldvalue, newvalue) -> {
            if (!newvalue.isEmpty()) {
                Set<MenuItem> collect = menuItems.stream()
                        .filter(menuItem -> menuItem.getText().contains(newvalue))
                        .collect(Collectors.toSet());
                contextMenu.getItems().clear();
                contextMenu.getItems().addAll(collect);
                contextMenu.show(searchField, Side.BOTTOM, 0, 0);
            } else {
                contextMenu.hide();
            }
        });
    }

}
