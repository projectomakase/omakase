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
package org.projectomakase.omakase.rest.converter.v1;

import org.projectomakase.omakase.job.message.Message;
import org.projectomakase.omakase.job.message.MessageType;
import org.projectomakase.omakase.rest.converter.RepresentationConverter;
import org.projectomakase.omakase.rest.model.v1.MessageModel;

import javax.ws.rs.core.UriInfo;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Optional;

/**
 * @author Richard Lucas
 */
public class MessageRepresentationConverter implements RepresentationConverter<MessageModel, Message> {

    @Override
    public MessageModel from(UriInfo uriInfo, Message source) {
        boolean error = Optional.of(source.getMessageType()).filter(type -> type.equals(MessageType.ERROR)).map(type -> true).orElse(false);
        ZonedDateTime timestamp = Optional.ofNullable(source.getCreated()).map(created -> ZonedDateTime.ofInstant(created.toInstant(), ZoneId.systemDefault())).orElse(null);
        return new MessageModel(source.getId(), source.getMessageValue(), timestamp, error);
    }

    @Override
    public Message to(UriInfo uriInfo, MessageModel representation) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Message update(UriInfo uriInfo, MessageModel representation, Message target) {
        throw new UnsupportedOperationException();
    }
}
