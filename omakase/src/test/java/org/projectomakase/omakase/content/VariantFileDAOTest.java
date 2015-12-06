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
package org.projectomakase.omakase.content;

import org.jcrom.Jcrom;
import org.junit.Before;
import org.junit.Test;

import javax.jcr.Session;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
/**
 * @author Richard Lucas
 */
public class VariantFileDAOTest {

    private VariantFileDAO variantFileDAO;

    @Before
    public void setUp() throws Exception {
        Session session = mock(Session.class);
        Jcrom jcrom = mock(Jcrom.class);
        variantFileDAO = new VariantFileDAO(session, jcrom);
    }

    @Test
    public void shouldGetAssetIdFromVariantFile() throws Exception {
        VariantFile variantFile = new VariantFile();
        variantFile.path = "/organizations/default/assets/a7/00/5a/a7005a3b_4f0a_404a_8424_23a4fb41dab7/ef21945f_a7d1_41a3_8582_01b637f0c90e/files/16/9c/b8/169cb84b_d97b_4077_bfc1_4ea3104bc422";
        assertThat(variantFileDAO.getAssetId(variantFile)).isEqualTo("a7005a3b_4f0a_404a_8424_23a4fb41dab7");
    }

    @Test
    public void shouldGetVariantIdFromVariantFile() throws Exception {
        VariantFile variantFile = new VariantFile();
        variantFile.path = "/organizations/default/assets/a7/00/5a/a7005a3b_4f0a_404a_8424_23a4fb41dab7/ef21945f_a7d1_41a3_8582_01b637f0c90e/files/16/9c/b8/169cb84b_d97b_4077_bfc1_4ea3104bc422";
        assertThat(variantFileDAO.getVariantId(variantFile)).isEqualTo("ef21945f_a7d1_41a3_8582_01b637f0c90e");
    }
}