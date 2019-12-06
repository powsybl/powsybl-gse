/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.gse.util;

import javafx.scene.Node;
import javafx.scene.control.MenuItem;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;

import java.util.ResourceBundle;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class GseMenuItem extends MenuItem {

    private static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle.getBundle("lang.GseMenuItem");

    private static final String ICON_SIZE = "1.1em";

    private int order = 9999;

    public GseMenuItem(String title, Node graphic, KeyCodeCombination shortcut) {
        super(title, graphic);
        setAccelerator(shortcut);
    }

    public GseMenuItem(String text) {
        super(text);
    }

    public GseMenuItem(String text, Node graphic) {
        super(text, graphic);
    }

    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    public GseMenuItem order(int order) {
        this.order = order;
        return this;
    }

    public static GseMenuItem createArchiveMenuItem() {
        return new GseMenuItem(RESOURCE_BUNDLE.getString("Archive"), createGraphic('\uf0ed'), null);
    }

    public static GseMenuItem createUnarchiveMenuItem() {
        return new GseMenuItem(RESOURCE_BUNDLE.getString("Unarchive"), createGraphic('\uf0ee'), null);
    }

    public static GseMenuItem createCopyMenuItem() {
        return new GseMenuItem(RESOURCE_BUNDLE.getString("Copy"), createGraphic('\uf0c5'), new KeyCodeCombination(KeyCode.C, KeyCombination.CONTROL_DOWN));
    }

    public static GseMenuItem createPasteMenuItem() {
        return new GseMenuItem(RESOURCE_BUNDLE.getString("Paste"), createGraphic('\uf0ea'), new KeyCodeCombination(KeyCode.V, KeyCombination.CONTROL_DOWN));
    }

    public static GseMenuItem createDeleteMenuItem() {
        return new GseMenuItem(RESOURCE_BUNDLE.getString("Delete"), createGraphic('\uf1f8'), new KeyCodeCombination(KeyCode.DELETE));

    }

    private static Glyph createGraphic(char c) {
        return Glyph.createAwesomeFont(c).size(ICON_SIZE);
    }
}
