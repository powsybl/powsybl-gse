/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.gse.copypasyte.afs.remote;

import com.google.auto.service.AutoService;
import com.google.common.base.Supplier;
import com.powsybl.afs.ServiceCreationContext;
import com.powsybl.afs.ServiceExtension;
import com.powsybl.gse.copypaste.afs.CopyService;
import com.powsybl.afs.ws.client.utils.RemoteServiceConfig;

import java.util.Objects;
import java.util.Optional;


/**
 * @author Nassirou Nambiema <nassirou.nambiena at rte-france.com>
 */
@AutoService(ServiceExtension.class)
public class RemoteCopyServiceExtension implements ServiceExtension<CopyService> {

    private final Supplier<Optional<RemoteServiceConfig>> configSupplier;

    public RemoteCopyServiceExtension() {
        this(RemoteServiceConfig::load);
    }

    public RemoteCopyServiceExtension(Supplier<Optional<RemoteServiceConfig>> configSupplier) {
        this.configSupplier = Objects.requireNonNull(configSupplier);
    }

    @Override
    public ServiceKey<CopyService> getServiceKey() {
        return new ServiceKey<>(CopyService.class, true);
    }

    @Override
    public CopyService createService(ServiceCreationContext context) {
        return new RemoteCopyService(configSupplier, context.getToken());
    }
}
