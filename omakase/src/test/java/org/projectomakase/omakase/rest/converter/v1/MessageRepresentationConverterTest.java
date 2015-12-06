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
import org.projectomakase.omakase.rest.model.v1.MessageModel;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author Richard Lucas
 */
public class MessageRepresentationConverterTest {

    private MessageRepresentationConverter converter;

    @Before
    public void setUp() throws Exception {
        converter = new MessageRepresentationConverter();
    }

    @Test
    public void shouldConvertFromErrorMessageToModel() throws Exception {
        Message message = new Message("test", MessageType.ERROR);
        assertThat(converter.from(null, message)).isEqualToComparingFieldByField(new MessageModel(message.getId(), message.getMessageValue(), null, true));
    }

    @Test
    public void shouldConvertFromInfoMessageToModel() throws Exception {
        Message message = new Message("test", MessageType.INFO);
        assertThat(converter.from(null, message)).isEqualToComparingFieldByField(new MessageModel(message.getId(), message.getMessageValue(), null, false));
    }

    @Test
    public void shouldThrowUnsupportedOperationExceptionTo() throws Exception {
        assertThatThrownBy(() -> converter.to(null, new MessageModel())).isExactlyInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    public void shouldThrowUnsupportedOperationExceptionFrom() throws Exception {
        assertThatThrownBy(() -> converter.update(null, new MessageModel(), new Message())).isExactlyInstanceOf(UnsupportedOperationException.class);
    }
}