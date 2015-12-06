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

import com.google.common.collect.ImmutableList;
import com.jayway.restassured.RestAssured;
import com.jayway.restassured.builder.ResponseSpecBuilder;
import com.jayway.restassured.filter.log.RequestLoggingFilter;
import com.jayway.restassured.filter.log.ResponseLoggingFilter;
import com.jayway.restassured.response.ResponseBody;
import com.jayway.restassured.specification.ResponseSpecification;
import org.projectomakase.omakase.Archives;
import org.projectomakase.omakase.IdGenerator;
import org.projectomakase.omakase.RestIntegrationTests;
import org.projectomakase.omakase.commons.functions.Throwables;
import org.projectomakase.omakase.time.DateTimeFormatters;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static com.jayway.restassured.RestAssured.given;
import static org.projectomakase.omakase.RestIntegrationTests.badRequestResponseSpecification;
import static org.projectomakase.omakase.RestIntegrationTests.delete;
import static org.projectomakase.omakase.RestIntegrationTests.forbiddenResponseSpecification;
import static org.projectomakase.omakase.RestIntegrationTests.get;
import static org.projectomakase.omakase.RestIntegrationTests.getAssetsResourcePath;
import static org.projectomakase.omakase.RestIntegrationTests.getVariantResourcePath;
import static org.projectomakase.omakase.RestIntegrationTests.getVariantsResourcePath;
import static org.projectomakase.omakase.RestIntegrationTests.notFoundResponseSpecification;
import static org.projectomakase.omakase.RestIntegrationTests.patch;
import static org.projectomakase.omakase.RestIntegrationTests.post;
import static org.projectomakase.omakase.RestIntegrationTests.put;
import static org.projectomakase.omakase.RestIntegrationTests.requestSpecification;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.isEmptyOrNullString;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

/**
 * Variant V1 REST API Black Box Tests
 *
 * @author Richard Lucas
 */
@RunAsClient
@RunWith(Arquillian.class)
public class VariantV1RestIT {

    DateTimeFormatter dateTimeFormatter = DateTimeFormatters.getDefaultZonedDateTimeFormatter();

    @ArquillianResource
    URL baseURL;

    private String assetId;

    @Deployment
    public static WebArchive deploy() {
        return Archives.omakaseRESTITWar();
    }

    @Before
    public void before() throws Exception {
        String json = "{ \"name\" : \"test1\", \"external_ids\" : [\"id1\", \"id2\"]}";
        assetId = post(getAssetsResourcePath(baseURL), json, "editor", "password", Optional.of("v1"), Optional.empty()).header("Location").replace(getAssetsResourcePath(baseURL) + "/", "");
        RestAssured.replaceFiltersWith(ResponseLoggingFilter.responseLogger(), new RequestLoggingFilter());
    }

    @After
    public void after() throws Exception {
        RestAssured.replaceFiltersWith(ImmutableList.of());
        RestIntegrationTests.cleanup(baseURL);
    }

    @Test
    public void shouldReturnCreatedOnPostVariants() throws Exception {
        String json = "{ \"name\" : \"test1\", \"external_ids\" : [\"id1\", \"id2\"]}";
        post(getVariantsResourcePath(baseURL, assetId), json, "editor", "password", Optional.of("v1"), Optional.empty());
    }

    @Test
    public void shouldReturnForbiddenOnPostVariantsWithUnauthorizedUser() throws Exception {
        String json = "{ \"name\" : \"test1\", \"external_ids\" : [\"id1\", \"id2\"]}";
        post(getVariantsResourcePath(baseURL, assetId), json, "reader", "password", Optional.of("v1"), Optional.of(forbiddenResponseSpecification()));
    }

    @Test
    public void shouldReturnNotFoundOnPostAssetsWithUnknownAsset() throws Exception {
        String json = "{ \"name\" : \"test1\", \"external_ids\" : [\"id1\", \"id2\"]}";
        assetId = UUID.randomUUID().toString();
        post(getVariantsResourcePath(baseURL, assetId), json, "reader", "password", Optional.of("v1"), Optional.of(notFoundResponseSpecification()));
    }

    @Test
    public void shouldReturnOkOnGetVariantsWithNoResults() throws Exception {
        get(getVariantsResourcePath(baseURL, assetId), "reader", "password", Optional.of("v1"), Optional.empty());
    }

