/**
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.gse.map;

import javafx.scene.paint.Color;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class LineGraphic {

    private static final Logger LOGGER = LoggerFactory.getLogger(LineGraphic.class);

    private final String id;

    private final int drawOrder;

    private final Color color;

    private final List<SegmentGraphic> segments = new ArrayList<>();

    private final List<BranchGraphic> branches = new ArrayList<>();

    public LineGraphic(String id, int drawOrder, Color color) {
        this.id = Objects.requireNonNull(id);
        this.drawOrder = drawOrder;
        this.color = Objects.requireNonNull(color);
    }

    public String getId() {
        return id;
    }

    public int getDrawOrder() {
        return drawOrder;
    }

    public Color getColor() {
        return color;
    }

    public List<SegmentGraphic> getSegments() {
        return segments;
    }

    private static Stream<PylonGraphic> getNeighborsStream(PylonGraphic pylon, Set<PylonGraphic> remaining) {
        return pylon.getNeighbors()
                .stream()
                .map(PylonGraphic.Neighbor::getPylon)
                .filter(remaining::contains);
    }

    private void startBranch(Set<PylonGraphic> remaining) {
        if (remaining.isEmpty()) {
            return;
        }
        PylonGraphic pylon = remaining.stream()
                .filter(p -> getNeighborsStream(p, remaining).count() != 2) // to start branch at a leaf or cross
                .findFirst()
                .orElseGet(() -> remaining.iterator().next());
        List<PylonGraphic> pylons = new ArrayList<>();
        remaining.remove(pylon);
        traverse(pylon, pylons, remaining);
    }

    private void traverse(PylonGraphic pylon, List<PylonGraphic> pylons, Set<PylonGraphic> remaining) {
        LOGGER.debug("Traverse pylon {}", pylon.getCoordinate());
        pylons.add(pylon);
        getNeighborsStream(pylon, remaining).findFirst().ifPresent(next -> {
            if (remaining.remove(next)) {
                if (getNeighborsStream(next, remaining).count() == 1) {
                    traverse(next, pylons, remaining);
                } else {
                    LOGGER.debug("Restart");
                    if (!pylons.isEmpty()) {
                        branches.add(new BranchGraphic(pylons, this));
                    }
                    startBranch(remaining);
                }
            }
        });
    }

    public List<BranchGraphic> getBranches() {
        return branches;
    }

    public void updateSegmentGroups() {
        branches.clear();

        // index segment by position side 1 and 2
        Set<Coordinate> positions = new HashSet<>(segments.size());
        for (SegmentGraphic segment : segments) {
            positions.add(segment.getCoordinate1());
            positions.add(segment.getCoordinate2());
        }

        // create pylons
        Map<Coordinate, PylonGraphic> pylons = positions.stream().collect(Collectors.toMap(Function.identity(), PylonGraphic::new));

        // links pylons
        for (SegmentGraphic segment : segments) {
            PylonGraphic pylon1 = pylons.get(segment.getCoordinate1());
            PylonGraphic pylon2 = pylons.get(segment.getCoordinate2());
            pylon1.getNeighbors().add(new PylonGraphic.Neighbor(pylon2, segment));
            pylon2.getNeighbors().add(new PylonGraphic.Neighbor(pylon1, segment));
        }

        Set<PylonGraphic> remaining = new HashSet<>(pylons.values());
        while (!remaining.isEmpty()) {
            startBranch(remaining);
        }
    }
}
