/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the M
 * License, v. 2.0. If a copy of the MPL was not distribu
 * file, You can obtain one at http://mozilla.org/MPL/2.0
 */
package com.powsybl.gse.util;


/**
 * @author Nassirou Nambiema <nassirou.nambiena at rte-france.com>
 */
public class Shortcut {

    private String action;

    private String keycode;

    public Shortcut(String action, String keycode) {
        this.action = action;
        this.keycode = keycode;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getKeycode() {
        return keycode;
    }

    public void setKeycode(String keycode) {
        this.keycode = keycode;
    }

}