    @Test
    public void shouldReturnOkOnGetVariantsWithResults() throws Exception {
        String json = "{ \"name\" : \"test1\", \"external_ids\" : [\"id1\", \"id2\"]}";
        post(getVariantsResourcePath(baseURL, assetId), json, "editor", "password", Optional.of("v1"), Optional.empty());

        ResponseSpecBuilder builder = new ResponseSpecBuilder();
        builder.expectStatusCode(200);
        builder.expectBody("data.name", contains("test1"));

        get(getVariantsResourcePath(baseURL, assetId), "reader", "password", Optional.of("v1"), Optional.of(builder.build()));
    }

    @Test
    public void shouldReturnOkOnGetVariantsWithResultsUsingPagination() throws Exception {
        String variantsResourcePath = getVariantsResourcePath(baseURL, assetId);

        for (int i = 0; i < 30; i++) {
            String json = "{ \"name\" : \"test1\", \"external_ids\" : [\"id1\", \"id2\"]}";
            post(getVariantsResourcePath(baseURL, assetId), json, "editor", "password", Optional.of("v1"), Optional.empty());
        }

        ResponseSpecBuilder builder = new ResponseSpecBuilder();
        builder.expectStatusCode(200);
        builder.expectBody("page", equalTo(1));
        builder.expectBody("per_page", equalTo(10));
        builder.expectBody("total_pages", equalTo(3));
        builder.expectBody("total_count", equalTo(30));
        builder.expectBody("data", hasSize(10));
        builder.expectBody("links.first.href", equalTo(getVariantsResourcePath(baseURL, assetId) + "?page=1&per_page=10"));
        builder.expectBody("links.last.href", equalTo(getVariantsResourcePath(baseURL, assetId) + "?page=3&per_page=10"));
        builder.expectBody("links.next.href", equalTo(getVariantsResourcePath(baseURL, assetId) + "?page=2&per_page=10"));
        com.jayway.restassured.response.Response response = get(getVariantsResourcePath(baseURL, assetId), "reader", "password", Optional.of("v1"), Optional.of(builder.build()));
        // Validate Link Headers
        assertThat(response.headers().getList("Link")).extracting("value")
                .containsExactly("<" + getVariantsResourcePath(baseURL, assetId) + "?page=1&per_page=10>; rel=\"first\"", "<" + getVariantsResourcePath(baseURL, assetId) + "?page=3&per_page=10>; rel=\"last\"",
                        "<" + getVariantsResourcePath(baseURL, assetId) + "?page=2&per_page=10>; rel=\"next\"");

        builder = new ResponseSpecBuilder();
        builder.expectStatusCode(200);
        builder.expectBody("page", equalTo(2));
        builder.expectBody("per_page", equalTo(10));
        builder.expectBody("total_pages", equalTo(3));
        builder.expectBody("total_count", equalTo(30));
        builder.expectBody("data", hasSize(10));
        builder.expectBody("links.first.href", equalTo(getVariantsResourcePath(baseURL, assetId) + "?page=1&per_page=10"));
        builder.expectBody("links.last.href", equalTo(getVariantsResourcePath(baseURL, assetId) + "?page=3&per_page=10"));
        builder.expectBody("links.next.href", equalTo(getVariantsResourcePath(baseURL, assetId) + "?page=3&per_page=10"));
        builder.expectBody("links.prev.href", equalTo(getVariantsResourcePath(baseURL, assetId) + "?page=1&per_page=10"));
        response = get(variantsResourcePath + "?page=2&per_page=10", "reader", "password", Optional.of("v1"), Optional.of(builder.build()));
        // Validate Link Headers
        assertThat(response.headers().getList("Link")).extracting("value")
                .containsExactly("<" + getVariantsResourcePath(baseURL, assetId) + "?page=1&per_page=10>; rel=\"first\"", "<" + getVariantsResourcePath(baseURL, assetId) +
                        "?page=3&per_page=10>; rel=\"last\"",
                        "<" + getVariantsResourcePath(baseURL, assetId) + "?page=1&per_page=10>; rel=\"prev\"", "<" + getVariantsResourcePath(baseURL, assetId) + "?page=3&per_page=10>; rel=\"next\"");


        builder = new ResponseSpecBuilder();
        builder.expectStatusCode(200);
        builder.expectBody("page", equalTo(3));
        builder.expectBody("per_page", equalTo(10));
        builder.expectBody("total_pages", equalTo(3));
        builder.expectBody("total_count", equalTo(30));
        builder.expectBody("data", hasSize(10));
        builder.expectBody("links.first.href", equalTo(getVariantsResourcePath(baseURL, assetId) + "?page=1&per_page=10"));
        builder.expectBody("links.last.href", equalTo(getVariantsResourcePath(baseURL, assetId) + "?page=3&per_page=10"));
        builder.expectBody("links.prev.href", equalTo(getVariantsResourcePath(baseURL, assetId) + "?page=2&per_page=10"));
        response = get(variantsResourcePath + "?page=3&per_page=10", "reader", "password", Optional.of("v1"), Optional.of(builder.build()));
        // Validate Link Headers
        assertThat(response.headers().getList("Link")).extracting("value")
                .containsExactly("<" + getVariantsResourcePath(baseURL, assetId) + "?page=1&per_page=10>; rel=\"first\"", "<" + getVariantsResourcePath(baseURL, assetId) +
                        "?page=3&per_page=10>; rel=\"last\"",
                        "<" + getVariantsResourcePath(baseURL, assetId) + "?page=2&per_page=10>; rel=\"prev\"");

        builder = new ResponseSpecBuilder();
        builder.expectStatusCode(200);
        builder.expectBody("page", equalTo(4));
        builder.expectBody("per_page", equalTo(10));
        builder.expectBody("total_pages", equalTo(3));
        builder.expectBody("total_count", equalTo(30));
        builder.expectBody("data", hasSize(0));
        builder.expectBody("links.first.href", equalTo(getVariantsResourcePath(baseURL, assetId) + "?page=1&per_page=10"));
        builder.expectBody("links.last.href", equalTo(getVariantsResourcePath(baseURL, assetId) + "?page=3&per_page=10"));
        builder.expectBody("links.prev", nullValue());
        builder.expectBody("links.next", nullValue());
        response = get(variantsResourcePath + "?page=4&per_page=10", "reader", "password", Optional.of("v1"), Optional.of(builder.build()));
        // Validate Link Headers
        assertThat(response.headers().getList("Link")).extracting("value")
                .containsExactly("<" + getVariantsResourcePath(baseURL, assetId) + "?page=1&per_page=10>; rel=\"first\"", "<" + getVariantsResourcePath(baseURL, assetId) +
                        "?page=3&per_page=10>; rel=\"last\"");
    }

