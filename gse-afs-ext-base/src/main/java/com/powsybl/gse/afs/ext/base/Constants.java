/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 */

package com.powsybl.gse.afs.ext.base;

import com.google.common.collect.ImmutableList;

import java.util.List;

/**
 * @author Paul Bui-Quang <paul.buiquang at rte-france.com>
 */
public final class Constants {

    public static final List<String> STANDARD_SUGGESTIONS = ImmutableList.of("as", "assert", "boolean", "break", "breaker", "byte",
            "case", "catch", "char", "class", "continue", "def", "default", "double", "else", "enum",
            "extends", "false", "finally", "float", "for", "generator", "if", "implements", "import", "in",
            "instanceof", "int", "interface", "load", "long", "native", "network", "new", "null", "package", "private",
            "protected", "public", "return", "short", "static", "substation", "super", "switch", "synchronized", "this",
            "threadsafe", "throw", "throws", "transient", "true", "try", "void", "volatile", "voltageLevel", "while"
    );

    private Constants() throws IllegalAccessException {
        throw new IllegalAccessException("Can't instanciate an utility class");
    }

}
