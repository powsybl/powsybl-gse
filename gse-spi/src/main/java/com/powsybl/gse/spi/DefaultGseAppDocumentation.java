package com.powsybl.gse.spi;

import com.powsybl.commons.config.ModuleConfig;
import com.powsybl.commons.config.PlatformConfig;
import javafx.application.Application;

import java.util.Optional;

/**
 * Application documentation provider for FARAO GSE.
 *
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 * @author Marc Erkol {@literal <marc.erkol@rte-france.com>}
 */

public final class DefaultGseAppDocumentation implements GseAppDocumentation {
    private final Application application;
    private final String url;

    public static Optional<GseAppDocumentation> getOptionalDocumentation(Application application) {
        Optional<ModuleConfig> gseModuleConfig = PlatformConfig.defaultConfig().getOptionalModuleConfig("gse-documentation");
        if (gseModuleConfig.isPresent() && gseModuleConfig.get().hasProperty("documentation-url")) {
            return Optional.of(new DefaultGseAppDocumentation(application, gseModuleConfig.get().getStringProperty("documentation-url")));
        } else {
            return Optional.empty();
        }
    }

    private DefaultGseAppDocumentation(Application application, String url) {
        this.application = application;
        this.url = url;
    }

    @Override
    public void show() {
        application.getHostServices().showDocument(url);
    }
}
