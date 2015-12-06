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
package org.projectomakase.omakase.commons.collectors;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.assertj.core.api.Assertions;
import org.junit.Test;

import javax.json.Json;
import javax.json.JsonObject;
import java.util.stream.Stream;

import static org.projectomakase.omakase.commons.collectors.ImmutableSetCollector.toImmutableSet;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Richard Lucas
 */
public class CollectorsTest {

    @Test
    public void shouldCollectObjectsIntoImmutableList() {
        Assertions.assertThat(Stream.of("1", "2", "3").collect(ImmutableListCollector.toImmutableList())).isInstanceOf(ImmutableList.class).containsExactly("1", "2", "3");
    }

    @Test
    public void shouldCollectImmutableListsIntoImmutableList() {
        assertThat(Stream.of(ImmutableList.of("1", "2", "3"), ImmutableList.of("4", "5", "6")).collect(ImmutableListsCollector.toImmutableList())).isInstanceOf(ImmutableList.class)
                .containsExactly("1", "2", "3", "4", "5", "6");
    }

    @Test
    public void shouldCollectObjectsIntoImmutableSet() {
        assertThat(Stream.of("1", "2", "3").collect(toImmutableSet())).isInstanceOf(ImmutableSet.class).containsExactly("1", "2", "3");
    }

    @Test
    public void shouldCollectJsonObjectsIntoJsonArray() {
        String expected = "[{\"test\":\"1\"},{\"test\":\"2\"},{\"test\":\"3\"}]";
        assertThat(ImmutableList.of("1", "2", "3").stream().map(this::jsonObject).collect(JsonArrayCollector.toJsonArray()).toString()).isEqualTo(expected);
    }

    @Test
    public void shouldCollectJsonObjectsIntoJsonArrayParallel() {
        String expected = "[{\"test\":\"1\"},{\"test\":\"2\"},{\"test\":\"3\"}]";
        assertThat(ImmutableList.of("1", "2", "3").parallelStream().map(this::jsonObject).collect(JsonArrayCollector.toJsonArray()).toString()).isEqualTo(expected);
    }

    private JsonObject jsonObject(String value) {
        return Json.createObjectBuilder().add("test", value).build();
    }
}