    @Test
    public void shouldReturnOkOnGetVariantsWithResultsUsingOnlyCount() throws Exception {
        for (int i = 0; i < 30; i++) {
            String json = "{ \"name\" : \"test1\", \"external_ids\" : [\"id1\", \"id2\"]}";
            post(getVariantsResourcePath(baseURL, assetId), json, "editor", "password", Optional.of("v1"), Optional.empty());
        }

        ResponseSpecBuilder responseSpecBuilder = new ResponseSpecBuilder();
        responseSpecBuilder.expectStatusCode(200);
        responseSpecBuilder.expectBody("page", nullValue());
        responseSpecBuilder.expectBody("per_page", nullValue());
        responseSpecBuilder.expectBody("total_pages", nullValue());
        responseSpecBuilder.expectBody("total_count", equalTo(30));
        responseSpecBuilder.expectBody("data", hasSize(0));
        responseSpecBuilder.expectBody("links", nullValue());
        UriBuilder uriBuilder = UriBuilder.fromUri(Throwables.returnableInstance(baseURL::toURI));
        uriBuilder.path("api/assets/{assetId}/variants");
        uriBuilder.queryParam("only_count", true);
        com.jayway.restassured.response.Response response = get(uriBuilder.build(assetId).toString(), "reader", "password", Optional.of("v1"), Optional.of(responseSpecBuilder.build()));
        // Validate Link Headers
        assertThat(response.header("Link")).isNull();
    }

    @Test
    public void shouldReturnOkOnGetVariantsDefaultSort() throws Exception {
        for (int i = 0; i < 5; i++) {
            String json = "{ \"name\" : \"test" + i + "\", \"external_ids\" : [\"id1\", \"id2\"]}";
            post(getVariantsResourcePath(baseURL, assetId), json, "editor", "password", Optional.of("v1"), Optional.empty());
        }

        ResponseSpecBuilder responseSpecBuilder = new ResponseSpecBuilder();
        responseSpecBuilder.expectStatusCode(200);
        responseSpecBuilder.expectBody("data.name", contains("test4", "test3", "test2", "test1", "test0"));
        get(getVariantsResourcePath(baseURL, assetId), "reader", "password", Optional.of("v1"), Optional.of(responseSpecBuilder.build()));
    }

