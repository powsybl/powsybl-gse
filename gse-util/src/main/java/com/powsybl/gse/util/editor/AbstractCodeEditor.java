/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 */
package com.powsybl.gse.util.editor;

import javafx.beans.value.ObservableValue;
import org.controlsfx.control.MasterDetailPane;

import java.util.List;

/**
 * @author Paul Bui-Quang <paul.buiquang at rte-france.com>
 */
public abstract class AbstractCodeEditor extends MasterDetailPane {

    public abstract void setTabSize(int size);

    public abstract void setEditable(boolean isEditable);

    public abstract void setCode(String code);

    public abstract String getCode();

    public abstract ObservableValue<String> codeProperty();

    public abstract ObservableValue<Integer> caretPositionProperty();

    public abstract String currentPosition();

    public abstract void setCompletions(List<String> completions);
}
