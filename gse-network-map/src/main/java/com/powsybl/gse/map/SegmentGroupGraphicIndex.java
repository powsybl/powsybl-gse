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
import com.github.davidmoten.rtree.geometry.Rectangle;
import org.apache.commons.lang3.time.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Objects;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public final class SegmentGroupGraphicIndex {

    private static final Logger LOGGER = LoggerFactory.getLogger(SegmentGroupGraphicIndex.class);

    private final RTree<SegmentGroupGraphic, Geometry> tree;

    private SegmentGroupGraphicIndex(RTree<SegmentGroupGraphic, Geometry> tree) {
        this.tree = Objects.requireNonNull(tree);
    }

    public static SegmentGroupGraphicIndex build(Collection<SegmentGroupGraphic> segmentGroups) {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        RTree<SegmentGroupGraphic, Geometry> tree = RTree.star().maxChildren(6).create();

        for (SegmentGroupGraphic segmentGroup : segmentGroups) {
            tree = tree.add(segmentGroup, segmentGroup.getBoundingBox());
        }

        LOGGER.info("Line segment groups R-tree built in {} ms", stopWatch.getTime());

        return new SegmentGroupGraphicIndex(tree);
    }

    public RTree<SegmentGroupGraphic, Geometry> getTree() {
        return tree;
    }
}
