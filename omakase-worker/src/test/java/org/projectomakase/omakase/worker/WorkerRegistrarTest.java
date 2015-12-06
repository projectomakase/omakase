/*
 * #%L
 * omakase-worker
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
package org.projectomakase.omakase.worker;

import org.projectomakase.omakase.commons.exceptions.OmakaseRuntimeException;
import org.projectomakase.omakase.worker.rest.v1.client.OmakaseClient;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

/**
 * @author Richard Lucas
 */
public class WorkerRegistrarTest {

    private WorkerRegistrar workerRegistrar;
    private OmakaseClient omakaseClient;

    @Before
    public void setUp() throws Exception {
        omakaseClient = mock(OmakaseClient.class);
        workerRegistrar = new WorkerRegistrar();
        workerRegistrar.omakaseClient = omakaseClient;
        workerRegistrar.workerName = "test";
        workerRegistrar.retryFrequencyInMs = 5;
        workerRegistrar.maxRetries = 2;
    }

    @Test
    public void shouldRegisterSuccessfully() throws Exception {
        doReturn("worker-id").when(omakaseClient).registerWorker(anyString());
        assertThat(workerRegistrar.register()).isEqualTo("worker-id");
    }

    @Test
    public void shouldFailToRegister() throws Exception {
        Mockito.doThrow(new OmakaseRuntimeException("Test Exception")).when(omakaseClient).registerWorker(anyString());
        assertThatThrownBy(() -> workerRegistrar.register()).isExactlyInstanceOf(OmakaseRuntimeException.class).hasMessage("Failed to register with Omakase");
    }
}