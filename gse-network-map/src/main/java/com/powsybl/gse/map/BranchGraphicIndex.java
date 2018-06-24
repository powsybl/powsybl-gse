/**
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.gse.map;

import com.github.davidmoten.rtree.RTree;
import com.github.davidmoten.rtree.geometry.Geometry;
import org.apache.commons.lang3.time.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Objects;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public final class BranchGraphicIndex {

    private static final Logger LOGGER = LoggerFactory.getLogger(BranchGraphicIndex.class);

    private final RTree<BranchGraphic, Geometry> tree;

    private BranchGraphicIndex(RTree<BranchGraphic, Geometry> tree) {
        this.tree = Objects.requireNonNull(tree);
    }

    public static BranchGraphicIndex build(Collection<BranchGraphic> segmentGroups) {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        RTree<BranchGraphic, Geometry> tree = RTree.star().maxChildren(6).create();

        for (BranchGraphic segmentGroup : segmentGroups) {
            tree = tree.add(segmentGroup, segmentGroup.getBoundingBox());
        }

        LOGGER.info("Line branches R-tree built in {} ms", stopWatch.getTime());

        return new BranchGraphicIndex(tree);
    }

    public RTree<BranchGraphic, Geometry> getTree() {
        return tree;
    }
}