    @Test
    public void shouldReturnOkOnGetVariantsSortNameAsc() throws Exception {
        for (int i = 0; i < 5; i++) {
            String json = "{ \"name\" : \"test" + i + "\", \"external_ids\" : [\"id1\", \"id2\"]}";
            post(getVariantsResourcePath(baseURL, assetId), json, "editor", "password", Optional.of("v1"), Optional.empty());
        }

        ResponseSpecBuilder responseSpecBuilder = new ResponseSpecBuilder();
        responseSpecBuilder.expectStatusCode(200);
        responseSpecBuilder.expectBody("data.name", contains("test0", "test1", "test2", "test3", "test4"));
        get(getVariantsResourcePath(baseURL, assetId) + "?sort=name&order=ASC", "reader", "password", Optional.of("v1"), Optional.of(responseSpecBuilder.build()));
    }

    @Test
    public void shouldReturnOkOnGetVariantsFilteredByNameEqual() throws Exception {
        for (int i = 0; i < 5; i++) {
            String json = "{ \"name\" : \"test" + i + "\", \"external_ids\" : [\"id1\", \"id2\"]}";
            post(getVariantsResourcePath(baseURL, assetId), json, "editor", "password", Optional.of("v1"), Optional.empty());
        }

        ResponseSpecBuilder responseSpecBuilder = new ResponseSpecBuilder();
        responseSpecBuilder.expectStatusCode(200);
        responseSpecBuilder.expectBody("data.name", contains("test0"));
        get(getVariantsResourcePath(baseURL, assetId) + "?name[eq]=test0", "reader", "password", Optional.of("v1"), Optional.of(responseSpecBuilder.build()));
    }

    @Test
    public void shouldReturnOkOnGetVariantsFilteredByNameWithSpaces() throws Exception {
        for (int i = 0; i < 5; i++) {
            String json = "{ \"name\" : \"test " + i + "\", \"external_ids\" : [\"id1\", \"id2\"]}";
            post(getVariantsResourcePath(baseURL, assetId), json, "editor", "password", Optional.of("v1"), Optional.empty());
        }

        ResponseSpecBuilder responseSpecBuilder = new ResponseSpecBuilder();
        responseSpecBuilder.expectStatusCode(200);
        responseSpecBuilder.expectBody("data.name", contains("test 0"));
        // the RestAssured framework auto escapes the space to %20
        get(getVariantsResourcePath(baseURL, assetId) + "?name[eq]=test 0", "reader", "password", Optional.of("v1"), Optional.of(responseSpecBuilder.build()));
    }

    @Test
    public void shouldReturnOkOnGetVariantsFilteredByNameLike() throws Exception {
        for (int i = 0; i < 5; i++) {
            String json = "{ \"name\" : \"test" + i + "\", \"external_ids\" : [\"id1\", \"id2\"]}";
            post(getVariantsResourcePath(baseURL, assetId), json, "editor", "password", Optional.of("v1"), Optional.empty());
        }

        ResponseSpecBuilder responseSpecBuilder = new ResponseSpecBuilder();
        responseSpecBuilder.expectStatusCode(200);
        responseSpecBuilder.expectBody("data.name", contains("test3"));
        get(getVariantsResourcePath(baseURL, assetId) + "?name[like]=3", "reader", "password", Optional.of("v1"), Optional.of(responseSpecBuilder.build()));
    }

    @Test
    public void shouldReturnBadRequestOnGetVariantsFilteredByInvalidDateFormat() throws Exception {
        ResponseSpecBuilder responseSpecBuilder = new ResponseSpecBuilder();
        responseSpecBuilder.expectStatusCode(200);
        responseSpecBuilder.expectBody("data.name", contains("test"));
        get(getVariantsResourcePath(baseURL, assetId) + "?created[eq]=3", "reader", "password", Optional.of("v1"),
                Optional.of(badRequestResponseSpecification(Optional.of("Invalid date format for attribute created"))));
    }

