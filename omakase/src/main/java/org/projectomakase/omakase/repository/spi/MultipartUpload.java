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
package org.projectomakase.omakase.repository.spi;

import org.projectomakase.omakase.commons.aws.MultipartUploadInfo;

/**
 * Implemented by repository provider implementations that support/require files be upload using multiple parts.
 * <p>
 * Repository Provider implementations that need to support multipart upload semantics should implement this interface. Implementing this interface ensures that a multipart upload will be performed,
 * if required, when ingesting or replicating content into the repository.
 * </p>
 *
 * @author Richard Lucas
 */
public interface MultipartUpload {

    boolean requiresMultipartUpload();
    MultipartUploadInfo getMultipartUploadInfo();
}
