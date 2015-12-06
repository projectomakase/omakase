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

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/**
 * @author Richard Lucas
 */
public class TailCallsTest {

    @Test
    public void shouldUseTailCallOptimization() {
        try {
            assertThat(factorial(20000)).isEqualTo(0);
        } catch (StackOverflowError e) {
            fail("Should not overflow", e);
        }
    }

    private int factorial(final int number) {
        return factorialTailRec(1, number).invoke();
    }

    private TailCall<Integer> factorialTailRec(final int factorial, final int number) {
        if (number == 1) {
            return TailCalls.done(factorial);
        } else {
            return TailCalls.call(() -> factorialTailRec(factorial * number, number - 1));
        }
    }


}