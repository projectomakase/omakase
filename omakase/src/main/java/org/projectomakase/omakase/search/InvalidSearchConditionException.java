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
package org.projectomakase.omakase.search;

/**
 * Runtime exception thrown if a Search Condition is not valid.
 *
 * @author Richard Lucas
 */
public class InvalidSearchConditionException extends RuntimeException {

    private final String invalidAttribute;
    private final String originalMessage;

    public InvalidSearchConditionException(String message, String invalidAttribute) {
        super(message);
        this.originalMessage = message;
        this.invalidAttribute = invalidAttribute;
    }

    public InvalidSearchConditionException(String message, String invalidAttribute, Throwable cause) {
        super(message, cause);
        this.originalMessage = message;
        this.invalidAttribute = invalidAttribute;
    }

    public String getInvalidAttribute() {
        return invalidAttribute;
    }

    @Override
    public String getMessage() {
        return super.getMessage() + " " + invalidAttribute;
    }

    public String getOriginalMessage() {
        return originalMessage;
    }
}
