package com.powsybl.gse.cop_past;

import com.google.auto.service.AutoService;
import com.powsybl.afs.ServiceCreationContext;
import com.powsybl.afs.ServiceExtension;

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
