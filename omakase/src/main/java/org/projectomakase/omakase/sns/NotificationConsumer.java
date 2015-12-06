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
package org.projectomakase.omakase.sns;

import org.jboss.logging.Logger;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import java.io.StringReader;
import java.util.function.Consumer;

/**
 * SNS Notification Consumer
 *
 * @author Richard Lucas
 */
public class NotificationConsumer implements Consumer<JsonObject> {

    private static final Logger LOGGER = Logger.getLogger(NotificationConsumer.class);

    private static final String ARCHIVE_RETRIEVAL = "ArchiveRetrieval";

    @Override
    public void accept(JsonObject jsonObject) {
        if (!jsonObject.containsKey("Message")) {
            throw new IllegalArgumentException("Notification JSON does not contain a 'Message' property");
        }
        JsonObject message = parseMessage(jsonObject.getString("Message"));

        if (!message.containsKey("Action")) {
            throw new IllegalArgumentException("Message JSON does not contain an 'Action' property");
        }

        messageConsumer(message.getString("Action")).accept(message);

    }

    private static JsonObject parseMessage(String message) {
        try (StringReader stringReader = new StringReader(message); JsonReader jsonReader = Json.createReader(stringReader)) {
            return jsonReader.readObject();
        }
    }

    private static Consumer<JsonObject> messageConsumer(String messageType) {

        Consumer<JsonObject> log = jsonObject -> {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(messageType + " " + jsonObject);
            }
        };

        if (ARCHIVE_RETRIEVAL.equals(messageType)) {
            return log.andThen(new ArchiveRetrievalNotificationConsumer());
        } else {
            throw new UnsupportedOperationException("Consuming " + messageType + " notifications is not supported");
        }
    }
}
