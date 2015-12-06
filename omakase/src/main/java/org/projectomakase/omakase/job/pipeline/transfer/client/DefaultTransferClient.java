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
package org.projectomakase.omakase.job.pipeline.transfer.client;

import com.google.common.collect.ImmutableList;
import org.projectomakase.omakase.commons.exceptions.OmakaseRuntimeException;
import org.projectomakase.omakase.job.pipeline.transfer.TransferFile;
import org.projectomakase.omakase.job.pipeline.transfer.TransferFileGroup;
import org.projectomakase.omakase.job.task.TaskGroup;
import org.projectomakase.omakase.job.task.TaskManager;
import org.projectomakase.omakase.task.api.Task;
import org.projectomakase.omakase.task.providers.transfer.ContentInfo;
import org.projectomakase.omakase.task.providers.transfer.IOInstruction;
import org.projectomakase.omakase.task.providers.transfer.TransferTaskConfiguration;
import org.projectomakase.omakase.task.providers.transfer.TransferTaskOutput;
import org.projectomakase.omakase.task.spi.TaskConfiguration;
import org.projectomakase.omakase.task.spi.TaskOutput;
import org.projectomakase.omakase.commons.collectors.ImmutableListCollector;

import javax.inject.Inject;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author Richard Lucas
 */
public class DefaultTransferClient implements TransferClient {

    private static final String TRANSFER = "TRANSFER";

    @Inject
    TaskManager taskManager;

    @Override
    public TransferFileGroup initiateTransferFileGroup(TransferFileGroup transferFileGroup) {
        return transferFileGroup;
    }

    @Override
    public TransferFileGroup completeTransferFileGroup(TransferFileGroup transferFileGroup, TaskOutput taskOutput) {
        Map<URI, String> sourceToTransferFileIdMap = transferFileGroup.getTransferFiles().stream().collect(Collectors.toMap(TransferFile::getSource, TransferFile::getId));
        Map<String, TransferFile> transferFileMap = transferFileGroup.getTransferFiles().stream().collect(Collectors.toMap(TransferFile::getId, Function.identity()));
        TransferTaskOutput transferTaskOutput = (TransferTaskOutput) taskOutput;
        if (transferFileGroup.getTransferFiles().size() > 1) {
            return TransferFileGroup.builder(transferFileGroup).transferFiles(transferTaskOutput.getContentInfos().stream().map(contentInfo -> {
                String id = sourceToTransferFileIdMap.get(contentInfo.getSource());
                TransferFile transferFile = transferFileMap.get(id);
                if (transferFile == null) {
                    throw new OmakaseRuntimeException("Unable to find transfer file " + id);
                }
                return TransferFile.builder(transferFile).size(contentInfo.getSize()).outputHashes(contentInfo.getHashes()).build();
            }).collect(ImmutableListCollector.toImmutableList())).build();
        } else {
            ContentInfo contentInfo = transferTaskOutput.getContentInfos().get(0);
            TransferFile transferFile = transferFileGroup.getTransferFiles().get(0);
            return TransferFileGroup.builder(transferFileGroup)
                    .transferFiles(ImmutableList.of(TransferFile.builder(transferFile).size(contentInfo.getSize()).outputHashes(contentInfo.getHashes()).build())).build();
        }
    }

    @Override
    public void abortTransferFileGroup(TransferFileGroup transferFileGroup) {
        // no-op
    }

    @Override
    public Task createTask(TransferFileGroup transferFileGroup, TaskGroup taskGroup, String description, int priority) {
        List<TransferFile> transferFiles = transferFileGroup.getTransferFiles();
        if (transferFiles.size() > 1) {
            List<IOInstruction> ioInstructions =
                    transferFileGroup.getTransferFiles().stream().map(transferFile -> new IOInstruction(transferFile.getSource(), transferFile.getDestination())).collect(
                            ImmutableListCollector.toImmutableList());
            TransferTaskConfiguration configuration = new TransferTaskConfiguration(ioInstructions, ImmutableList.of("MD5"));
            return taskManager.createTask(taskGroup, new Task(TRANSFER, description, priority, configuration));
        } else {
            TransferFile transferFile = transferFileGroup.getTransferFiles().get(0);
            TaskConfiguration configuration = new TransferTaskConfiguration(ImmutableList.of(new IOInstruction(transferFile.getSource(), transferFile.getDestination())), ImmutableList.of("MD5"));
            return taskManager.createTask(taskGroup, new Task(TRANSFER, description, priority, configuration));
        }
    }
}
