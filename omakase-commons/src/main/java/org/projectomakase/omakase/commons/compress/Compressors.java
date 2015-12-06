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
package org.projectomakase.omakase.commons.compress;

import org.projectomakase.omakase.commons.exceptions.OmakaseRuntimeException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.zip.Deflater;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Methods for compressing and uncompressing payloads.
 *
 * @author Richard Lucas
 */
public final class Compressors {

    private Compressors() {
    }

    /**
     * Compresses a string using the GZIP file format, and returns a base-64 encoded version of the compressed string.
     *
     * @param payload
     *         the string to compress
     * @return a base-64 encoded version of the compressed string.
     */
    public static String compressString(String payload) {
        try (ByteArrayOutputStream byteArrayOS = new ByteArrayOutputStream(); GZIPOutputStream gzipOS = new GZIPOutputStream(byteArrayOS) {{
            def.setLevel(Deflater.BEST_COMPRESSION);
        }}) {
            gzipOS.write(payload.getBytes());
            gzipOS.close();
            return new String(Base64.getEncoder().encode(byteArrayOS.toByteArray()), "UTF-8");
        } catch (IOException e) {
            throw new OmakaseRuntimeException("String compression error. " + e.getMessage(), e);
        }
    }

    /**
     * Uncompresses a base-64 encoded string that has been compressed using the GZIP file format.
     *
     * @param payload
     *         a base-64 GZIP compressed string
     * @return the decoded uncompressed string.
     */
    public static String uncompressString(String payload) {
        byte[] buffer = new byte[8192];
        int len;

        try (ByteArrayOutputStream byteArrayOS = new ByteArrayOutputStream(); ByteArrayInputStream byteArrayIS = new ByteArrayInputStream(Base64.getDecoder().decode(payload));
                GZIPInputStream gzipIS = new GZIPInputStream(byteArrayIS)) {
            while ((len = gzipIS.read(buffer)) > 0) {
                byteArrayOS.write(buffer, 0, len);
            }

            return new String(byteArrayOS.toByteArray(), "UTF-8");
        } catch (IllegalArgumentException | IOException e) {
            throw new OmakaseRuntimeException("String un-compression error. " + e.getMessage(), e);
        }
    }
}
