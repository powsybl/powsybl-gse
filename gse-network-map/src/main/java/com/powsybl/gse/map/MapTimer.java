/**
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.gse.map;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.util.Duration;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class MapTimer {

    private final DoubleProperty slowProgress = new SimpleDoubleProperty();
    private final DoubleProperty mediumProgress = new SimpleDoubleProperty();
    private final DoubleProperty fastProgress = new SimpleDoubleProperty();

    private final Timeline slowTimeline;
    private final Timeline mediumTimeline;
    private final Timeline fastTimeline;

    public MapTimer() {
        slowTimeline = createTimeline(slowProgress, 0.5);
        mediumTimeline = createTimeline(mediumProgress, 1.5);
        fastTimeline = createTimeline(fastProgress, 3);
    }

    private static Timeline createTimeline(DoubleProperty progress, double speedFactor) {
        Timeline timeline = new Timeline(
                new KeyFrame(Duration.seconds(0d), new KeyValue(progress, 0d)),
                new KeyFrame(Duration.seconds(1d / speedFactor), new KeyValue(progress, 1d))
        );
        timeline.setCycleCount(Timeline.INDEFINITE);
        return timeline;
    }

    public void start() {
        slowTimeline.play();
        mediumTimeline.play();
        fastTimeline.play();
    }

    public void stop() {
        slowTimeline.stop();
        mediumTimeline.stop();
        fastTimeline.stop();
    }

    public DoubleProperty getSlowProgress() {
        return slowProgress;
    }

    public DoubleProperty getMediumProgress() {
        return mediumProgress;
    }

    public DoubleProperty getFastProgress() {
        return fastProgress;
    }
}
