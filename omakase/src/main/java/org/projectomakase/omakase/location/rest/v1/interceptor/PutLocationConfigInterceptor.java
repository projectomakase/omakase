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
package org.projectomakase.omakase.location.rest.v1.interceptor;

import org.projectomakase.omakase.location.LocationManager;
import org.projectomakase.omakase.location.spi.LocationConfiguration;
import org.projectomakase.omakase.rest.JsonStrings;

import javax.inject.Inject;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.Providers;
import javax.ws.rs.ext.ReaderInterceptor;
import javax.ws.rs.ext.ReaderInterceptorContext;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.Charset;

/**
 * ReaderInterceptor implementation used to lookup and set the concrete LocationConfiguration implementation that the location configuration JSON payload should be mapped to.
 *
 * @author Richard Lucas
 */
@PutLocationConfig
public class PutLocationConfigInterceptor implements ReaderInterceptor {

    @Context
    UriInfo uriInfo;

    @Context
    Providers providers;

    @Inject
    LocationManager locationManager;

    @Override
    public Object aroundReadFrom(ReaderInterceptorContext context) throws IOException {

        String json = JsonStrings.inputStreamToString(context.getInputStream());
        JsonStrings.isNotNullOrEmpty(json);
        context.setInputStream(new ByteArrayInputStream(json.getBytes(Charset.forName("UTF-8"))));

        String locationId = uriInfo.getPathParameters().getFirst("locationId");
        Class<? extends LocationConfiguration> configurationType = locationManager.getLocationConfigurationType(locationId);
        context.setType(configurationType);
        context.setGenericType(configurationType);
        return context.proceed();

    }
}
