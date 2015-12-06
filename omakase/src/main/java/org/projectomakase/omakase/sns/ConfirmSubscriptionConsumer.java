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

import org.projectomakase.omakase.commons.exceptions.OmakaseRuntimeException;
import org.projectomakase.omakase.commons.functions.Throwables;
import org.jboss.logging.Logger;

import javax.json.JsonObject;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Scanner;
import java.util.function.Consumer;

/**
 * SNS SubscriptionConfirmation Message Consumer.
 * <p>
 * Calls the SubscribeURL from the SubscriptionConfirmation message in order to confirm the subscription.
 * </p>
 *
 * @author Richard Lucas
 */
public class ConfirmSubscriptionConsumer implements Consumer<JsonObject> {

    private static final Logger LOGGER = Logger.getLogger(ConfirmSubscriptionConsumer.class);

    @Override
    public void accept(JsonObject jsonObject) {
        URL conformationUrl = Throwables.returnableInstance(() -> new URL(jsonObject.getString("SubscribeURL")));
        try (InputStream inputStream = conformationUrl.openStream(); Scanner scanner = new Scanner(inputStream, "UTF-8")) {
            StringBuilder stringBuilder = new StringBuilder();
            scanner.forEachRemaining(stringBuilder::append);
            LOGGER.info("Confirmed subscription " + stringBuilder.toString());
        } catch (IOException e) {
            throw new OmakaseRuntimeException("Failed to confirm SNS subscription", e);
        }
    }
}
