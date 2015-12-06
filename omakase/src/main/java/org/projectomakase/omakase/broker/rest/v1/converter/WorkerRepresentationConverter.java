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
package org.projectomakase.omakase.broker.rest.v1.converter;

import com.google.common.collect.ImmutableMap;
import org.projectomakase.omakase.broker.Worker;
import org.projectomakase.omakase.broker.WorkerStatus;
import org.projectomakase.omakase.broker.rest.v1.model.WorkerModel;
import org.projectomakase.omakase.rest.converter.RepresentationConverter;
import org.projectomakase.omakase.rest.model.v1.Href;
import org.projectomakase.omakase.rest.model.v1.ResourceStatusModel;

import javax.ws.rs.core.UriInfo;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;

/**
 * Worker Representation Converter
 *
 * @author Richard Lucas
 */
public class WorkerRepresentationConverter implements RepresentationConverter<WorkerModel, Worker> {

    @Override
    public WorkerModel from(UriInfo uriInfo, Worker worker) {
        WorkerModel workerModel = new WorkerModel();
        workerModel.setId(worker.getId());
        workerModel.setName(worker.getWorkerName());
        workerModel.setExternalIds(worker.getExternalIds());
        workerModel.setStatus(new ResourceStatusModel(worker.getStatus().name(), ZonedDateTime.ofInstant(worker.getStatusTimestamp().toInstant(), ZoneId.systemDefault())));
        workerModel.setCreated(ZonedDateTime.ofInstant(worker.getCreated().toInstant(), ZoneId.systemDefault()));
        workerModel.setCreatedBy(worker.getCreatedBy());
        workerModel.setLastModified(ZonedDateTime.ofInstant(worker.getLastModified().toInstant(), ZoneId.systemDefault()));
        workerModel.setLastModifiedBy(worker.getLastModifiedBy());
        ImmutableMap.Builder<String, Href> builder = ImmutableMap.builder();
        builder.put("self", new Href(getWorkerResourceUrlAsString(uriInfo, worker, null)));
        builder.put("messages", new Href(getWorkerResourceUrlAsString(uriInfo, worker, "messages")));
        builder.put("status", new Href(getWorkerResourceUrlAsString(uriInfo, worker, "status")));
        builder.put("tasks", new Href(getWorkerResourceUrlAsString(uriInfo, worker, "tasks")));
        workerModel.setLinks(builder.build());
        return workerModel;
    }

    @Override
    public Worker to(UriInfo uriInfo, WorkerModel representation) {
        return Worker.Builder.build(worker -> {
            worker.setWorkerName(representation.getName());
            worker.setExternalIds(representation.getExternalIds());
        });
    }

    @Override
    public Worker update(UriInfo uriInfo, WorkerModel workerModel, Worker worker) {
        worker.setWorkerName(workerModel.getName());
        worker.setExternalIds(workerModel.getExternalIds());
        worker.setStatus(WorkerStatus.valueOf(workerModel.getStatus().getCurrent().toUpperCase()));
        worker.setStatusTimestamp(new Date());
        return worker;
    }

    private String getWorkerResourceUrlAsString(UriInfo uriInfo, Worker worker, String path) {
        if (path == null) {
            return getResourceUriAsString(uriInfo, "broker", "workers", worker.getId());
        } else {
            return getResourceUriAsString(uriInfo, "broker", "workers", worker.getId(), path);
        }
    }
}
