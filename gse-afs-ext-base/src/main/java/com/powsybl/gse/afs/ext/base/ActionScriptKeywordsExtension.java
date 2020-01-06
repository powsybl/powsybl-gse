/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.gse.afs.ext.base;

import com.google.auto.service.AutoService;
import com.google.common.collect.ImmutableSet;
import com.powsybl.afs.action.dsl.ActionScript;
import com.powsybl.gse.spi.AutoCompletionWordsProvider;
import javafx.util.Pair;

import java.util.*;

/**
 * @author Nassirou Nambiema <nassirou.nambiena at rte-france.com>
 */
@AutoService(AutoCompletionWordsProvider.class)
public class ActionScriptKeywordsExtension implements AutoCompletionWordsProvider {

    @Override
    public Class getProjectFileType() {
        return ActionScript.class;
    }

    @Override
    public Set<String> completionKeyWords() {
        Set<String> keyWords = new HashSet<>();
        keyWords.addAll(Constants.STANDARD_SUGGESTIONS);
        keyWords.addAll(ImmutableSet.of("action", "apply", "contingency", "description", "equipments", "life", "rule", "tasks", "test", "when"));
        return keyWords;
    }

    @Override
    public List<Pair<String, String>> completionMethods() {
        return Collections.emptyList();
    }
}
