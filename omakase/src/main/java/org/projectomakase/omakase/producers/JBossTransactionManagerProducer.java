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
package org.projectomakase.omakase.producers;

import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.jta.JtaTransactionManager;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Named;

/**
 * CDI Producer for a {@link JtaTransactionManager} configured to use the application containers transaction services.
 *
 * @author Richard Lucas
 */
public class JBossTransactionManagerProducer {

    private static final String JBOSS_USER_TRANSACTION = "java:jboss/UserTransaction";
    private static final String JBOSS_TRANSACTION_MANAGER = "java:jboss/TransactionManager";
    private static final String JBOSS_TRANSACTION_SYNC_REG = "java:jboss/TransactionSynchronizationRegistry";

    @Produces
    @ApplicationScoped
    @Named("jbossTransactionManager")
    public PlatformTransactionManager createJbossJtaTransactionManager() {
        JtaTransactionManager transactionManager;
        transactionManager = new JtaTransactionManager();
        transactionManager.setUserTransactionName(JBOSS_USER_TRANSACTION);
        transactionManager.setTransactionManagerName(JBOSS_TRANSACTION_MANAGER);
        transactionManager.setTransactionSynchronizationRegistryName(JBOSS_TRANSACTION_SYNC_REG);
        transactionManager.afterPropertiesSet();
        return transactionManager;
    }
}
