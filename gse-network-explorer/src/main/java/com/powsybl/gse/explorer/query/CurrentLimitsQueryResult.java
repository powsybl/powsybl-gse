/**
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.gse.explorer.query;

import java.util.List;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class CurrentLimitsQueryResult {

    private String side;

    private double permanentLimit = Double.NaN;

    private List<TemporaryLimitQueryResult> temporaryLimits;

    public String getSide() {
        return side;
    }

    public void setSide(String side) {
        this.side = side;
    }

    public double getPermanentLimit() {
        return permanentLimit;
    }

    public void setPermanentLimit(double permanentLimit) {
        this.permanentLimit = permanentLimit;
    }

    public List<TemporaryLimitQueryResult> getTemporaryLimits() {
        return temporaryLimits;
    }

    public void setTemporaryLimits(List<TemporaryLimitQueryResult> temporaryLimits) {
        this.temporaryLimits = temporaryLimits;
    }
}
