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
package org.projectomakase.omakase;

import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import java.net.URI;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

/**
 * @author Richard Lucas
 */
public class Tests {

    private Tests() {
        // hides implicit constructor
    }

    public static UriInfo mockUriInfo(UriBuilder mockUriBuilder) {
        UriInfo uriInfo = mock(UriInfo.class);
        doReturn(mockUriBuilder).when(uriInfo).getBaseUriBuilder();
        doReturn(mockUriBuilder).when(uriInfo).getAbsolutePathBuilder();
        return uriInfo;
    }

    public static UriBuilder mockUriBuilder(URI result) {
        UriBuilder uriBuilder = mock(UriBuilder.class);
        doReturn(uriBuilder).when(uriBuilder).path(anyString());
        doReturn(result).when(uriBuilder).build();
        return uriBuilder;
    }
}
