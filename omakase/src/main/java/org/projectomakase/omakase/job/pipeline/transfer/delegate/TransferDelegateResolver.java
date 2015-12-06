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
package org.projectomakase.omakase.job.pipeline.transfer.delegate;

import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

/**
 * {@link TransferDelegate} resolver.
 *
 * @author Richard Lucas
 */
public class TransferDelegateResolver {

    @Inject
    @Any
    Instance<TransferDelegate> transferDelegates;

    public TransferDelegate resolve(String transferType) {
        switch (transferType) {
            case "INGEST":
                return transferDelegates.select(IngestDelegate.class).get();
            case "EXPORT":
                return transferDelegates.select(ExportDelegate.class).get();
            case "REPLICATION":
                return transferDelegates.select(ReplicationDelegate.class).get();
            default:
                throw new UnsupportedOperationException();
        }
    }
}
