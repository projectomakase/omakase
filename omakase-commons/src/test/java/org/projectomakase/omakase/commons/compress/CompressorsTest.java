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
import org.jboss.logging.Logger;
import org.junit.Test;

import java.util.Base64;
import java.util.zip.ZipException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author Richard Lucas
 */
public class CompressorsTest {

    private static final Logger LOGGER = Logger.getLogger(CompressorsTest.class);

    private static final String RAW_STRING =
            "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Pellentesque imperdiet pretium massa at egestas. Class aptent taciti sociosqu ad litora torquent per conubia nostra, " +
                    "per inceptos himenaeos. Donec ac metus nibh. Aenean varius arcu sed erat ullamcorper, non pulvinar eros aliquam. Morbi laoreet sit amet ligula nec cursus. Mauris eu placerat " +
                    "ex, congue ultricies quam. Maecenas id sapien sem. Nam eget ullamcorper diam. Fusce in dui consectetur, tempus diam quis, ornare quam. Nullam in massa ac erat fermentum " +
                    "suscipit in a neque. Donec eu sagittis ante. Aliquam non odio tortor. Interdum et malesuada fames ac ante ipsum primis in faucibus. Vestibulum accumsan nulla ac ipsum " +
                    "pretium vestibulum. Vivamus posuere leo sit amet luctus convallis. Phasellus et risus arcu.";

    @Test
    public void shouldCompressAndUncompressString() throws Exception {
        LOGGER.debug("raw string size " + RAW_STRING.length());
        String compressedString = Compressors.compressString(RAW_STRING);
        LOGGER.debug("compressed string size " + compressedString.length());
        assertThat(compressedString.length()).isLessThan(RAW_STRING.length());
        String uncompressedString = Compressors.uncompressString(compressedString);
        assertThat(uncompressedString).isEqualTo(RAW_STRING);
    }

    @Test
    public void shouldThrowExceptionWhenUncompressingNonBase64String() throws Exception {
        assertThatThrownBy(() -> Compressors.uncompressString(RAW_STRING))
                .isExactlyInstanceOf(OmakaseRuntimeException.class)
                .hasCauseExactlyInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("String un-compression error. Illegal base64 character");
    }

    @Test
    public void shouldThrowExceptionWhenUncompressingNonGZIPString() throws Exception {
        String base64String = new String(Base64.getEncoder().encode(RAW_STRING.getBytes()), "UTF-8");
        assertThatThrownBy(() -> Compressors.uncompressString(base64String))
                .isExactlyInstanceOf(OmakaseRuntimeException.class)
                .hasCauseExactlyInstanceOf(ZipException.class)
                .hasMessageContaining("String un-compression error. Not in GZIP format");
    }
}

