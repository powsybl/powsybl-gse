/**
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.gse.map.util;

import java.util.Objects;

/**
 *
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class Polyline {

    protected final double[] x;

    protected final double[] y;

    public Polyline(double[] x, double[] y) {
        this.x = Objects.requireNonNull(x);
        this.y = Objects.requireNonNull(y);
        if (x.length != y.length) {
            throw new IllegalArgumentException("x.length != y.length");
        }
    }

    public int getLength() {
        return x.length;
    }

    public double getX(int i) {
        return x[i];
    }

    public double getY(int i) {
        return y[i];
    }
}
