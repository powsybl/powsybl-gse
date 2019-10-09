/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.gse.copypaste.afs.server;

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

/**
 * @author Nassirou Nambiema <nassirou.nambiena at rte-france.com>
 */
@Named
@ApplicationScoped
@Path("copypaste")
@JwtTokenNeeded
public class CopyServer {
/*
    private CopyManager copyManager;
    @Inject
    private AppDataBean appDataBean;

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("fileSystems/{fileSystemName}/nodes/{nodeId}/copy")
    public Response copy(@PathParam("fileSystemName") String fileSystemName, @PathParam("nodeId") String nodeId, MappingAnalysisParameters parameters) {
        //appDataBean.getProjectFile(fileSystemName, nodeId, MultiCaseConfig.class).run(parameters);
        //copyManager.copy()
        return Response.ok().build();
    }

    @PostConstruct
    private void init(){
        copyManager = new CopyManager();
    }*/
}