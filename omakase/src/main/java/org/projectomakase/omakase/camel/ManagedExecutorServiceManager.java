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
package org.projectomakase.omakase.camel;

import com.google.common.collect.ImmutableList;
import org.apache.camel.CamelContext;
import org.apache.camel.impl.DefaultExecutorServiceManager;

import java.util.List;
import java.util.concurrent.ExecutorService;

/**
 * Overrides methods in {@link org.apache.camel.impl.DefaultExecutorServiceManager} that invoke {@link java.util.concurrent.ExecutorService} lifecycle methods and performs a no-op.
 * <p>
 * Managed Scheduled Executor Service instances are managed by the application server, and Java EE applications are forbidden to invoke any lifecycle related method.
 * </p>
 *
 * @author Richard Lucas
 */
public class ManagedExecutorServiceManager extends DefaultExecutorServiceManager {

    public ManagedExecutorServiceManager(CamelContext camelContext) {
        super(camelContext);
    }

    @Override
    public boolean awaitTermination(ExecutorService executorService, long shutdownAwaitTermination) throws InterruptedException {
        return true;
    }

    @Override
    public void shutdown(ExecutorService executorService) {
        //no-op
    }

    @Override
    public void shutdownGraceful(ExecutorService executorService) {
        //no-op
    }

    @Override
    public void shutdownGraceful(ExecutorService executorService, long shutdownAwaitTermination) {
        //no-op
    }

    @Override
    public List<Runnable> shutdownNow(ExecutorService executorService) {
        return ImmutableList.of();
    }

    @Override
    protected void doShutdown() throws Exception {
        //no-op
    }
}
