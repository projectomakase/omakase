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
package org.projectomakase.omakase.broker;

import org.projectomakase.omakase.jcr.JcrEntity;
import org.jcrom.annotations.JcrNode;
import org.jcrom.annotations.JcrProperty;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * Represents an Omakase Worker.
 *
 * @author Richard Lucas
 */
@JcrNode(mixinTypes = {"omakase:worker"})
public class Worker extends JcrEntity {

    public static final String WORKER_NAME = "omakase:name";
    public static final String EXTERNAL_IDS = "omakase:externalIds";
    public static final String STATUS = "omakase:status";
    public static final String STATUS_TIMESTAMP = "omakase:statusTimestamp";
    public static final String TASKS = "omakase:tasks";

    @JcrProperty(name = WORKER_NAME)
    private String workerName;
    @JcrProperty(name = EXTERNAL_IDS)
    private List<String> externalIds;
    @JcrProperty(name = STATUS)
    private WorkerStatus status = WorkerStatus.ACTIVE;
    @JcrProperty(name = STATUS_TIMESTAMP)
    private Date statusTimestamp = new Date();
    @JcrProperty(name = TASKS)
    private List<String> tasks;

    public String getWorkerName() {
        return workerName;
    }

    public void setWorkerName(String workerName) {
        this.workerName = workerName;
    }

    public List<String> getExternalIds() {
        if (externalIds == null) {
            externalIds = new ArrayList<>();
        }
        return externalIds;
    }

    public void setExternalIds(List<String> externalIds) {
        this.externalIds = externalIds;
    }

    public WorkerStatus getStatus() {
        return status;
    }

    public void setStatus(WorkerStatus status) {
        this.status = status;
    }

    public Date getStatusTimestamp() {
        return statusTimestamp;
    }

    public void setStatusTimestamp(Date statusTimestamp) {
        this.statusTimestamp = statusTimestamp;
    }

    public List<String> getTasks() {
        if (tasks == null) {
            tasks = new ArrayList<>();
        }
        return tasks;
    }

    public void setTasks(List<String> tasks) {
        this.tasks = tasks;
    }

    public static class Builder {
        @FunctionalInterface
        public interface WorkerSetter extends Consumer<Worker> {
        }

        public static Worker build(WorkerSetter... workerSetters) {
            final Worker worker = new Worker();

            Stream.of(workerSetters).forEach(s -> s.accept(worker));

            return worker;
        }
    }
}
