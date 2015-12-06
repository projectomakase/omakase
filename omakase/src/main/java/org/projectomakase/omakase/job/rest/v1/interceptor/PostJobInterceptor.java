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
package org.projectomakase.omakase.job.rest.v1.interceptor;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.CaseFormat;
import org.projectomakase.omakase.Omakase;
import org.projectomakase.omakase.exceptions.InvalidPropertyException;
import org.projectomakase.omakase.job.JobType;
import org.projectomakase.omakase.rest.JsonStrings;
import org.projectomakase.omakase.job.rest.v1.model.JobModel;

import javax.inject.Inject;
import javax.ws.rs.ext.ReaderInterceptor;
import javax.ws.rs.ext.ReaderInterceptorContext;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Optional;

/**
 * ReaderInterceptor implementation used to modify the JSON payload of POST /jobs prior to deserializing into {@link JobModel}
 * <p>
 * Determines the job configuration class from the job type and add it to the JSON payload so that it can be correctly deserialized. Assumes the configuration class name conforms to the following
 * pattern org.projectomakase.omakase.job.rest.v1.model. + UpperCamel + JobConfigurationModel and the job tye confirms to UPPER_UNDERSCORE e.g. type = INGEST class =
 * org.projectomakase.omakase.job.rest.v1.model.IngestJobConfigurationModel
 * </p>
 * <p>
 * Wraps the status in a status object if it is included in the payload.
 * </p>
 *
 * @author Richard Lucas
 */
@PostJob
public class PostJobInterceptor implements ReaderInterceptor {

    private static final String PACKAGE = "org.projectomakase.omakase.job.rest.v1.model.";
    private static final String CLASS_SUFFIX = "JobConfigurationModel";


    @Omakase
    @Inject
    ObjectMapper objectMapper;

    @Override
    public Object aroundReadFrom(ReaderInterceptorContext context) throws IOException {
        String json = JsonStrings.inputStreamToString(context.getInputStream());
        JsonStrings.isNotNullOrEmpty(json);

        JsonNode jsonNode = objectMapper.readValue(json, JsonNode.class);

        addJobTypeImplementationToPayload(jsonNode);

        wrapStatusInObject(jsonNode);

        ByteArrayOutputStream resultAsByteArray = new ByteArrayOutputStream();
        objectMapper.writeValue(resultAsByteArray, jsonNode);
        context.setInputStream(new ByteArrayInputStream(resultAsByteArray.toByteArray()));

        return context.proceed();

    }

    private static void addJobTypeImplementationToPayload(JsonNode jsonNode) throws JsonMappingException {
        JsonNode typeNode = jsonNode.get("type");
        String type;
        if (typeNode != null) {
            type = typeNode.asText();
        } else {
            throw new JsonMappingException("'type' is a required property");
        }

        try {
            JobType.valueOf(type);
        } catch (IllegalArgumentException e) {
            throw new InvalidPropertyException("Unsupported job type", e);
        }

        JsonNode configurationNode = jsonNode.get("configuration");
        if (configurationNode == null) {
            throw new JsonMappingException("'configuration' is a required property");
        }

        // the @class property is used by jackson to deserialize the configuration in the JSON payload into the correct model object
        ((ObjectNode) jsonNode.get("configuration")).put("@class", getConfigurationClassName(type));
    }

    private static void wrapStatusInObject(JsonNode jsonNode) {
        // Modify the POST jobs payload to wrap status in a status object
        Optional.ofNullable(jsonNode.get("status")).ifPresent(status -> {
            ((ObjectNode) jsonNode).remove("status");
            ObjectNode statusModel = ((ObjectNode) jsonNode).putObject("status");
            statusModel.put("current", status.textValue());
        });
    }

    private static String getConfigurationClassName(String type) {
        String formatted = CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, type);
        return PACKAGE + formatted + CLASS_SUFFIX;
    }


}
