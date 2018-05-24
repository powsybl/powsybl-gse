/**
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.gse.spi;

import com.powsybl.afs.ws.client.utils.UserSession;
import com.powsybl.commons.util.ServiceLoaderCache;

import java.util.Optional;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public interface GseAuthenticator {

    static Optional<GseAuthenticator> find() {
        return new ServiceLoaderCache<>(GseAuthenticator.class).getServices().stream().findFirst();
    }

    UserSession signIn(String login, String password);
}
