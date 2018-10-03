/**
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.gse.explorer.query;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class LineQueryResult {

    private double r;

    private double x;

    private double g1;

    private double g2;

    private double b1;

    private double b2;

    private String voltageLevel1;

    private String voltageLevel2;

    public double getR() {
        return r;
    }

    public void setR(double r) {
        this.r = r;
    }

    public double getX() {
        return x;
    }

    public void setX(double x) {
        this.x = x;
    }

    public double getG1() {
        return g1;
    }

    public void setG1(double g1) {
        this.g1 = g1;
    }

    public double getG2() {
        return g2;
    }

    public void setG2(double g2) {
        this.g2 = g2;
    }

    public double getB1() {
        return b1;
    }

    public void setB1(double b1) {
        this.b1 = b1;
    }

    public double getB2() {
        return b2;
    }

    public void setB2(double b2) {
        this.b2 = b2;
    }

    public String getVoltageLevel1() {
        return voltageLevel1;
    }

    public void setVoltageLevel1(String voltageLevel1) {
        this.voltageLevel1 = voltageLevel1;
    }

    public String getVoltageLevel2() {
        return voltageLevel2;
    }

    public void setVoltageLevel2(String voltageLevel2) {
        this.voltageLevel2 = voltageLevel2;
    }
}
