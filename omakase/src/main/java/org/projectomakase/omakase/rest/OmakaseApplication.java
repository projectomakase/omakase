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
package org.projectomakase.omakase.rest;

import org.projectomakase.omakase.broker.rest.v1.BrokerResource;
import org.projectomakase.omakase.content.rest.v1.ContentResource;
import org.projectomakase.omakase.job.rest.v1.JobManagementResource;
import org.projectomakase.omakase.job.rest.v1.interceptor.PostJobInterceptor;
import org.projectomakase.omakase.job.rest.v1.interceptor.PutJobConfigInterceptor;
import org.projectomakase.omakase.location.rest.v1.LocationResource;
import org.projectomakase.omakase.location.rest.v1.interceptor.PutLocationConfigInterceptor;
import org.projectomakase.omakase.repository.rest.v1.RepositoryResource;
import org.projectomakase.omakase.repository.rest.v1.interceptor.PutRepositoryConfigInterceptor;
import org.projectomakase.omakase.rest.exception.EJBExceptionMapper;
import org.projectomakase.omakase.rest.exception.InvalidPropertyExceptionMapper;
import org.projectomakase.omakase.rest.exception.InvalidSearchConditionExceptionMapper;
import org.projectomakase.omakase.rest.exception.InvalidUUIDExceptionMapper;
import org.projectomakase.omakase.rest.exception.JaxRsNotFoundExceptionMapper;
import org.projectomakase.omakase.rest.exception.JobConfigurationExceptionMapper;
import org.projectomakase.omakase.rest.exception.JsonMappingExceptionMapper;
import org.projectomakase.omakase.rest.exception.JsonParseExceptionMapper;
import org.projectomakase.omakase.rest.exception.NotAuthorizedExceptionMapper;
import org.projectomakase.omakase.rest.exception.NotFoundExceptionMapper;
import org.projectomakase.omakase.rest.exception.NotUpdateableExceptionMapper;
import org.projectomakase.omakase.rest.exception.RepositoryConfigurationExceptionMapper;
import org.projectomakase.omakase.rest.exception.ThrowableExceptionMapper;
import org.projectomakase.omakase.rest.exception.WebApplicationExceptionMapper;
import org.projectomakase.omakase.rest.patch.JsonPatchInterceptor;
import org.projectomakase.omakase.rest.provider.JsonProvider;
import org.projectomakase.omakase.rest.status.v1.StatusResource;
import org.projectomakase.omakase.rest.version.APIVersionRequestFilter;
import org.projectomakase.omakase.version.rest.v1.VersionResource;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;
import java.util.HashSet;
import java.util.Set;

/**
 * Omakase REST Application
 *
 * @author Richard Lucas
 */
@ApplicationPath("api")
public class OmakaseApplication extends Application {

    @Override
    public Set<Class<?>> getClasses() {
        Set<Class<?>> resources = new HashSet<>();
        resources.addAll(getProviders());
        resources.addAll(getInterceptorsAndFilters());
        resources.addAll(getOmakaseAPIResources());
        resources.addAll(getExceptionMappers());
        return resources;
    }

    private Set<Class<?>> getOmakaseAPIResources() {
        Set<Class<?>> resources = new HashSet<>();
        resources.add(ContentResource.class);
        resources.add(JobManagementResource.class);
        resources.add(RepositoryResource.class);
        resources.add(LocationResource.class);
        resources.add(BrokerResource.class);
        resources.add(StatusResource.class);
        resources.add(VersionResource.class);
        return resources;
    }

    private Set<Class<?>> getExceptionMappers() {
        Set<Class<?>> resources = new HashSet<>();
        resources.add(ThrowableExceptionMapper.class);
        resources.add(WebApplicationExceptionMapper.class);
        resources.add(EJBExceptionMapper.class);
        resources.add(NotAuthorizedExceptionMapper.class);
        resources.add(NotFoundExceptionMapper.class);
        resources.add(InvalidPropertyExceptionMapper.class);
        resources.add(JaxRsNotFoundExceptionMapper.class);
        resources.add(JsonMappingExceptionMapper.class);
        resources.add(JsonParseExceptionMapper.class);
        resources.add(InvalidSearchConditionExceptionMapper.class);
        resources.add(RepositoryConfigurationExceptionMapper.class);
        resources.add(NotUpdateableExceptionMapper.class);
        resources.add(InvalidUUIDExceptionMapper.class);
        resources.add(JobConfigurationExceptionMapper.class);
        return resources;
    }

    private Set<Class<?>> getProviders() {
        Set<Class<?>> resources = new HashSet<>();
        resources.add(JsonProvider.class);
        return resources;
    }

    private Set<Class<?>> getInterceptorsAndFilters() {
        Set<Class<?>> resources = new HashSet<>();
        resources.add(JsonPatchInterceptor.class);
        resources.add(PostJobInterceptor.class);
        resources.add(PutJobConfigInterceptor.class);
        resources.add(PutRepositoryConfigInterceptor.class);
        resources.add(PutLocationConfigInterceptor.class);
        resources.add(APIVersionRequestFilter.class);
        return resources;
    }
}
