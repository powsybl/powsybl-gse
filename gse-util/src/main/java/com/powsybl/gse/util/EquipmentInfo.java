/**
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.gse.util;

import javafx.scene.input.DataFormat;

import java.io.Serializable;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class EquipmentInfo implements Serializable {

    public static final DataFormat DATA_FORMAT = new DataFormat("equipmentInfo");

    private IdAndName idAndName;

    private String type;

    public EquipmentInfo() {
    }

    public EquipmentInfo(IdAndName idAndName, String type) {
        this.idAndName = idAndName;
        this.type = type;
    }

    public IdAndName getIdAndName() {
        return idAndName;
    }

    public void setIdAndName(IdAndName idAndName) {
        this.idAndName = idAndName;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
