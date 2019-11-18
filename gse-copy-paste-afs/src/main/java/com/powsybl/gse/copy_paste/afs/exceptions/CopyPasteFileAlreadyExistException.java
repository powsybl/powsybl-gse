/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 */

package com.powsybl.gse.copy_paste.afs.exceptions;

/**
 * @author Paul Bui-Quang <paul.buiquang at rte-france.com>
 */
public class CopyPasteFileAlreadyExistException extends CopyPasteException {

    public static final String MESSAGE = "FileAlreadyExist";

    public CopyPasteFileAlreadyExistException() {
        super(MESSAGE);
    }
}
