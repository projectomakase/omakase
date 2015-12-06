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
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.util.function.Consumer;

/**
 * Consumes SNS Notifications.
 *
 * @author Richard Lucas
 */
@WebServlet(urlPatterns = {"/callback/sns"})
public class SNSServlet extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(SNSServlet.class);

    private static final long serialVersionUID = 1L;

    private static final String MESSAGE_TYPE = "x-amz-sns-message-type";
    private static final String SUBSCRIBE = "SubscriptionConfirmation";
    private static final String NOTIFICATION = "Notification";

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String messageType = request.getHeader(MESSAGE_TYPE);
        JsonObject message = parseMessage(request.getInputStream());
        messageConsumer(messageType).accept(message);
        response.setStatus(HttpServletResponse.SC_OK);
    }

    private static JsonObject parseMessage(InputStream rawMessage) {
        try (JsonReader jsonReader = Json.createReader(rawMessage)) {
            return jsonReader.readObject();
        }
    }

    private static Consumer<JsonObject> messageConsumer(String messageType) {

        Consumer<JsonObject> log = jsonObject -> {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(messageType + " " + jsonObject);
            }
        };

        switch (messageType) {
            case SUBSCRIBE:
                return log.andThen(new ConfirmSubscriptionConsumer());
            case NOTIFICATION:
                return log.andThen(new NotificationConsumer());
            default:
                return log;
        }
    }
}
