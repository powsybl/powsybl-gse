/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.gse.copypaste.afs.local;

import com.google.auto.service.AutoService;
import com.powsybl.afs.ServiceCreationContext;
import com.powsybl.afs.ServiceExtension;
import com.powsybl.gse.copypaste.afs.CopyService;

/**
 * @author Nassirou Nambiema <nassirou.nambiena at rte-france.com>
 */
@AutoService(ServiceExtension.class)
public class LocalCopyServiceExtension implements ServiceExtension<CopyService> {

    @Override
    public ServiceKey<CopyService> getServiceKey() {
        return new ServiceKey<>(CopyService.class, false);
    }

    @Override
    public CopyService createService(ServiceCreationContext context) {
        return new LocalCopyService();
    }
}
