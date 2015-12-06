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
package org.projectomakase.omakase.callback;

import com.google.common.collect.ImmutableMultimap;

/**
 * The event fired by {@link Callback} to any {@link CallbackListener} observers that are interested in the event.
 *
 * @author Richard Lucas
 */
public class CallbackEvent {

    private final String objectId;
    private final ImmutableMultimap<String, String> properties;

    public CallbackEvent(String objectId, ImmutableMultimap<String, String> properties) {
        this.objectId = objectId;
        this.properties = properties;
    }

    /**
     * Returns the id of the object waiting for the the callback event.
     *
     * @return id of the object waiting for the the callback event.
     */
    public String getObjectId() {
        return objectId;
    }

    /**
     * Returns the callback event properties
     *
     * @return the callback event properties
     */
    public ImmutableMultimap<String, String> getProperties() {
        return properties;
    }

    @Override
    public String toString() {
        return "CallbackEvent{" +
                "objectId='" + objectId + '\'' +
                ", properties=" + properties +
                '}';
    }
}