    @Test
    public void shouldReturnBadRequestOnGetVariantsFilteredUnsupportedOperator() throws Exception {
        ResponseSpecBuilder responseSpecBuilder = new ResponseSpecBuilder();
        responseSpecBuilder.expectStatusCode(200);
        responseSpecBuilder.expectBody("data.name", contains("test"));
        get(getVariantsResourcePath(baseURL, assetId) + "?name[ne]=bad", "reader", "password", Optional.of("v1"),
                Optional.of(badRequestResponseSpecification(Optional.of("Invalid search condition operator NE for condition attribute name"))));
    }

    @Test
    public void shouldReturnNotFoundOnGetVariantsWithUnknownAsset() throws Exception {
        assetId = UUID.randomUUID().toString();
        get(getVariantsResourcePath(baseURL, assetId), "reader", "password", Optional.of("v1"), Optional.of(notFoundResponseSpecification()));
    }

    @Test
    public void shouldReturnOkOnGetVariant() throws Exception {
        String json = "{ \"name\" : \"test1\", \"external_ids\" : [\"id1\", \"id2\"]}";
        String variantId = post(getVariantsResourcePath(baseURL, assetId), json, "editor", "password", Optional.of("v1"), Optional.empty()).header("Location").replace(getVariantsResourcePath(baseURL, assetId) +
                "/", "");
        get(getVariantResourcePath(baseURL, assetId, variantId), "reader", "password", Optional.of("v1"),
                Optional.of(getVariantOkResponseSpecification(variantId, "test1", "editor", "editor", "id1", "id2")));
    }

    @Test
    public void shouldReturnNotFoundOnGetVariant() throws Exception {
        get(getVariantResourcePath(baseURL, assetId, new IdGenerator().getId()), "reader", "password", Optional.of("v1"), Optional.of(notFoundResponseSpecification()));
    }

    @Test
    public void shouldReturnNotFoundOnGetVariantWithUnknownAsset() throws Exception {
        IdGenerator idGenerator = new IdGenerator();
        get(getVariantResourcePath(baseURL, idGenerator.getId(), idGenerator.getId()), "reader", "password", Optional.of("v1"), Optional.of(notFoundResponseSpecification()));
    }

    @Test
    public void shouldReturnNotModifiedOnGetVariantWithEtag() throws Exception {
        String json = "{ \"name\" : \"test1\", \"external_ids\" : [\"id1\", \"id2\"]}";
        String variantId = post(getVariantsResourcePath(baseURL, assetId), json, "editor", "password", Optional.of("v1"), Optional.empty()).header("Location").replace(getVariantsResourcePath(baseURL, assetId) +
                "/", "");
        com.jayway.restassured.response.Response response = get(getVariantResourcePath(baseURL, assetId, variantId), "reader", "password", Optional.of("v1"),
                Optional.of(getVariantOkResponseSpecification(variantId, "test1", "editor", "editor", "id1", "id2")));
        String etag = response.getHeader("ETag");
        given().spec(requestSpecification("reader", "password", Optional.of("v1"))).header("If-None-Match", etag).then().statusCode(Response.Status.NOT_MODIFIED.getStatusCode()).when()
                .header("ETag", notNullValue()).get(getVariantResourcePath(baseURL, assetId, variantId)).then().assertThat().body(isEmptyOrNullString());
    }

    @Test
    public void shouldReturnOkOnPutVariantWithMinimalPayload() {
        String json = "{ \"name\" : \"test1\", \"external_ids\" : [\"id1\", \"id2\"]}";
        String variantId = post(getVariantsResourcePath(baseURL, assetId), json, "editor", "password", Optional.of("v1"), Optional.empty()).header("Location").replace(getVariantsResourcePath(baseURL, assetId) +
                "/", "");

        ResponseBody before = get(getVariantResourcePath(baseURL, assetId, variantId), "reader", "password", Optional.of("v1"),
                Optional.of(getVariantOkResponseSpecification(variantId, "test1", "editor", "editor", "id1", "id2"))).body();

        String updateJson = "{ \"name\" : \"test2\", \"external_ids\" : [\"id1\", \"id2\", \"id3\"]}";
        put(getVariantResourcePath(baseURL, assetId, variantId), updateJson, "admin", "password", Optional.of("v1"), Optional.empty());

        ResponseBody after = get(getVariantResourcePath(baseURL, assetId, variantId), "reader", "password", Optional.of("v1"),
                Optional.of(getVariantOkResponseSpecification(variantId, "test2", "editor", "admin", "id1", "id2", "id3"))).body();
        checkVariantReadOnlyFieldsAreTheSame(before, after);
    }

