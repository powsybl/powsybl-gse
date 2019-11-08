/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.gse.copypaste.afs.server;

import com.powsybl.afs.AbstractNodeBase;
import com.powsybl.afs.FolderBase;
import com.powsybl.afs.ws.server.utils.AppDataBean;
import com.powsybl.afs.ws.server.utils.JwtTokenNeeded;
import com.powsybl.gse.copypaste.afs.CopyManager;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.Map;

/**
 * @author Nassirou Nambiema <nassirou.nambiena at rte-france.com>
 */
@Named
@ApplicationScoped
@Path("copypaste")
@JwtTokenNeeded
public class CopyServer {

    @Inject
    private CopyManager copyManager;

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("fileSystems/{fileSystemName}/nodes/{nodeId}/copy")
    public Map<String, CopyManager.CopyInfo> copy(List<? extends AbstractNodeBase> nodes) {
        return copyManager.copy(nodes);
    }


    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("fileSystems/{fileSystemName}/nodes/{nodeId}/paste")
    public Response paste(@PathParam("fileSystemName") String fileSystemName, @PathParam("nodeId") String nodeId, AbstractNodeBase folder) {
        copyManager.paste(fileSystemName, nodeId, folder);
        return Response.ok().build();
    }

    @PostConstruct
    private void init() {
        copyManager = new CopyManager();
    }
}
