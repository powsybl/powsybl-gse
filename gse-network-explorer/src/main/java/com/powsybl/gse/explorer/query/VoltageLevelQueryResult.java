/**
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.gse.explorer.query;

import com.powsybl.gse.util.EquipmentInfo;
import com.powsybl.gse.util.IdAndName;

import java.util.List;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class VoltageLevelQueryResult {

    private IdAndName idAndName;

    private List<EquipmentInfo> equipments;

    public IdAndName getIdAndName() {
        return idAndName;
    }

    public void setIdAndName(IdAndName idAndName) {
        this.idAndName = idAndName;
    }

    public List<EquipmentInfo> getEquipments() {
        return equipments;
    }

    public void setEquipments(List<EquipmentInfo> equipments) {
        this.equipments = equipments;
    }
}