    @Test
    public void shouldReturnOkOnPutVariantWithFullPayload() {
        String json = "{ \"name\" : \"test1\", \"external_ids\" : [\"id1\", \"id2\"]}";
        String variantId = post(getVariantsResourcePath(baseURL, assetId), json, "editor", "password", Optional.of("v1"), Optional.empty()).header("Location").replace(getVariantsResourcePath(baseURL, assetId) +
                "/", "");

        ResponseBody before = get(getVariantResourcePath(baseURL, assetId, variantId), "reader", "password", Optional.of("v1"),
                Optional.of(getVariantOkResponseSpecification(variantId, "test1", "editor", "editor", "id1", "id2"))).body();

        String updateJson = "{" +
                "    \"id\": 55277082," +
                "    \"name\": \"test2\"," +
                "    \"external_ids\": [" +
                "        \"id1\"," +
                "        \"id2\"," +
                "        \"id3\"" +
                "    ]," +
                "    \"created\": \"2013-07-09T10:33:51-07:00\"," +
                "    \"created_by\": \"bad\"," +
                "    \"last_modified\": \"2013-07-09T10:33:51-07:00\"," +
                "    \"last_modified_by\": \"bad\"" +
                "}";
        put(getVariantResourcePath(baseURL, assetId, variantId), updateJson, "admin", "password", Optional.of("v1"), Optional.empty());

        ResponseBody after = get(getVariantResourcePath(baseURL, assetId, variantId), "reader", "password", Optional.of("v1"),
                Optional.of(getVariantOkResponseSpecification(variantId, "test2", "editor", "admin", "id1", "id2", "id3"))).body();
        checkVariantReadOnlyFieldsAreTheSame(before, after);
    }

    @Test
    public void shouldReturnNotFoundOnPutVariant() {
        String updateJson = "{ \"name\" : \"test2\", \"external_ids\" : [\"id1\", \"id2\", \"id3\"]}";
        put(getVariantResourcePath(baseURL, assetId, new IdGenerator().getId()), updateJson, "admin", "password", Optional.of("v1"), Optional.of(notFoundResponseSpecification()));
    }

    @Test
    public void shouldReturnNotFoundOnPutVariantUnknownAsset() {
        String updateJson = "{ \"name\" : \"test2\", \"external_ids\" : [\"id1\", \"id2\", \"id3\"]}";
        IdGenerator idGenerator = new IdGenerator();
        put(getVariantResourcePath(baseURL, idGenerator.getId(), idGenerator.getId()), updateJson, "admin", "password", Optional.of("v1"), Optional.of(notFoundResponseSpecification()));
    }

    @Test
    public void shouldReturnForbiddenOnPutAVariant() {
        String json = "{ \"name\" : \"test1\", \"external_ids\" : [\"id1\", \"id2\"]}";
        String variantId = post(getVariantsResourcePath(baseURL, assetId), json, "editor", "password", Optional.of("v1"), Optional.empty()).header("Location").replace(getVariantsResourcePath(baseURL, assetId) +
                "/", "");
        put(getVariantResourcePath(baseURL, assetId, variantId), json, "reader", "password", Optional.of("v1"), Optional.of(forbiddenResponseSpecification()));
    }

    @Test
    public void shouldReturnOkOnPatchVariant() {
        String json = "{ \"name\" : \"test1\", \"external_ids\" : [\"id1\", \"id2\"]}";
        String variantId = post(getVariantsResourcePath(baseURL, assetId), json, "editor", "password", Optional.of("v1"), Optional.empty()).header("Location").replace(getVariantsResourcePath(baseURL, assetId) +
                "/", "");

        ResponseBody before = get(getVariantResourcePath(baseURL, assetId, variantId), "reader", "password", Optional.of("v1"),
                Optional.of(getVariantOkResponseSpecification(variantId, "test1", "editor", "editor", "id1", "id2"))).body();

        String patchJson = "[" +
                "    {" +
                "        \"op\": \"replace\"," +
                "        \"path\": \"/name\"," +
                "        \"value\": \"test2\"" +
                "    }," +
                "    {" +
                "        \"op\": \"add\"," +
                "        \"path\": \"/external_ids/-\"," +
                "        \"value\": \"id3\"" +
                "    }" +
                "]";
        patch(getVariantResourcePath(baseURL, assetId, variantId), patchJson, "admin", "password", Optional.of("v1"), Optional.empty());

        ResponseBody after = get(getVariantResourcePath(baseURL, assetId, variantId), "reader", "password", Optional.of("v1"),
                Optional.of(getVariantOkResponseSpecification(variantId, "test2", "editor", "admin", "id1", "id2", "id3"))).body();
        checkVariantReadOnlyFieldsAreTheSame(before, after);
    }

