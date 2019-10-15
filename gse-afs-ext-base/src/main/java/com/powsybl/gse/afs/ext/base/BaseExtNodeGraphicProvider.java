/**
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.gse.afs.ext.base;

import com.google.auto.service.AutoService;
import com.powsybl.afs.ext.base.Case;
import com.powsybl.afs.ext.base.ImportedCase;
import com.powsybl.afs.ext.base.ModificationScript;
import com.powsybl.afs.ext.base.VirtualCase;
import com.powsybl.gse.spi.NodeGraphicProvider;
import com.powsybl.gse.util.Glyph;
import com.powsybl.iidm.import_.Importer;
import javafx.scene.Node;
import javafx.scene.text.Font;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
@AutoService(NodeGraphicProvider.class)
public class BaseExtNodeGraphicProvider implements NodeGraphicProvider {

    static {
        Font.loadFont(Glyph.class.getResourceAsStream("/fonts/powsybl-gse-font.ttf"), 12);
    }

    public static Glyph createIidmGlyph() {
        return new Glyph("powsybl-gse-font", '\ue901')
                .size("1.4em")
                .color("orangered");
    }

    public Node createCaseGlyph(Importer importer) {
        String format = importer.getFormat();
        if (format.equals("CIM1") || format.equals("CGMES")) {
            return new Glyph("powsybl-gse-font", '\ue900')
                    .size("1.4em")
                    .color("#4A4A89");
        } else if (format.equals("XIIDM")) {
            return createIidmGlyph();
        }  else if (format.equals("UCTE")) {
            return new Glyph("powsybl-gse-font", '\ue902')
                    .size("1.4em");
        }
        return null;
    }

    public static Node createModificationScriptGlyph() {
        return Glyph.createAwesomeFont('\uf0f6').size("1.2em");
    }

    public static Node createVirtualCaseGlyph() {
        return createIidmGlyph()
                .stack(Glyph.createAwesomeFont('\uf14b')
                        .color("limegreen")
                        .size("0.9em"));
    }

    public static Node createImcompleteVirtualCaseGlyph() {
        return createIidmGlyph()
                .stack(Glyph.createAwesomeFont('\uf14b')
                        .color("limegreen")
                        .size("0.9em")
                        .stack(incompleteIconGlyph()));
    }

    private static Glyph incompleteIconGlyph() {
        return Glyph.createAwesomeFont('\uf057')
                .color("red")
                .size("0.8em");
    }

    @Override
    public Node getGraphic(Object file) {
        if (file instanceof Case) {
            Case aCase = (Case) file;
            return createCaseGlyph(aCase.getImporter());
        } else if (file instanceof ImportedCase) {
            ImportedCase importedCase = (ImportedCase) file;
            return createCaseGlyph(importedCase.getImporter());
        } else if (file instanceof VirtualCase) {
            if (((VirtualCase) file).mandatoryDependenciesAreMissing()) {
                return createImcompleteVirtualCaseGlyph();
            }
            return createVirtualCaseGlyph();
        } else if (file instanceof ModificationScript) {
            return createModificationScriptGlyph();
        }
        return null;
    }
}
