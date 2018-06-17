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
public final class SegmentGraphicIndex {

    private static final Logger LOGGER = LoggerFactory.getLogger(SegmentGraphicIndex.class);

    private final RTree<SegmentGraphic, Geometry> tree;

    private SegmentGraphicIndex(RTree<SegmentGraphic, Geometry> tree) {
        this.tree = Objects.requireNonNull(tree);
    }

    public static SegmentGraphicIndex build(Collection<SegmentGraphic> segments) {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        RTree<SegmentGraphic, Geometry> tree = RTree.star().maxChildren(6).create();

        for (SegmentGraphic segment : segments) {
            double lon1 = segment.getCoordinate1().getLon();
            double lat1 = segment.getCoordinate1().getLat();
            double lon2 = segment.getCoordinate2().getLon();
            double lat2 = segment.getCoordinate2().getLat();
            Rectangle rectangle = Geometries.rectangleGeographic(Math.min(lon1, lon2), Math.min(lat1, lat2),
                    Math.max(lon1, lon2), Math.max(lat1, lat2));
            tree = tree.add(segment, rectangle);
        }

        LOGGER.info("Line segments R-tree built in {} ms", stopWatch.getTime());

        return new SegmentGraphicIndex(tree);
    }

    public RTree<SegmentGraphic, Geometry> getTree() {
        return tree;
    }
}
