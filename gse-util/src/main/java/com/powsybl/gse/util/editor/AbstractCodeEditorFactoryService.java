/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 */

package com.powsybl.gse.util.editor;

import java.util.Objects;

/**
 * @author Paul Bui-Quang <paul.buiquang at rte-france.com>
 */
public abstract class AbstractCodeEditorFactoryService<T extends AbstractCodeEditor> {

    private final Class<T> editorClass;

    public AbstractCodeEditorFactoryService(Class<T> editorClass) {
        this.editorClass = Objects.requireNonNull(editorClass);
    }

    /**
     * Create a new editor
     * @return the newly created editor
     */
    public T build() throws IllegalAccessException, InstantiationException {
        return editorClass.newInstance();
    }

    /**
     * Class of the editor
     * @return the code editor class
     */
    public Class<T> getEditorClass() {
        return editorClass;
    }
}
