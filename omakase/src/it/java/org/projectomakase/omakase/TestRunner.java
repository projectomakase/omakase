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
package org.projectomakase.omakase;

import org.projectomakase.omakase.commons.exceptions.OmakaseRuntimeException;
import org.projectomakase.omakase.security.OmakaseSecurity;

/**
 * Provides methods for running blocks of test code.
 *
 * @author Richard Lucas
 */
public final class TestRunner {

    private TestRunner() {
        // hides the implicit public constructor
    }

    /**
     * Interface implemented by the block of test code that is being run.
     */
    @FunctionalInterface
    public interface Test {

        /**
         * Runs the block of test code.
         */
        void run();
    }

    /**
     * Runs the specified Test implementation in a PrivilegedAction and executes it within the context of a a Subject created using the provided credentials.
     *
     * @param username
     *         the username
     * @param password
     *         the password
     * @param test
     *         the Test implementation
     */
    public static void runAsUser(String username, String password, Test test) {
        try {
            OmakaseSecurity.doAs(username, password, () -> {
                test.run();
                return null;
            });
        } catch (Exception e) {
            throw new OmakaseRuntimeException(e.getMessage(), e);
        }

    }
}
