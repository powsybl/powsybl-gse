/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.gse.copy_paste.afs.exceptions;

/**
 * @author Nassirou Nambiema <nassirou.nambiena at rte-france.com>
 */
public class CopyNotFinishedException extends CopyPasteException {

    public static final String MESSAGE = "Nodes copying are still ongoing";

    public CopyNotFinishedException() {
        super(MESSAGE);
    }
}
