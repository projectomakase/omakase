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
package org.projectomakase.omakase.job.pipeline.transfer;

import com.google.common.base.Splitter;
import org.projectomakase.omakase.commons.exceptions.OmakaseRuntimeException;
import org.projectomakase.omakase.commons.functions.Throwables;
import org.projectomakase.omakase.pipeline.Pipelines;
import org.projectomakase.omakase.pipeline.stage.PipelineContext;

import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Transfer utility methods.
 *
 * @author Richard Lucas
 */
public final class TransferPipeline {

    public static final String TRANSFER = "transfer";
    public static final String MANIFEST_TRANSFER = "manifestTransfer";

    private TransferPipeline() {
        // hide default constructor
    }

    /**
     * Returns the {@link Transfer} stored in the {@link PipelineContext}.
     *
     * @param pipelineContext
     *         the {@link PipelineContext}
     * @return the {@link Transfer} stored in the {@link PipelineContext}.
     */
    public static Transfer getTransferFromPipelineContext(PipelineContext pipelineContext) {
        return Transfer.fromJson(Pipelines.getPipelineProperty(pipelineContext, TRANSFER));
    }

    /**
     * Returns the {@link TransferFileGroup} associated with the task id in the {@link PipelineContext}.
     *
     * @param pipelineContext
     *         the {@link PipelineContext}
     * @param taskId
     *         the task id
     * @return the {@link TransferFileGroup} associated with the task id in the {@link PipelineContext}.
     */
    public static TransferFileGroup getTransferFileGroupFromPipelineContext(PipelineContext pipelineContext, String taskId) {
        String transferFileGroupId = Pipelines.getPipelineProperty(pipelineContext, taskId);
        Transfer transfer = getTransferFromPipelineContext(pipelineContext);
        List<TransferFileGroup> transferFileGroups = transfer.getTransferFileGroups();
        return transferFileGroups.stream().filter(transferFileGroup -> transferFileGroup.getId().equals(transferFileGroupId)).findFirst()
                .orElseThrow(() -> new OmakaseRuntimeException("pipeline context does not contain a transfer file  group for task " + taskId));
    }

    /**
     * Returns true if the transfer has one or more files otherwise false.
     *
     * @param transfer
     *         the transfer
     * @return true if the transfer has one or more files otherwise false.
     */
    public static boolean doesTransferHaveFiles(Transfer transfer) {
        return transfer.getTransferFileGroups()
                .stream()
                .filter(transferFileGroup -> !transferFileGroup.getTransferFiles().isEmpty())
                .findAny()
                .isPresent();
    }

    /**
     * Returns the {@link ManifestTransfer} stored in the {@link PipelineContext}.
     *
     * @param pipelineContext
     *         the {@link PipelineContext}
     * @return the {@link ManifestTransfer} stored in the {@link PipelineContext}.
     */
    public static ManifestTransfer getManifestTransferFromPipelineContext(PipelineContext pipelineContext) {
        return ManifestTransfer.fromJson(Pipelines.getPipelineProperty(pipelineContext, MANIFEST_TRANSFER));
    }

    /**
     * Returns the {@link ManifestTransferFile} associated with the task id in the {@link PipelineContext}.
     *
     * @param pipelineContext
     *         the {@link PipelineContext}
     * @param taskId
     *         the task id
     * @return the {@link ManifestTransferFile} associated with the task id in the {@link PipelineContext}.
     */
    public static ManifestTransferFile getManifestTransferFileFromPipelineContext(PipelineContext pipelineContext, String taskId) {
        String manifestTransferFileId = Pipelines.getPipelineProperty(pipelineContext, taskId);

        ManifestTransfer manifestTransfer = getManifestTransferFromPipelineContext(pipelineContext);

        return manifestTransfer.getManifestTransferFiles().stream().filter(transferFile -> transferFile.getId().equals(manifestTransferFileId)).findFirst()
                .orElseThrow(() -> new OmakaseRuntimeException("pipeline context does not contain a transfer file for task " + taskId));
    }

    /**
     * Returns the absolute source URI for a URI referenced in a manifest.
     *
     * @param parent
     *         the manifest transfer file that references the URI
     * @param uri
     *         the URI referenced in the manifest
     * @return the absolute source URI for a URI referenced in a manifest.
     */
    public static URI getAbsoluteSourceUri(ManifestTransferFile parent, URI uri) {
        URI absoluteURI;
        if (uri.isAbsolute()) {
            absoluteURI = uri;
        } else if (uri.toString().startsWith("/")) {
            URI parentUri = parent.getSource();
            absoluteURI = Throwables.returnableInstance(() -> new URI(parentUri.getScheme(), parentUri.getUserInfo(), parentUri.getHost(), parentUri.getPort(), uri.getPath(), null, null));
        } else {
            URI parentUri = parent.getSource();
            List<String> pathParts = Splitter.on("/").splitToList(parentUri.getPath());
            String newPath = pathParts.stream().limit(pathParts.size() - 1L).collect(Collectors.joining("/")) + "/" + uri;
            absoluteURI = Throwables.returnableInstance(() -> new URI(parentUri.getScheme(), parentUri.getUserInfo(), parentUri.getHost(), parentUri.getPort(), newPath, null, null));
        }
        return absoluteURI;
    }

    public static String getOriginalFilepath(ManifestTransfer manifestTransfer, ManifestTransferFile parent, URI uri) {
        String originalFilepath;
        if (uri.isAbsolute()) {
            originalFilepath = uri.getPath().replaceFirst(manifestTransfer.getRootPath().toString() + "/", "");
        } else if (uri.toString().startsWith("/")) {
            originalFilepath = uri.toString();
        } else {
            Path parentPath = Paths.get(parent.getOriginalFilepath());
            originalFilepath = Optional.ofNullable(parentPath.getParent()).map(Path::toString).orElse("") + "/" + uri;
        }

        if (originalFilepath.startsWith("/")) {
            originalFilepath = originalFilepath.replaceFirst("/", "");
        }

        return originalFilepath;
    }

    public static URI getDestinationUri(String repositoryType, URI repositoryUri, String repositoryFilePath, String originalFileName) {
        if ("S3".equals(repositoryType)) {
            return Throwables.returnableInstance(() -> new URI(repositoryUri.toString() + "/" + repositoryFilePath + "?originalFileName=" + originalFileName));
        } else {
            return Throwables.returnableInstance(() -> new URI(repositoryUri.toString() + "/" + repositoryFilePath));
        }
    }
}