    @Test
    public void shouldIgnoreReadOnlyFieldsOnPatchVariant() {
        String json = "{ \"name\" : \"test1\", \"external_ids\" : [\"id1\", \"id2\"]}";
        String variantId = post(getVariantsResourcePath(baseURL, assetId), json, "editor", "password", Optional.of("v1"), Optional.empty()).header("Location").replace(getVariantsResourcePath(baseURL, assetId) +
                "/", "");

        ResponseBody before = get(getVariantResourcePath(baseURL, assetId, variantId), "reader", "password", Optional.of("v1"),
                Optional.of(getVariantOkResponseSpecification(variantId, "test1", "editor", "editor", "id1", "id2"))).body();

        String patchJson = "[" +
                "    {" +
                "        \"op\": \"replace\"," +
                "        \"path\": \"/id\"," +
                "        \"value\": \"1\"" +
                "    }," +
                "    {" +
                "        \"op\": \"replace\"," +
                "        \"path\": \"/created_by\"," +
                "        \"value\": \"bad\"" +
                "    }" +
                "]";
        patch(getVariantResourcePath(baseURL, assetId, variantId), patchJson, "admin", "password", Optional.of("v1"), Optional.empty());

        ResponseBody after = get(getVariantResourcePath(baseURL, assetId, variantId), "reader", "password", Optional.of("v1"),
                Optional.of(getVariantOkResponseSpecification(variantId, "test1", "editor", "admin", "id1", "id2"))).body();
        checkVariantReadOnlyFieldsAreTheSame(before, after);
    }

    @Test
    public void shouldReturnBadRequestOnPatchVariantIfExternalIdsIsReplacedWithASingleValue() {
        String json = "{ \"name\" : \"test1\", \"external_ids\" : [\"id1\", \"id2\"]}";
        String variantId = post(getVariantsResourcePath(baseURL, assetId), json, "editor", "password", Optional.of("v1"), Optional.empty()).header("Location").replace(getVariantsResourcePath(baseURL, assetId) +
                "/", "");

        String patchJson = "[" +
                "    {" +
                "        \"op\": \"replace\"," +
                "        \"path\": \"/external_ids\"," +
                "        \"value\": \"id1\"" +
                "    }" +
                "]";
        patch(getVariantResourcePath(baseURL, assetId, variantId), patchJson, "admin", "password", Optional.of("v1"), Optional.of(badRequestResponseSpecification()));
    }

    @Test
    public void shouldReturnNotFoundOnPatchVariant() {
        String patchJson = "[" +
                "    {" +
                "        \"op\": \"replace\"," +
                "        \"path\": \"/name\"," +
                "        \"value\": \"test2\"" +
                "    }" +
                "]";
        patch(getVariantResourcePath(baseURL, assetId, new IdGenerator().getId()), patchJson, "admin", "password", Optional.of("v1"), Optional.of(notFoundResponseSpecification()));
    }

    @Test
    public void shouldReturnNotFoundOnPatchVariantUnknownAsset() {
        String patchJson = "[" +
                "    {" +
                "        \"op\": \"replace\"," +
                "        \"path\": \"/name\"," +
                "        \"value\": \"test2\"" +
                "    }" +
                "]";
        IdGenerator idGenerator = new IdGenerator();
        patch(getVariantResourcePath(baseURL, idGenerator.getId(), idGenerator.getId()), patchJson, "admin", "password", Optional.of("v1"), Optional.of(notFoundResponseSpecification()));
    }

    @Test
    public void shouldReturnForbiddenOnPatchVariant() {
        String json = "{ \"name\" : \"test1\", \"external_ids\" : [\"id1\", \"id2\"]}";
        String variantId = post(getVariantsResourcePath(baseURL, assetId), json, "editor", "password", Optional.of("v1"), Optional.empty()).header("Location").replace(getVariantsResourcePath(baseURL, assetId) +
                "/", "");
        String patchJson = "[" +
                "    {" +
                "        \"op\": \"replace\"," +
                "        \"path\": \"/name\"," +
                "        \"value\": \"test2\"" +
                "    }" +
                "]";
        patch(getVariantResourcePath(baseURL, assetId, variantId), patchJson, "reader", "password", Optional.of("v1"), Optional.of(forbiddenResponseSpecification()));
    }

