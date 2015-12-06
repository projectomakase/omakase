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
package org.projectomakase.omakase.commons.functions;

import org.projectomakase.omakase.commons.exceptions.OmakaseRuntimeException;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.fail;

/**
 * @author Richard Lucas
 */
public class ThrowablesTest {

    private Throwables.VoidInstance voidInstance = () -> {
        throw new Exception("test");
    };

    private Throwables.ReturnableInstance<String> returnableInstance = () -> {
        throw new Exception("test");
    };

    @Test
    public void shouldApplyVoidInstance() {
        try {
            Throwables.voidInstance(() -> System.out.println("testing"));
        } catch (OmakaseRuntimeException e) {
            fail("Exception should not have been thrown");
        }
    }

    @Test
    public void shouldApplyReturnableInstance() {
        assertThat(Throwables.returnableInstance(() -> "testing")).isEqualTo("testing");
    }

    @Test
    public void shouldWrapVoidInstanceInOmakaseRuntimeException() {
        assertThatThrownBy(() -> Throwables.voidInstance(voidInstance)).isExactlyInstanceOf(OmakaseRuntimeException.class);
    }

    @Test
    public void shouldWrapVoidInstanceInIllegalArgumentException() {
        assertThatThrownBy(() -> Throwables.voidInstance(voidInstance, IllegalArgumentException::new)).isExactlyInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void shouldWrapReturnableInstanceInOmakaseRuntimeException() {
        assertThatThrownBy(() -> Throwables.returnableInstance(returnableInstance)).isExactlyInstanceOf(OmakaseRuntimeException.class);
    }

    @Test
    public void shouldWrapReturnableInstanceInIllegalArgumentException() {
        assertThatThrownBy(() -> Throwables.returnableInstance(returnableInstance, IllegalArgumentException::new)).isExactlyInstanceOf(IllegalArgumentException.class);
    }

}