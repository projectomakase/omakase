/*
 * #%L
 * omakase
 * %%
 * Copyright (C) 2015 Project Omakase LLC
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package org.projectomakase.omakase.version.rest.v1;

import org.projectomakase.omakase.rest.etag.EntityTagGenerator;
import org.projectomakase.omakase.version.Version;
import org.jboss.resteasy.annotations.GZIP;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObjectBuilder;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import java.io.IOException;

/**
 * Exposes a REST API for retrieving the applications version.
 *
 * @author Richard Lucas
 */
@Path("version")
@Consumes({MediaType.APPLICATION_JSON, "application/v1+json"})
@Produces({MediaType.APPLICATION_JSON, "application/v1+json"})
public class VersionResource {

    @Inject
    Version version;
    @Context
    Request request;

    @GET
    @GZIP
    public Response getVersion() throws IOException {
        EntityTag etag = EntityTagGenerator.entityTagFromString(version.getVersion() + version.getBuildTag() + version.getBuildTime());
        Response.ResponseBuilder responseBuilder = request.evaluatePreconditions(etag);

        if (responseBuilder != null) {
            return responseBuilder.tag(etag).build();
        } else {
            JsonObjectBuilder jsonObjectBuilder = Json.createObjectBuilder();
            jsonObjectBuilder.add("version", version.getVersion()).add("buildTag", version.getBuildTag()).add("buildTime", version.getBuildTime());
            return Response.ok(jsonObjectBuilder.build()).tag(etag).build();
        }
    }
}
