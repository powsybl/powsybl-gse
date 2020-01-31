/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 */

package com.powsybl.gse.spi;

import javafx.scene.Node;

import java.util.Optional;

/**
 * Application extension allows to extend GseApp menu bar to open custom views
 *
 * @author Paul Bui-Quang <paul.buiquang at rte-france.com>
 */
public interface GseAppExtension<T extends Node> {

    /**
     * Label of the menu link
     *
     * @return
     */
    String getMenuText();

    /**
     * Content of the view or side effects triggered when the menu is accessed
     *
     * @param context gse context
     * @return an optional view
     */
    Optional<View<T>> view(GseContext context, View<T> previousNode);

    /**
     * Indicate if the menu is a global main menu link (left side of the app bar) or a submenu link
     *
     * @return
     */
    boolean isMain();

    /**
     * View definition
     *
     * @param <U>
     */
    interface View<U extends Node> {

        /**
         * Javafx node content
         *
         * @return
         */
        U getNode();

        /**
         * Title of the content
         *
         * @return
         */
        String getTitle();
    }
}
