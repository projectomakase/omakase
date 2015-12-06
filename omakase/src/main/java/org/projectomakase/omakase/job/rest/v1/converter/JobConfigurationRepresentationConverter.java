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
package org.projectomakase.omakase.job.rest.v1.converter;

import org.projectomakase.omakase.exceptions.NotUpdateableException;
import org.projectomakase.omakase.job.configuration.JobConfiguration;
import org.projectomakase.omakase.job.rest.v1.model.JobConfigurationModel;
import org.projectomakase.omakase.rest.converter.RepresentationConverter;
import org.modelmapper.ModelMapper;
import org.reflections.Reflections;

import javax.inject.Inject;
import javax.ws.rs.core.UriInfo;
import java.util.Set;

/**
 * Job Representation Converter
 *
 * @author Richard Lucas
 */
public class JobConfigurationRepresentationConverter implements RepresentationConverter<JobConfigurationModel, JobConfiguration> {

    @Inject
    Reflections reflections;

    private static ModelMapper modelMapper = new ModelMapper();

    @Override
    public JobConfigurationModel from(UriInfo uriInfo, JobConfiguration jobConfiguration) {
        return modelMapper.map(jobConfiguration, getJobConfigurationModelClass(jobConfiguration));
    }

    @Override
    public JobConfiguration to(UriInfo uriInfo, JobConfigurationModel representation) {
        throw new UnsupportedOperationException();
    }

    @Override
    public JobConfiguration update(UriInfo uriInfo, JobConfigurationModel jobConfigurationModel, JobConfiguration target) {
        modelMapper.map(jobConfigurationModel, target);
        return target;
    }

    private Class<? extends JobConfigurationModel> getJobConfigurationModelClass(JobConfiguration jobConfiguration) {
        Set<Class<? extends JobConfigurationModel>> subTypes = reflections.getSubTypesOf(JobConfigurationModel.class);
        return subTypes.stream().filter(clazz -> clazz.getSimpleName().toUpperCase().contains(jobConfiguration.getClass().getSimpleName().toUpperCase())).findFirst()
                .orElseThrow(() -> new NotUpdateableException("Unsupported job configuration"));
    }
}
