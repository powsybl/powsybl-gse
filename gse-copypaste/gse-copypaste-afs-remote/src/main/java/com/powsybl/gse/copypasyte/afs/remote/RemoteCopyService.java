/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.gse.copypasyte.afs.remote;

import com.google.common.base.Supplier;
import com.powsybl.afs.*;
import com.powsybl.afs.ws.client.utils.ClientUtils;
import com.powsybl.afs.ws.client.utils.RemoteServiceConfig;
import com.powsybl.afs.ws.utils.JsonProvider;
import com.powsybl.gse.copypaste.afs.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.*;

import static com.powsybl.afs.ws.client.utils.ClientUtils.checkOk;


/**
 * @author Nassirou Nambiema <nassirou.nambiena at rte-france.com>
 */
public class RemoteCopyService implements CopyService {

    private static final Logger LOGGER = LoggerFactory.getLogger(RemoteCopyService.class);

    private final CopyManager copyManager = new CopyManager();

    private final Supplier<Optional<RemoteServiceConfig>> configSupplier;

    private final String token;

    RemoteCopyService(Supplier<Optional<RemoteServiceConfig>> configSupplier, String token) {
        this.configSupplier = Objects.requireNonNull(configSupplier);
        this.token = token;
    }

    private static WebTarget createWebTarget(Client client, URI baseUri) {
        return client.target(baseUri)
                .path("rest")
                .path("mappingAnalysis");
    }

    private RemoteServiceConfig getConfig() {
        return Objects.requireNonNull(configSupplier.get()).orElseThrow(() -> new AfsException("Remote service config is missing"));
    }

    @Override
    public void copy(List<? extends AbstractNodeBase> nodes) {
        Objects.requireNonNull(nodes);

        for (AbstractNodeBase node : nodes) {
            AppFileSystem fileSystem;

            if (node instanceof Node) {
                fileSystem = ((Node) node).getFileSystem();
                LOGGER.info("copy(fileSystemName={}, nodeId={})", fileSystem.getName(), node.getId());
            } else {
                fileSystem = ((ProjectNode) node).getFileSystem();
                LOGGER.info("copy(fileSystemName={}, nodeId={})", fileSystem.getName(), node.getId());
            }

            Client client = ClientUtils.createClient()
                    .register(new JsonProvider());
            try {
                WebTarget webTarget = createWebTarget(client, getConfig().getRestUri());

                Response response = webTarget.path("fileSystems/{fileSystemName}/nodes/{nodeId}/copy")
                        .resolveTemplate("fileSystemName", fileSystem.getName())
                        .resolveTemplate("nodeId", node.getId())
                        .request()
                        .header(HttpHeaders.AUTHORIZATION, token)
                        .get();
                try {
                    checkOk(response);
                } finally {
                    response.close();
                }
            } finally {
                client.close();
            }
        }
    }

    @Override
    public void paste(String fileSystemName, String nodeId, AbstractNodeBase folder) throws CopyPasteException {

        Objects.requireNonNull(folder);

        Client client = ClientUtils.createClient()
                .register(new JsonProvider());
        try {
            WebTarget webTarget = createWebTarget(client, getConfig().getRestUri());

            Response response = webTarget.path("fileSystems/{fileSystemName}/nodes/{nodeId}/paste")
                    .resolveTemplate("fileSystemName", fileSystemName)
                    .resolveTemplate("nodeId", nodeId)
                    .request()
                    .header(HttpHeaders.AUTHORIZATION, token)
                    .post(Entity.text(folder.getId()));
            try {
                checkOk(response);
            } catch (AfsException e) {
                if ("copy failed".equals(e.getMessage())) {
                    throw new CopyFailedException("");
                } else if ("copy In progress".equals(e.getMessage())) {
                    throw new CopyNotFinishedException("");
                }
            } finally {
                response.close();
            }
        } finally {
            client.close();
        }

    }

}
