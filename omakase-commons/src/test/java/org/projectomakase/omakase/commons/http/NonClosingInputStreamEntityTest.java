/*
 * #%L
 * omakase-commons
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
package org.projectomakase.omakase.commons.http;

import com.google.common.base.Charsets;
import com.google.common.io.ByteStreams;
import org.junit.Test;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/**
 * @author Richard Lucas
 */
public class NonClosingInputStreamEntityTest {

    @Test
    public void shouldWriteToOutputStream() throws Exception{
        String test = "this is a test";
        try (InputStream inputStream = new ByteArrayInputStream(test.getBytes(Charsets.UTF_8)); ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            NonClosingInputStreamEntity entity = new NonClosingInputStreamEntity(inputStream, test.length());
            entity.writeTo(outputStream);
            assertThat(outputStream.toString(Charsets.UTF_8.name())).isEqualTo("this is a test");
        }
    }

    @Test
    public void shouldNotCloseInputStream() throws Exception{
        String test = "this is a test";
        InputStream inputStream = new BufferedInputStream(new ByteArrayInputStream(test.getBytes(Charsets.UTF_8)));
        try (OutputStream outputStream = ByteStreams.nullOutputStream()) {
            inputStream.mark(test.length());
            NonClosingInputStreamEntity entity = new NonClosingInputStreamEntity(inputStream, test.length());
            entity.writeTo(outputStream);
        }

        try {
            inputStream.reset();
        } catch (IOException e) {
            fail("The stream should still be open", e);
        }
    }

}