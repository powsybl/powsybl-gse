/**
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.gse.spi;

import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class PowsyblLogo extends Pane {

    private static final Color HEAD_COLOR = Color.valueOf("#F8C960");
    private static final Color MEMBER_COLOR = Color.valueOf("#D38317");
    private static final Color EYE_COLOR = Color.valueOf("#F5D9B4");
    private static final Color NOSE_COLOR = Color.valueOf("#A66930");

    private final Circle head = new Circle();
    private final Circle leftEar = new Circle();
    private final Circle rightEar = new Circle();
    private final Circle leftHand = new Circle();
    private final Circle rightHand = new Circle();
    private final Circle rightEye = new Circle();
    private final Circle leftEye = new Circle();
    private final Circle noseHand = new Circle();

    public PowsyblLogo(double prefWidth, double prefHeight) {
        setPrefWidth(prefWidth);
        setPrefHeight(prefHeight);
        head.setFill(HEAD_COLOR);
        leftEar.setFill(MEMBER_COLOR);
        rightEar.setFill(MEMBER_COLOR);
        leftHand.setFill(MEMBER_COLOR);
        rightHand.setFill(MEMBER_COLOR);
        rightEye.setFill(EYE_COLOR);
        leftEye.setFill(EYE_COLOR);
        noseHand.setFill(NOSE_COLOR);

        getChildren().addAll(rightEar, head, leftEar, leftHand, rightHand, leftEye, rightEye, noseHand);
    }

    @Override
    protected void layoutChildren() {
        double size = Math.min(getWidth(), getHeight());

        double headX = size / 2;
        double headY = size / 2;
        double headRadius = (0.9 * size) / 2;
        head.setCenterX(headX);
        head.setCenterY(headY);
        head.setRadius(headRadius);

        double leftEarX = headX - headRadius * Math.cos(Math.toRadians(49));
        double leftEarY = headY - headRadius * Math.sin(Math.toRadians(49));
        double  leftEarRadius = 0.16 * size;
        leftEar.setCenterX(leftEarX);
        leftEar.setCenterY(leftEarY);
        leftEar.setRadius(leftEarRadius);

        double rightEarX = headX + headRadius * Math.cos(Math.toRadians(45));
        double rightEarY = headY - headRadius * Math.sin(Math.toRadians(45));
        double rightEarRadius = 0.13 * size;
        rightEar.setCenterX(rightEarX);
        rightEar.setCenterY(rightEarY);
        rightEar.setRadius(rightEarRadius);

        double handRadius = size / 10;
        double leftHandX = handRadius;
        double leftHandY = size - handRadius;
        leftHand.setCenterX(leftHandX);
        leftHand.setCenterY(leftHandY);
        leftHand.setRadius(handRadius);

        double rightHandX = size - handRadius;
        double rightHandY = size - handRadius;
        rightHand.setCenterX(rightHandX);
        rightHand.setCenterY(rightHandY);
        rightHand.setRadius(handRadius);

        double leftEyeRadius = size / 6;
        double leftEyeX = headX - size / 11;
        double leftEyeY = headY + size / 16;
        leftEye.setCenterX(leftEyeX);
        leftEye.setCenterY(leftEyeY);
        leftEye.setRadius(leftEyeRadius);

        double rightEyeRadius = size / 7.5;
        double rightEyeX = headX + size / 4;
        double rightEyeY = headY + size / 7;
        rightEye.setCenterX(rightEyeX);
        rightEye.setCenterY(rightEyeY);
        rightEye.setRadius(rightEyeRadius);

        double noseRadiusRadius = size / 12;
        double noseX = headX + size / 20;
        double noseY = headY + size / 3.2;
        noseHand.setCenterX(noseX);
        noseHand.setCenterY(noseY);
        noseHand.setRadius(noseRadiusRadius);
    }
}
