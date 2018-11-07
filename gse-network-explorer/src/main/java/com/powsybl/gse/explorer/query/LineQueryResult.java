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

    private String idVoltageLevel1;

    private String idVoltageLevel2;

    private String nameVoltageLevel1;

    private String nameVoltageLevel2;

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

    public String getIdVoltageLevel1() {
        return idVoltageLevel1;
    }

    public void setIdVoltageLevel1(String idVoltageLevel1) {
        this.idVoltageLevel1 = idVoltageLevel1;
    }

    public String getIdVoltageLevel2() {
        return idVoltageLevel2;
    }

    public void setIdVoltageLevel2(String idVoltageLevel2) {
        this.idVoltageLevel2 = idVoltageLevel2;
    }

    public String getNameVoltageLevel1() {
        return nameVoltageLevel1;
    }

    public void setNameVoltageLevel1(String nameVoltageLevel1) {
        this.nameVoltageLevel1 = nameVoltageLevel1;
    }

    public String getNameVoltageLevel2() {
        return nameVoltageLevel2;
    }

    public void setNameVoltageLevel2(String nameVoltageLevel2) {
        this.nameVoltageLevel2 = nameVoltageLevel2;
    }
}
