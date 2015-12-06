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
package org.projectomakase.omakase.commons.file;

import com.google.common.base.Charsets;
import com.google.common.io.Files;
import org.projectomakase.omakase.commons.functions.Throwables;

import java.io.File;
import java.util.UUID;

/**
 * @author Richard Lucas
 */
public final class FileGenerator {

    private static final String FILE_SEED =
            "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Pellentesque imperdiet pretium massa at egestas. Class aptent taciti sociosqu ad litora torquent per conubia nostra, " +
                    "per inceptos himenaeos. Donec ac metus nibh. Aenean varius arcu sed erat ullamcorper, non pulvinar eros aliquam. Morbi laoreet sit amet ligula nec cursus. Mauris eu placerat " +
                    "ex, congue ultricies quam. Maecenas id sapien sem. Nam eget ullamcorper diam. Fusce in dui consectetur, tempus diam quis, ornare quam. Nullam in massa ac erat fermentum " +
                    "suscipit in a neque. Donec eu sagittis ante. Aliquam non odio tortor. Interdum et malesuada fames ac ante ipsum primis in faucibus. Vestibulum accumsan nulla ac ipsum " +
                    "pretium vestibulum. Vivamus posuere leo sit amet luctus convallis. Phasellus et risus arcu.";

    private FileGenerator() {
        // hides default constructor
    }

    public static File generate(File directory, long sizeInBytes) {
        File file = new File(directory, UUID.randomUUID().toString() + ".bin");
        while (file.length() < sizeInBytes) {
            Throwables.voidInstance(() -> Files.append(FILE_SEED, file, Charsets.UTF_8));
        }
        return file;
    }
}
