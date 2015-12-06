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

import org.projectomakase.omakase.exceptions.NotFoundException;
import org.projectomakase.omakase.exceptions.NotUpdateableException;
import org.projectomakase.omakase.job.Job;
import org.projectomakase.omakase.job.JobManager;
import org.projectomakase.omakase.job.configuration.JobConfiguration;
import org.projectomakase.omakase.job.rest.v1.model.JobConfigurationModel;
import org.projectomakase.omakase.rest.JsonStrings;
import org.reflections.Reflections;

import javax.inject.Inject;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.ReaderInterceptor;
import javax.ws.rs.ext.ReaderInterceptorContext;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Set;

/**
 * ReaderInterceptor implementation used to lookup and set the concrete {@link JobConfiguration} implementation that the repository configuration JSON payload should be
 * mapped to.
 *
 * @author Richard Lucas
 */
@PutJobConfig
public class PutJobConfigInterceptor implements ReaderInterceptor {

    @Context
    UriInfo uriInfo;

    @Inject
    JobManager jobManager;

    @Inject
    Reflections reflections;

    @Override
    public Object aroundReadFrom(ReaderInterceptorContext context) throws IOException {

        String json = JsonStrings.inputStreamToString(context.getInputStream());
        JsonStrings.isNotNullOrEmpty(json);
        context.setInputStream(new ByteArrayInputStream(json.getBytes(Charset.forName("UTF-8"))));

        String jobId = uriInfo.getPathParameters().getFirst("jobId");
        Job job = jobManager.getJob(jobId).orElseThrow(NotFoundException::new);
        Class<? extends JobConfigurationModel> configurationModelType = getJobConfigurationModelClass(job.getJobConfiguration());
        context.setType(configurationModelType);
        context.setGenericType(configurationModelType);
        return context.proceed();

    }

    private Class<? extends JobConfigurationModel> getJobConfigurationModelClass(JobConfiguration jobConfiguration) {
        Set<Class<? extends JobConfigurationModel>> subTypes = reflections.getSubTypesOf(JobConfigurationModel.class);
        return subTypes.stream().filter(clazz -> clazz.getSimpleName().toUpperCase().contains(jobConfiguration.getClass().getSimpleName().toUpperCase())).findFirst()
                .orElseThrow(() -> new NotUpdateableException("Unsupported job configuration"));
    }
}