    @Test
    public void shouldReturnOkOnDeleteVariant() throws Exception {
        String json = "{ \"name\" : \"test1\", \"external_ids\" : [\"id1\", \"id2\"]}";
        String variantId = post(getVariantsResourcePath(baseURL, assetId), json, "editor", "password", Optional.of("v1"), Optional.empty()).header("Location").replace(getVariantsResourcePath(baseURL, assetId) +
                "/", "");
        delete(getVariantResourcePath(baseURL, assetId, variantId), "editor", "password", Optional.of("v1"), Optional.empty());
    }

    @Test
    public void shouldReturnNotFoundOnDeleteVariant() throws Exception {
        delete(getVariantResourcePath(baseURL, assetId, new IdGenerator().getId()), "editor", "password", Optional.of("v1"), Optional.of(notFoundResponseSpecification()));
    }

    @Test
    public void shouldReturnNotFoundOnDeleteVariantUnknownAsset() throws Exception {
        IdGenerator idGenerator = new IdGenerator();
        delete(getVariantResourcePath(baseURL,idGenerator.getId(), idGenerator.getId()), "editor", "password", Optional.of("v1"), Optional.of(notFoundResponseSpecification()));
    }

    @Test
    public void shouldReturnForbiddenOnDeleteVariant() throws Exception {
        String json = "{ \"name\" : \"test1\", \"external_ids\" : [\"id1\", \"id2\"]}";
        String variantId = post(getVariantsResourcePath(baseURL, assetId), json, "editor", "password", Optional.of("v1"), Optional.empty()).header("Location").replace(getVariantsResourcePath(baseURL, assetId) +
                "/", "");
        delete(getVariantResourcePath(baseURL, assetId, variantId), "reader", "password", Optional.of("v1"), Optional.of(forbiddenResponseSpecification()));
    }

    private ResponseSpecification getVariantOkResponseSpecification(String variantId, String name, String createdUsername, String modifiedUsername, String... external_ids) {
        ResponseSpecBuilder builder = new ResponseSpecBuilder();
        builder.expectStatusCode(200);
        builder.expectBody("name", equalTo(name));
        builder.expectBody("external_ids", contains(external_ids));
        builder.expectBody("created", notNullValue(String.class));
        builder.expectBody("created_by", equalTo(createdUsername));
        builder.expectBody("last_modified", notNullValue(String.class));
        builder.expectBody("last_modified_by", equalTo(modifiedUsername));
        builder.expectBody("links.self.href", equalTo(getVariantsResourcePath(baseURL, assetId) + "/" + variantId));
        builder.expectBody("links.metadata.href", equalTo(getVariantsResourcePath(baseURL, assetId) + "/" + variantId + "/metadata"));
        builder.expectBody("links.repositories.href", equalTo(getVariantsResourcePath(baseURL, assetId) + "/" + variantId + "/repositories"));
        builder.expectBody("links.files.href", equalTo(getVariantsResourcePath(baseURL, assetId) + "/" + variantId + "/files"));
        return builder.build();
    }

    private void checkVariantReadOnlyFieldsAreTheSame(ResponseBody before, ResponseBody after) {
        assertThat(before.jsonPath().getString("id")).isEqualTo(after.jsonPath().getString("id"));
        assertThat(before.jsonPath().getString("created")).isEqualTo(after.jsonPath().getString("created"));
        assertThat(before.jsonPath().getString("created_by")).isEqualTo(after.jsonPath().getString("created_by"));
        assertThat(before.jsonPath().getString("links.self.href")).isEqualTo(after.jsonPath().getString("links.self.href"));
        assertThat(before.jsonPath().getString("links.metadata.href")).isEqualTo(after.jsonPath().getString("links.metadata.href"));
        assertThat(before.jsonPath().getString("links.repositories.href")).isEqualTo(after.jsonPath().getString("links.repositories.href"));
        assertThat(before.jsonPath().getString("links.files.href")).isEqualTo(after.jsonPath().getString("links.files.href"));
    }
}
