/**
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.gse.map;

import com.gluonhq.charm.down.ServiceFactory;
import com.gluonhq.charm.down.Services;
import com.gluonhq.charm.down.plugins.StorageService;
import com.google.auto.service.AutoService;
import com.powsybl.gse.spi.FontLoader;

import java.io.File;
import java.util.Optional;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
@AutoService(FontLoader.class)
public class GluonHqMapsInitializer implements FontLoader {

    @Override
    public void load() {
        // define service for desktop
        StorageService storageService = new StorageService() {
            @Override
            public Optional<File> getPrivateStorage() {
                // user home app config location (linux: /home/[yourname]/.gluonmaps)
                return Optional.of(new File(System.getProperty("user.home")));
            }

            @Override
            public Optional<File> getPublicStorage(String subdirectory) {
                // this should work on desktop systems because home path is public
                return getPrivateStorage();
            }

            @Override
            public boolean isExternalStorageWritable() {
                //noinspection ConstantConditions
                return getPrivateStorage().orElseThrow(AssertionError::new).canWrite();
            }

            @Override
            public boolean isExternalStorageReadable() {
                //noinspection ConstantConditions
                return getPrivateStorage().orElseThrow(AssertionError::new).canRead();
            }
        };

        // define service factory for desktop
        ServiceFactory<StorageService> storageServiceFactory = new ServiceFactory<StorageService>() {

            @Override
            public Class<StorageService> getServiceType() {
                return StorageService.class;
            }

            @Override
            public Optional<StorageService> getInstance() {
                return Optional.of(storageService);
            }

        };

        // register service
        Services.registerServiceFactory(storageServiceFactory);
    }
}
