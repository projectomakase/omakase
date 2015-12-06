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

import com.google.common.collect.ImmutableList;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Richard Lucas
 */
public class TransferTest {

    private static final String TRANSFER_FILE_JSON = "{\"id\":\"123\",\"source_repository_file_id\":\"abc\",\"destination_repository_file_id\":\"abc\",\"original_file_name\":\"test\"," +
            "\"original_file_path\":\"test\",\"source\":\"file:/test\",\"destination\":\"file:/test2\",\"size\":1024,\"source_hashes\":[],\"output_hashes\":[],\"parts\":[]}";

    private static final String TRANSFER_FILE_GROUP_JSON = "{\"id\":\"123\",\"description\":\"test\",\"transfer_files\":[" + TRANSFER_FILE_JSON + "]}";

    private final static String TRANSFER_JSON = "{\"transfer_file_groups\":[" + TRANSFER_FILE_GROUP_JSON + "]}";

    @Test
    public void shouldDeserializeTransferFromJson() throws Exception {
        TransferFile transferFile = TransferFile.fromJson(TRANSFER_FILE_JSON);
        TransferFileGroup transferFileGroup = TransferFileGroup.builder().transferFiles(ImmutableList.of(transferFile)).id("123").description("test").build();
        Transfer expectedTransfer = new Transfer(ImmutableList.of(transferFileGroup));

        Transfer actualTransfer = Transfer.fromJson(TRANSFER_JSON);
        assertThat(actualTransfer.getTransferFileGroups()).hasSize(1).usingElementComparatorIgnoringFields("transferFiles").contains(transferFileGroup);
        assertThat(actualTransfer.getTransferFileGroups().get(0).getTransferFiles()).usingFieldByFieldElementComparator().contains(transferFile);
    }

    @Test
    public void shouldSerializeTransferToJson() throws Exception {
        TransferFile transferFile = TransferFile.fromJson(TRANSFER_FILE_JSON);
        TransferFileGroup transferFileGroup = TransferFileGroup.builder().transferFiles(ImmutableList.of(transferFile)).id("123").description("test").build();
        Transfer transfer = new Transfer(ImmutableList.of(transferFileGroup));

        assertThat(transfer.toJson()).isEqualTo(TRANSFER_JSON);
    }
}