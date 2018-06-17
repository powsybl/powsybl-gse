/**
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.gse.map;

import com.github.davidmoten.rtree.RTree;
import com.github.davidmoten.rtree.geometry.Geometries;
import com.github.davidmoten.rtree.geometry.Geometry;
import com.github.davidmoten.rtree.geometry.Point;
import org.apache.commons.lang3.time.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Objects;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public final class SubstationGraphicIndex {

    private static final Logger LOGGER = LoggerFactory.getLogger(SubstationGraphicIndex.class);

    private final RTree<SubstationGraphic, Geometry> tree;

    private SubstationGraphicIndex(RTree<SubstationGraphic, Geometry> tree) {
        this.tree = Objects.requireNonNull(tree);
    }

    public static SubstationGraphicIndex build(Collection<SubstationGraphic> substations) {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        RTree<SubstationGraphic, Geometry> tree = RTree.create();
        for (SubstationGraphic substation : substations) {
            Point point = Geometries.pointGeographic(substation.getPosition().getLon(),
                    substation.getPosition().getLat());
            tree = tree.add(substation, point);
        }

        LOGGER.info("Substation R-tree built in {} ms", stopWatch.getTime());

        return new SubstationGraphicIndex(tree);
    }

    public RTree<SubstationGraphic, Geometry> getTree() {
        return tree;
    }
}
