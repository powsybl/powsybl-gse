/**
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.gse.map;

import javafx.scene.paint.Color;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class LineGraphicTest {

    @Test
    public void test1() {
        LineGraphic line = new LineGraphic("l1", 0, Color.RED);
        line.getSegments().add(new SegmentGraphic(new Coordinate(1, 1), new Coordinate(1, 2), line));
        line.getSegments().add(new SegmentGraphic(new Coordinate(1, 2), new Coordinate(1, 3), line));
        line.updateBranches();
        List<BranchGraphic> branches = line.getBranches();
        assertEquals(1, branches.size());
        BranchGraphic branch = branches.get(0);
        assertEquals(Arrays.asList(new Coordinate(1, 1), new Coordinate(1, 2), new Coordinate(1, 3)),
                branch.getPylons().stream().map(PylonGraphic::getCoordinate).collect(Collectors.toList()));
    }

    @Test
    public void test2() {
        LineGraphic line = new LineGraphic("l1", 0, Color.RED);
        line.getSegments().add(new SegmentGraphic(new Coordinate(1, 1), new Coordinate(1, 2), line));
        line.getSegments().add(new SegmentGraphic(new Coordinate(1, 2), new Coordinate(1, 3), line));
        line.getSegments().add(new SegmentGraphic(new Coordinate(1, 4), new Coordinate(1, 5), line));
        line.updateBranches();
        List<BranchGraphic> branches = line.getBranches();
        assertEquals(2, branches.size());
        BranchGraphic branch1 = branches.get(0);
        BranchGraphic branch2 = branches.get(1);
        assertEquals(Arrays.asList(new Coordinate(1, 1), new Coordinate(1, 2), new Coordinate(1, 3)),
                branch1.getPylons().stream().map(PylonGraphic::getCoordinate).collect(Collectors.toList()));
        assertEquals(Arrays.asList(new Coordinate(1, 4), new Coordinate(1, 5)),
                branch2.getPylons().stream().map(PylonGraphic::getCoordinate).collect(Collectors.toList()));
    }

    @Test
    public void test3() {
        LineGraphic line = new LineGraphic("l1", 0, Color.RED);
        line.getSegments().add(new SegmentGraphic(new Coordinate(1, 1), new Coordinate(1, 2), line));
        line.getSegments().add(new SegmentGraphic(new Coordinate(1, 2), new Coordinate(1, 3), line));
        line.getSegments().add(new SegmentGraphic(new Coordinate(1, 2), new Coordinate(1, 4), line));
        line.getSegments().add(new SegmentGraphic(new Coordinate(1, 4), new Coordinate(1, 5), line));
        line.updateBranches();
        List<BranchGraphic> branches = line.getBranches();
        assertEquals(2, branches.size());
        BranchGraphic branch1 = branches.get(0);
        BranchGraphic branch2 = branches.get(1);
        assertEquals(Arrays.asList(new Coordinate(1, 2), new Coordinate(1, 4), new Coordinate(1, 5)),
                branch1.getPylons().stream().map(PylonGraphic::getCoordinate).collect(Collectors.toList()));
        assertEquals(Arrays.asList(new Coordinate(1, 1), new Coordinate(1, 2), new Coordinate(1, 3)),
                branch2.getPylons().stream().map(PylonGraphic::getCoordinate).collect(Collectors.toList()));
    }
}
