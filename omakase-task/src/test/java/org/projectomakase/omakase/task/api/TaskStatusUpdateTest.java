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

import com.google.common.collect.ImmutableList;
import org.projectomakase.omakase.commons.hash.Hash;
import org.projectomakase.omakase.task.providers.transfer.ContentInfo;
import org.projectomakase.omakase.task.providers.transfer.TransferTaskOutput;
import org.junit.Test;

import java.net.URI;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
/**
 * @author Richard Lucas
 */
public class TaskStatusUpdateTest {

    @Test
    public void shouldSerializeToJsonRequired() throws Exception {
        TaskStatusUpdate statusUpdate = new TaskStatusUpdate(TaskStatus.COMPLETED, null, -1, null);
        assertThat(statusUpdate.toJson()).isEqualTo("{\"status\":\"COMPLETED\"}");
    }

    @Test
    public void shouldSerializeToJsonWithMessage() throws Exception {
        TaskStatusUpdate statusUpdate = new TaskStatusUpdate(TaskStatus.COMPLETED, "testing", -1, null);
        assertThat(statusUpdate.toJson()).isEqualTo("{\"status\":\"COMPLETED\",\"message\":\"testing\"}");
    }

    @Test
    public void shouldSerializeToJsonWithMessageAndOutput() throws Exception {
        TaskStatusUpdate statusUpdate = new TaskStatusUpdate(TaskStatus.COMPLETED, "testing", -1, new TransferTaskOutput(ImmutableList.of(
                new ContentInfo(new URI("file:/test.txt"), 1024, Collections.singletonList(new Hash("MD5", "123"))))));
        assertThat(statusUpdate.toJson()).isEqualTo("{\"status\":\"COMPLETED\",\"message\":\"testing\",\"output\":{\"content_info\":[{\"source\":\"file:/test.txt\",\"size\":1024,\"hashes\":[{\"hash_algorithm\":\"MD5\",\"hash\":\"123\"}]}]}}");
    }

    @Test
    public void shouldSerializeToJsonAll() throws Exception {
        TaskStatusUpdate statusUpdate = new TaskStatusUpdate(TaskStatus.COMPLETED, "testing", 100, new TransferTaskOutput(ImmutableList.of(
                new ContentInfo(new URI("file:/test.txt"), 1024, Collections.singletonList(new Hash("MD5", "123"))))));
        assertThat(statusUpdate.toJson()).isEqualTo("{\"status\":\"COMPLETED\",\"message\":\"testing\",\"percent_complete\":100,\"output\":{\"content_info\":[{\"source\":\"file:/test.txt\",\"size\":1024,\"hashes\":[{\"hash_algorithm\":\"MD5\",\"hash\":\"123\"}]}]}}");
    }
}