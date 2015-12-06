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

import org.projectomakase.omakase.job.pipeline.transfer.TransferFile;
import org.projectomakase.omakase.job.pipeline.transfer.TransferFileGroup;

import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

/**
 * {@link TransferClient} resolver.
 *
 * @author Richard Lucas
 */
public class TransferClientResolver {

    @Inject
    @Any
    Instance<TransferClient> transferClients;

    public TransferClient resolve(TransferFileGroup transferFileGroup) {
        if (transferFileGroup.getTransferFiles().size() > 1) {
            return transferClients.select(DefaultTransferClient.class).get();
        } else {
            return resolve(transferFileGroup.getTransferFiles().get(0));
        }
    }

    private TransferClient resolve(TransferFile transferFile) {
        String clientType = transferFile.getDestination().getScheme().toUpperCase();
        switch (clientType) {
            case "S3":
                return transferClients.select(S3TransferClient.class).get();
            case "GLACIER":
                return transferClients.select(GlacierTransferClient.class).get();
            default:
                return transferClients.select(DefaultTransferClient.class).get();
        }
    }
}
