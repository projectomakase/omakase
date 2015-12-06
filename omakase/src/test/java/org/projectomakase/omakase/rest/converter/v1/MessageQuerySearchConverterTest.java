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

import org.projectomakase.omakase.job.message.MessageSearchBuilder;
import org.projectomakase.omakase.search.Operator;
import org.projectomakase.omakase.search.Search;
import org.projectomakase.omakase.search.SearchCondition;
import org.jboss.resteasy.specimpl.MultivaluedMapImpl;
import org.junit.Before;
import org.junit.Test;

import javax.ws.rs.core.MultivaluedMap;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Richard Lucas
 */
public class MessageQuerySearchConverterTest {

    private MessageQuerySearchConverter converter;

    @Before
    public void setUp() throws Exception {
        converter = new MessageQuerySearchConverter();
    }

    @Test
    public void shouldConvertQueryToSearchNoErrorCondition() throws Exception {
        MultivaluedMap<String, String> multivaluedMap = new MultivaluedMapImpl<>();
        multivaluedMap.add("timestamp[eq]", "2015-05-01");
        Search actualSearch = converter.from(multivaluedMap);
        assertThat(actualSearch.getSearchConditions()).usingFieldByFieldElementComparator().containsExactly(new SearchCondition("jcr:created", Operator.EQ, "2015-05-01", true));

        MessageSearchBuilder messageSearchBuilder = new MessageSearchBuilder();
        assertThat(actualSearch).isEqualToIgnoringGivenFields(messageSearchBuilder.build(), "searchConditions");
    }

    @Test
    public void shouldConvertQueryToSearchWithErrorConditionTrue() throws Exception {
        MultivaluedMap<String, String> multivaluedMap = new MultivaluedMapImpl<>();
        multivaluedMap.add("timestamp[eq]", "2015-05-01");
        multivaluedMap.add("error[eq]", "true");
        Search actualSearch = converter.from(multivaluedMap);
        assertThat(actualSearch.getSearchConditions()).usingFieldByFieldElementComparator()
                .containsOnly(new SearchCondition("jcr:created", Operator.EQ, "2015-05-01", true), new SearchCondition("omakase:messageType", Operator.EQ, "ERROR", false));

        MessageSearchBuilder messageSearchBuilder = new MessageSearchBuilder();
        assertThat(actualSearch).isEqualToIgnoringGivenFields(messageSearchBuilder.build(), "searchConditions");
    }

    @Test
    public void shouldConvertQueryToSearchWithErrorConditionFalse() throws Exception {
        MultivaluedMap<String, String> multivaluedMap = new MultivaluedMapImpl<>();
        multivaluedMap.add("timestamp[eq]", "2015-05-01");
        multivaluedMap.add("error[eq]", "false");
        Search actualSearch = converter.from(multivaluedMap);
        assertThat(actualSearch.getSearchConditions()).usingFieldByFieldElementComparator()
                .containsOnly(new SearchCondition("jcr:created", Operator.EQ, "2015-05-01", true), new SearchCondition("omakase:messageType", Operator.EQ, "INFO", false));

        MessageSearchBuilder messageSearchBuilder = new MessageSearchBuilder();
        assertThat(actualSearch).isEqualToIgnoringGivenFields(messageSearchBuilder.build(), "searchConditions");
    }
}