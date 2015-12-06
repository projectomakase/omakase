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

import com.google.common.collect.ImmutableMap;
import org.projectomakase.omakase.exceptions.InvalidPropertyException;
import org.projectomakase.omakase.job.Job;
import org.projectomakase.omakase.job.JobStatus;
import org.projectomakase.omakase.job.JobType;
import org.projectomakase.omakase.job.configuration.JobConfiguration;
import org.projectomakase.omakase.job.rest.v1.model.JobModel;
import org.projectomakase.omakase.rest.converter.RepresentationConverter;
import org.projectomakase.omakase.rest.model.v1.Href;
import org.projectomakase.omakase.rest.model.v1.ResourceStatusModel;
import org.modelmapper.ModelMapper;
import org.modelmapper.PropertyMap;
import org.reflections.Reflections;

import javax.inject.Inject;
import javax.ws.rs.core.UriInfo;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.Set;

/**
 * Job Representation Converter
 *
 * @author Richard Lucas
 */
public class JobRepresentationConverter implements RepresentationConverter<JobModel, Job> {

    @Inject
    Reflections reflections;

    private static ModelMapper modelMapper;

    static {
        modelMapper = new ModelMapper();
        modelMapper.addMappings(new JobPropertyMap());
    }

    @Override
    public JobModel from(UriInfo uriInfo, Job job) {
        JobModel jobModel = new JobModel();
        jobModel.setId(job.getId());
        jobModel.setName(job.getJobName());
        jobModel.setExternalIds(job.getExternalIds());
        jobModel.setType(job.getJobType().name());
        jobModel.setStatus(new ResourceStatusModel(job.getStatus().name(), ZonedDateTime.ofInstant(job.getStatusTimestamp().toInstant(), ZoneId.systemDefault())));
        jobModel.setPriority(job.getPriority());
        jobModel.setCreated(ZonedDateTime.ofInstant(job.getCreated().toInstant(), ZoneId.systemDefault()));
        jobModel.setCreatedBy(job.getCreatedBy());
        jobModel.setLastModified(ZonedDateTime.ofInstant(job.getLastModified().toInstant(), ZoneId.systemDefault()));
        jobModel.setLastModifiedBy(job.getLastModifiedBy());
        ImmutableMap.Builder<String, Href> builder = ImmutableMap.builder();
        builder.put("self", new Href(getJobResourceUrlAsString(uriInfo, job, null)));
        builder.put("configuration", new Href(getJobResourceUrlAsString(uriInfo, job, "configuration")));
        builder.put("status", new Href(getJobResourceUrlAsString(uriInfo, job, "status")));
        jobModel.setLinks(builder.build());
        return jobModel;
    }

    @Override
    public Job to(UriInfo uriInfo, JobModel representation) {
        return Job.Builder.build(j -> {
            j.setJobName(representation.getName());
            j.setExternalIds(representation.getExternalIds());
            j.setJobType(JobType.valueOf(representation.getType()));
            Optional.ofNullable(representation.getStatus()).map(ResourceStatusModel::getCurrent).map(JobStatus::valueOf).ifPresent(j::setStatus);
            j.setJobConfiguration(modelMapper.map(representation.getConfiguration(), getJobConfigurationClass(representation.getType())));
            Optional.ofNullable(representation.getPriority()).ifPresent(j::setPriority);
        });
    }

    @Override
    public Job update(UriInfo uriInfo, JobModel jobModel, Job target) {
        modelMapper.map(jobModel, target);
        return target;
    }

    private Class<? extends JobConfiguration> getJobConfigurationClass(String type) {
        Set<Class<? extends JobConfiguration>> subTypes = reflections.getSubTypesOf(JobConfiguration.class);
        return subTypes.stream().filter(clazz -> clazz.getSimpleName().toUpperCase().contains(type)).findFirst().orElseThrow(() -> new InvalidPropertyException("Unsupported job type " + type));
    }

    private String getJobResourceUrlAsString(UriInfo uriInfo, Job job, String path) {
        if (path == null) {
            return getResourceUriAsString(uriInfo, "jobs", job.getId());
        } else {
            return getResourceUriAsString(uriInfo, "jobs", job.getId(), path);
        }
    }

    private static class JobPropertyMap extends PropertyMap<JobModel, Job> {
        @Override
        protected void configure() {
            skip().setId(null);
            skip().setStatusTimestamp(null);
            map(source.getStatus().getCurrent(), destination.getStatus());
        }
    }
}
