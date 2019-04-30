package com.powsybl.gse.cop_past;

//import com.google.common.base.Supplier;
import com.google.auto.service.AutoService;
import com.powsybl.afs.ServiceCreationContext;
import com.powsybl.afs.ServiceExtension;
//import com.powsybl.afs.ws.client.utils.RemoteServiceConfig;

//import java.util.Objects;
//import java.util.Optional;

@AutoService(ServiceExtension.class)
public class RemoteCopyServiceExtension implements ServiceExtension<CopyService> {

   /* private final Supplier<Optional<RemoteServiceConfig>> configSupplier;

    public RemoteCopyServiceExtension() {
        this(RemoteServiceConfig::load);
    }

    public RemoteCopyServiceExtension(Supplier<Optional<RemoteServiceConfig>> configSupplier) {
        this.configSupplier = Objects.requireNonNull(configSupplier);
    }*/

    @Override
    public ServiceKey<CopyService> getServiceKey() {
        return new ServiceKey<>(CopyService.class, true);
    }

    @Override
    public CopyService createService(ServiceCreationContext context) {
        return null;
    }
}
