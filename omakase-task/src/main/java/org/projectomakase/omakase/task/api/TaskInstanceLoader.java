/*
 * #%L
 * omakase-task
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
package org.projectomakase.omakase.task.api;

import com.google.common.collect.ImmutableMap;
import org.projectomakase.omakase.commons.functions.Throwables;
import org.projectomakase.omakase.task.spi.TaskConfiguration;
import org.projectomakase.omakase.task.spi.TaskOutput;

import java.util.Optional;

/**
 * Task Instance Loader.
 * <p>
 * Used to load a new instance of a task's configuration or output implementation using the task type.
 * <p>
 * Uses a hardcoded mapping between the task type and implementation to use. In the future this will be replaced with a more dynamic loader that retrieve the mappings from a JAR's META-INF.
 * </p>
 *
 * @author Richard Lucas
 */
public final class TaskInstanceLoader {

    private final static ImmutableMap<String, String> TASK_TYPE_TO_CONFIG;
    private final static ImmutableMap<String, String> TASK_TYPE_TO_OUTPUT;

    static {
        ImmutableMap.Builder<String, String> configBuilder = ImmutableMap.builder();
        configBuilder.put("GLACIER_UPLOAD", "org.projectomakase.omakase.task.providers.aws.glacier.GlacierUploadTaskConfiguration");
        configBuilder.put("S3_UPLOAD", "org.projectomakase.omakase.task.providers.aws.s3.S3UploadTaskConfiguration");
        configBuilder.put("DELETE", "org.projectomakase.omakase.task.providers.delete.DeleteTaskConfiguration");
        configBuilder.put("HASH", "org.projectomakase.omakase.task.providers.hash.HashTaskConfiguration");
        configBuilder.put("MANIFEST_TRANSFER", "org.projectomakase.omakase.task.providers.manifest.ManifestTransferTaskConfiguration");
        configBuilder.put("RESTORE", "org.projectomakase.omakase.task.providers.restore.RestoreTaskConfiguration");
        configBuilder.put("TRANSFER", "org.projectomakase.omakase.task.providers.transfer.TransferTaskConfiguration");
        TASK_TYPE_TO_CONFIG = configBuilder.build();

        ImmutableMap.Builder<String, String> outputBuilder = ImmutableMap.builder();
        outputBuilder.put("GLACIER_UPLOAD", "org.projectomakase.omakase.task.providers.aws.glacier.GlacierUploadTaskOutput");
        outputBuilder.put("S3_UPLOAD", "org.projectomakase.omakase.task.providers.aws.s3.S3UploadTaskOutput");
        outputBuilder.put("DELETE", "org.projectomakase.omakase.task.providers.delete.DeleteTaskOutput");
        outputBuilder.put("HASH", "org.projectomakase.omakase.task.providers.hash.HashTaskOutput");
        outputBuilder.put("MANIFEST_TRANSFER", "org.projectomakase.omakase.task.providers.manifest.ManifestTransferTaskOutput");
        outputBuilder.put("RESTORE", "org.projectomakase.omakase.task.providers.restore.RestoreTaskOutput");
        outputBuilder.put("TRANSFER", "org.projectomakase.omakase.task.providers.transfer.TransferTaskOutput");
        TASK_TYPE_TO_OUTPUT = outputBuilder.build();
    }

    private TaskInstanceLoader() {
        // hides default public constructor
    }

    public static TaskConfiguration loadTaskConfigurationInstance(String taskType) {
        String className = Optional.ofNullable(TASK_TYPE_TO_CONFIG.get(taskType)).orElseThrow(() -> new IllegalArgumentException(taskType + " does not have a known task configuration instance"));
        return (TaskConfiguration) Throwables.returnableInstance(() -> Class.forName(className).newInstance());
    }

    public static TaskOutput loadTaskOutputInstance(String taskType) {
        String className = Optional.ofNullable(TASK_TYPE_TO_OUTPUT.get(taskType)).orElseThrow(() -> new IllegalArgumentException(taskType + " does not have a known task output instance"));
        return (TaskOutput) Throwables.returnableInstance(() -> Class.forName(className).newInstance());
    }
}
