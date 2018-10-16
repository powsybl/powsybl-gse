/**
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.gse.security;

import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.util.Callback;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class DecimalColumnFactory<X, T extends Number> implements Callback<TableColumn<X, T>, TableCell<X, T>> {

    private int precision;

    public DecimalColumnFactory(int precision) {
        this.precision = precision;
    }

    @Override
    public TableCell<X, T> call(TableColumn<X, T> param) {
        return new TableCell<X, T>() {

            @Override
            protected void updateItem(T item, boolean empty) {
                if (!empty && item != null) {
                    String format = "%." + precision + "f";
                    setText(String.format(format, item.doubleValue()));
                } else {
                    setText("");
                }
            }
        };
    }

    public int getPrecision() {
        return precision;
    }

    public void setPrecision(int precision) {
        this.precision = precision;
    }
}

