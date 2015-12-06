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
package org.projectomakase.omakase.rest;

import com.google.common.base.CharMatcher;
import com.google.common.base.Charsets;
import com.google.common.base.Strings;
import com.google.common.io.CharStreams;
import org.projectomakase.omakase.commons.exceptions.OmakaseRuntimeException;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Methods for working with JSON strings.
 *
 * @author Richard Lucas
 */
public final class JsonStrings {

    private JsonStrings() {
        // hides default constructor
    }

    public static String inputStreamToString(InputStream inputStream) {
        try (InputStreamReader reader = new InputStreamReader(inputStream, Charsets.UTF_8)) {
            return CharStreams.toString(reader);
        } catch (IOException e) {
            throw new OmakaseRuntimeException(e.getMessage(), e);
        }
    }

    public static void isNotNullOrEmpty(String json) {
        if (Strings.isNullOrEmpty(json)) {
            throw new WebApplicationException("JSON payload required", Response.status(Response.Status.BAD_REQUEST).build());
        } else if ("{}".equals(CharMatcher.WHITESPACE.removeFrom(json))) {
            throw new WebApplicationException("JSON payload must not be empty", Response.status(Response.Status.BAD_REQUEST).build());
        }
    }
}
