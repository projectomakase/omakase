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

import org.apache.http.entity.InputStreamEntity;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * An {@link InputStreamEntity} implementation that does NOT close the supplied input stream after writing to the output stream.
 *
 * @author Richard Lucas
 */
public class NonClosingInputStreamEntity extends InputStreamEntity {

    private static final int BUFFER_SIZE = 2048;

    public NonClosingInputStreamEntity(final InputStream inputStream, long length) {
        super(inputStream, length);

    }

    @Override
    public void writeTo(final OutputStream outputStream) throws IOException {
        if (outputStream == null) {
            throw new IllegalArgumentException("Output stream may not be null");
        }
            byte[] buffer = new byte[BUFFER_SIZE];
            int l;
            if (getContentLength() < 0) {
                // consume until EOF
                while ((l = getContent().read(buffer)) != -1) {
                    outputStream.write(buffer, 0, l);
                }
            } else {
                // consume no more than length
                long remaining = getContentLength();
                while (remaining > 0) {
                    l = getContent().read(buffer, 0, (int) Math.min(BUFFER_SIZE, remaining));
                    if (l == -1) {
                        break;
                    }
                    outputStream.write(buffer, 0, l);
                    remaining -= l;
                }
            }
    }

    @SuppressWarnings("deprecation")
    @Deprecated
    @Override
    public void consumeContent() throws IOException {
        // no-op
    }
}
