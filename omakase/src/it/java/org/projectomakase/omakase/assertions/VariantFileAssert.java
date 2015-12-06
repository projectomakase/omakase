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
package org.projectomakase.omakase.assertions;

import org.projectomakase.omakase.content.VariantFile;
import org.projectomakase.omakase.content.VariantFileHash;
import org.projectomakase.omakase.content.VariantFileType;
import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.Assertions;

import java.util.List;

/**
 * @author Richard Lucas
 */
public class VariantFileAssert extends AbstractAssert<VariantFileAssert, VariantFile> {

    public VariantFileAssert(VariantFile actual) {
        super(actual, VariantFileAssert.class);
    }

    public VariantFileAssert hasName(String name) {
        isNotNull();
        if (!actual.getVariantFilename().equals(name)) {
            failWithMessage("Expected variant file name to be <%s> but was <%s>", name, actual.getVariantFilename());
        }
        return this;
    }

    public VariantFileAssert hasOriginalFilename(String originalFilename) {
        isNotNull();
        if (!actual.getOriginalFilename().equals(originalFilename)) {
            failWithMessage("Expected original filename to be <%s> but was <%s>", originalFilename, actual.getOriginalFilename());
        }
        return this;
    }

    public VariantFileAssert hasOriginalFilepath(String originalFilepath) {
        isNotNull();
        if (!actual.getOriginalFilepath().equals(originalFilepath)) {
            failWithMessage("Expected original filepath to be <%s> but was <%s>", originalFilepath, actual.getOriginalFilepath());
        }
        return this;
    }

    public VariantFileAssert hasSize(long size) {
        isNotNull();
        if (!actual.getSize().equals(size)) {
            failWithMessage("Expected size to be <%d> but was <%d>", size, actual.getSize());
        }
        return this;
    }

    public VariantFileAssert hasHashes(List<VariantFileHash> variantFileHashes) {
        Assertions.assertThat(variantFileHashes).usingFieldByFieldElementComparator().containsExactlyElementsOf(variantFileHashes);
        return this;
    }

    public VariantFileAssert isVirtualVariantFile() {
        if (!actual.getType().equals(VariantFileType.VIRTUAL)) {
            failWithMessage("Expected type to be VIRTUAL but was <%s>", actual.getType());
        }

        if (actual.getOriginalFilename() != null) {
            failWithMessage("Expected original filename to be null but was <%s>", actual.getOriginalFilename());
        }

        if (actual.getOriginalFilepath() != null) {
            failWithMessage("Expected original filepath to be null but was <%s>", actual.getOriginalFilepath());
        }

        if (actual.getSize() != null) {
            failWithMessage("Expected size to be null but was <%d>", actual.getSize());
        }

        if (!actual.getHashes().isEmpty()) {
            failWithMessage("Expected hashes to be empty but was <%s>", actual.getHashes());
        }
        return this;
    }
}
