/**
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.gse.util;

import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;
import javafx.scene.text.Text;
import org.controlsfx.control.textfield.CustomTextField;
import org.controlsfx.control.textfield.TextFields;

/**
 * @author Nassirou Nambiema <nassirou.nambiena at rte-france.com>
 */
public class ReplaceWordBar extends HBox {

    private final Button replaceButton;
    private final Button replaceAllButton;
    private final CustomTextField searchField = (CustomTextField) TextFields.createClearableTextField();


    public ReplaceWordBar() {
        super(0);
        Text searchGlyph = Glyph.createAwesomeFont('\uf002').size("1.4em");
        searchField.setLeft(searchGlyph);
        searchField.setPrefWidth(300);
        searchField.getStyleClass().add("search-field");
        replaceButton = new Button("Replace");
        replaceAllButton = new Button("Replace all");
        setMargin(searchField, new Insets(0, 0, 0, 5));
        getChildren().addAll(searchField, replaceButton, replaceAllButton);

    }

    public Button getReplaceButton() {
        return replaceButton;
    }

    public Button getReplaceAllButton() {
        return replaceAllButton;
    }

    public CustomTextField getSearchField() {
        return searchField;
    }
}
