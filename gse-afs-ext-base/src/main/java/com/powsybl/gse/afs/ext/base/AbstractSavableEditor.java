/**
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.gse.afs.ext.base;

import com.powsybl.gse.spi.Savable;
import com.powsybl.gse.util.GseAlerts;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.layout.BorderPane;

import java.util.Optional;

/**
 * @author Miora Ralambotiana <miora.ralambotiana at rte-france.com>
 */
abstract class AbstractSavableEditor extends BorderPane implements Savable {

    boolean showSaveAlert(String name) {
        Optional<ButtonType> result = GseAlerts.showSaveAndQuitDialog(name);
        return result.map(type -> {
            if (type.getButtonData() == ButtonBar.ButtonData.YES) {
                save();
            }

            return type != ButtonType.CANCEL;
        }).orElse(false);
    }

}
