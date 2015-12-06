/*
 * #%L
 * omakase-worker
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
package org.projectomakase.omakase.worker.tool.protocol.provider.http;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.google.common.base.Charsets;
import com.google.common.io.CharStreams;
import com.google.common.io.Closeables;
import org.projectomakase.omakase.commons.http.HttpClientFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.head;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Richard Lucas
 */
public class HttpProtocolHandlerTest {

    private HttpProtocolHandler httpProtocolHandler;

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(8089);

    @Before
    public void before() {
        httpProtocolHandler = new HttpProtocolHandler();
        httpProtocolHandler.httpClient = HttpClientFactory.pooledConnectionHttpClient(100, 30000, 30000);
    }

    @After
    public void after() {
        httpProtocolHandler.httpClient.getConnectionManager().shutdown();
    }

    @Test
    public void shouldOpenStream() throws Exception {
        stubFor(get(urlEqualTo("/test-file.txt")).willReturn(aResponse().withStatus(200).withHeader("Content-Type", "text/plain;charset=UTF-8").withBody("This is a test")));
        InputStreamReader reader = null;
        httpProtocolHandler.init(new URI("http://localhost:8089/test-file.txt"));
        try (InputStream inputStream = httpProtocolHandler.openStream()) {
            assertThat(inputStream).isNotNull();
            reader = new InputStreamReader(inputStream, Charsets.UTF_8);
            assertThat(CharStreams.toString(reader)).isEqualTo("This is a test");
        } finally {
            Closeables.closeQuietly(reader);
        }
    }

    @Test
    public void shouldGetContentLength() throws Exception {
        String body = "This is a test";
        stubFor(head(urlEqualTo("/test-file.txt"))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "text/plain;charset=UTF-8").withBody(body)));
        httpProtocolHandler.init(new URI("http://localhost:8089/test-file.txt"));
        assertThat(httpProtocolHandler.getContentLength()).isEqualTo(body.length());
    }

}