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

import javax.enterprise.event.Event;
import javax.enterprise.inject.Any;
import javax.inject.Inject;

/**
 * Allows the application to fire CDI a {@link javax.enterprise.event.Event} that is an instance of {@link CallbackEvent}.
 * <p>
 * The events are only consumed by Observers that are annotated with {@link CallbackListener} or {@link javax.enterprise.inject.Any}.
 * </p>
 * <p>
 * Observers can be filtered by specifying a Callback Listener Id via the {@link CallbackListener} annotation.
 * When a Callback Listener Id is provided events will are only consumed by Observers that specify the Id in the {@link CallbackListener} annotation.
 * </p>
 *
 * @author Richard Lucas
 */
public class Callback {

    @Inject
    @Any
    Event<CallbackEvent> event;

    /**
     * Fires a callback event with the {@link CallbackListener} qualifier and notifies observers.
     *
     * @param callbackEvent
     *         the callback event
     */
    public void fire(CallbackEvent callbackEvent) {
        event.select(new CallbackListener.CallbackListenerAnnotationLiteral("")).fire(callbackEvent);
    }

    /**
     * Fires a callback event with the {@link CallbackListener} qualifier and notifies observers.
     *
     * @param callbackEvent
     *         the callback event
     * @param listenerId
     *         a callback listener id, used to filter which observers are notified.
     */
    public void fire(CallbackEvent callbackEvent, String listenerId) {
        event.select(new CallbackListener.CallbackListenerAnnotationLiteral(listenerId)).fire(callbackEvent);
    }
}
