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
package org.projectomakase.omakase.job.message;

import org.projectomakase.omakase.IdGenerator;
import org.projectomakase.omakase.jcr.JcrEntity;
import org.jcrom.annotations.JcrNode;
import org.jcrom.annotations.JcrProperty;

/**
 * Message implementation that uses {@link org.jcrom.Jcrom} to serialize/deserialize to/from a JCR node.
 *
 * @author Richard Lucas
 */
@JcrNode(mixinTypes = {Message.MIX_IN})
public class Message extends JcrEntity {

    public static final String TYPE = "omakase:messageType";
    protected static final String MIX_IN = "omakase:message";
    private static final String VALUE = "omakase:messageValue";

    @JcrProperty(name = VALUE)
    private String messageValue;
    @JcrProperty(name = TYPE)
    private MessageType messageType;

    public Message() {
        // required by JCROM
    }

    public Message(String messageValue, MessageType messageType) {
        IdGenerator idGenerator = new IdGenerator();
        super.name = idGenerator.getId();
        this.messageValue = messageValue;
        this.messageType = messageType;
    }

    public String getMessageValue() {
        return messageValue;
    }

    public MessageType getMessageType() {
        return messageType;
    }

    @Override
    public String toString() {
        return "Message{" +
                "id='" + super.name + '\'' +
                ", messageValue='" + messageValue + '\'' +
                ", messageType=" + messageType +
                ", created=" + super.created +
                '}';
    }
}
