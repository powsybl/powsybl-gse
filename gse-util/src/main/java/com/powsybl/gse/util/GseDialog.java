/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.gse.util;

import javafx.beans.value.ObservableValue;
import javafx.scene.Node;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.stage.Window;
import javafx.util.Callback;

/**
 * @author Nassirou Nambiema <nassirou.nambiena at rte-france.com>
 */
public class GseDialog<F> extends Dialog<F> {

    public GseDialog(String title, Node content, Window window, ObservableValue<? extends Boolean> observable, Callback<ButtonType, F> value) {
        setTitle(title);
        getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        getDialogPane().lookupButton(ButtonType.OK).disableProperty().bind(observable);
        getDialogPane().setContent(content);
        setResizable(true);
        initOwner(window);
        setResultConverter(value);
    }
}
