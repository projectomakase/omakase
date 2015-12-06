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
import org.projectomakase.omakase.RestIntegrationTests;
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
import java.net.URL;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static com.jayway.restassured.RestAssured.given;
import static org.projectomakase.omakase.RestIntegrationTests.badRequestResponseSpecification;
import static org.projectomakase.omakase.RestIntegrationTests.forbiddenResponseSpecification;
import static org.projectomakase.omakase.RestIntegrationTests.post;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.isEmptyOrNullString;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

/**
 * Asset V1 REST API Black Box Tests
 *
 * @author Richard Lucas
 */
@RunAsClient
@RunWith(Arquillian.class)
public class AssetV1RestIT {

    @ArquillianResource
    URL baseURL;

    @Deployment
    public static WebArchive deploy() {
        return Archives.omakaseRESTITWar();
    }

    @Before
    public void before() throws Exception {
        RestAssured.replaceFiltersWith(ResponseLoggingFilter.responseLogger(), new RequestLoggingFilter());
    }

    @After
    public void after() throws Exception {
        RestAssured.replaceFiltersWith(ImmutableList.of());
        RestIntegrationTests.cleanup(baseURL);
    }

    @Test
    public void shouldReturnCreatedOnPostAssets() throws Exception {
        String json = "{ \"name\" : \"test1\", \"external_ids\" : [\"id1\", \"id2\"]}";
        RestIntegrationTests.post(RestIntegrationTests.getAssetsResourcePath(baseURL), json, "editor", "password", Optional.of("v1"), Optional.empty());
    }

    @Test
    public void shouldReturnForbiddenOnPostAssetsWithUnauthorizedUser() throws Exception {
        String json = "{ \"name\" : \"test1\", \"external_ids\" : [\"id1\", \"id2\"]}";
        RestIntegrationTests.post(RestIntegrationTests.getAssetsResourcePath(baseURL), json, "reader", "password", Optional.of("v1"), Optional.of(RestIntegrationTests.forbiddenResponseSpecification()));
    }

    @Test
    public void shouldReturnBadRequestOnPostAssetsWithEmptyBody() throws Exception {
        RestIntegrationTests.post(RestIntegrationTests.getAssetsResourcePath(baseURL), "", "reader", "password", Optional.of("v1"), Optional.of(RestIntegrationTests.badRequestResponseSpecification()));
    }

    @Test
    public void shouldReturnOkOnGetAssetsWithNoResults() throws Exception {
        RestIntegrationTests.get(RestIntegrationTests.getAssetsResourcePath(baseURL), "reader", "password", Optional.of("v1"), Optional.empty());
    }

    @Test
    public void shouldReturnOkOnGetAssetsWithResults() throws Exception {
        String assetResourcePath = RestIntegrationTests.getAssetsResourcePath(baseURL);

        String json = "{ \"name\" : \"test1\", \"external_ids\" : [\"id1\", \"id2\"]}";
        RestIntegrationTests.post(assetResourcePath, json, "editor", "password", Optional.of("v1"), Optional.empty());

        ResponseSpecBuilder builder = new ResponseSpecBuilder();
        builder.expectStatusCode(200);
        builder.expectBody("data.name", contains("test1"));

        RestIntegrationTests.get(assetResourcePath, "reader", "password", Optional.of("v1"), Optional.of(builder.build()));
    }

    @Test
    public void shouldReturnOkOnGetAssetsWithResultsUsingPagination() throws Exception {
        String assetResourcePath = RestIntegrationTests.getAssetsResourcePath(baseURL);

        for (int i = 0; i < 30; i++) {
            String json = "{ \"name\" : \"test1\", \"external_ids\" : [\"id1\", \"id2\"]}";
            RestIntegrationTests.post(assetResourcePath, json, "editor", "password", Optional.of("v1"), Optional.empty());
        }

        ResponseSpecBuilder builder = new ResponseSpecBuilder();
        builder.expectStatusCode(200);
        builder.expectBody("page", equalTo(1));
        builder.expectBody("per_page", equalTo(10));
        builder.expectBody("total_pages", equalTo(3));
        builder.expectBody("total_count", equalTo(30));
        builder.expectBody("data", hasSize(10));
        builder.expectBody("links.first.href", equalTo(assetResourcePath + "?page=1&per_page=10"));
        builder.expectBody("links.last.href", equalTo(assetResourcePath + "?page=3&per_page=10"));
        builder.expectBody("links.next.href", equalTo(assetResourcePath + "?page=2&per_page=10"));
        com.jayway.restassured.response.Response response = RestIntegrationTests.get(assetResourcePath, "reader", "password", Optional.of("v1"), Optional.of(builder.build()));
        // Validate Link Headers
        assertThat(response.headers().getList("Link")).extracting("value")
                .containsExactly("<" + assetResourcePath + "?page=1&per_page=10>; rel=\"first\"", "<" + assetResourcePath + "?page=3&per_page=10>; rel=\"last\"",
                        "<" + assetResourcePath + "?page=2&per_page=10>; rel=\"next\"");

        builder = new ResponseSpecBuilder();
        builder.expectStatusCode(200);
        builder.expectBody("page", equalTo(2));
        builder.expectBody("per_page", equalTo(10));
        builder.expectBody("total_pages", equalTo(3));
        builder.expectBody("total_count", equalTo(30));
        builder.expectBody("data", hasSize(10));
        builder.expectBody("links.first.href", equalTo(assetResourcePath + "?page=1&per_page=10"));
        builder.expectBody("links.last.href", equalTo(assetResourcePath + "?page=3&per_page=10"));
        builder.expectBody("links.next.href", equalTo(assetResourcePath + "?page=3&per_page=10"));
        builder.expectBody("links.prev.href", equalTo(assetResourcePath + "?page=1&per_page=10"));
        response = RestIntegrationTests.get(assetResourcePath + "?page=2&per_page=10", "reader", "password", Optional.of("v1"), Optional.of(builder.build()));
        // Validate Link Headers
        assertThat(response.headers().getList("Link")).extracting("value")
                .containsExactly("<" + assetResourcePath + "?page=1&per_page=10>; rel=\"first\"", "<" + assetResourcePath + "?page=3&per_page=10>; rel=\"last\"",
                        "<" + assetResourcePath + "?page=1&per_page=10>; rel=\"prev\"", "<" + assetResourcePath + "?page=3&per_page=10>; rel=\"next\"");


        builder = new ResponseSpecBuilder();
        builder.expectStatusCode(200);
        builder.expectBody("page", equalTo(3));
        builder.expectBody("per_page", equalTo(10));
        builder.expectBody("total_pages", equalTo(3));
        builder.expectBody("total_count", equalTo(30));
        builder.expectBody("data", hasSize(10));
        builder.expectBody("links.first.href", equalTo(assetResourcePath + "?page=1&per_page=10"));
        builder.expectBody("links.last.href", equalTo(assetResourcePath + "?page=3&per_page=10"));
        builder.expectBody("links.prev.href", equalTo(assetResourcePath + "?page=2&per_page=10"));
        response = RestIntegrationTests.get(assetResourcePath + "?page=3&per_page=10", "reader", "password", Optional.of("v1"), Optional.of(builder.build()));
        // Validate Link Headers
        assertThat(response.headers().getList("Link")).extracting("value")
                .containsExactly("<" + assetResourcePath + "?page=1&per_page=10>; rel=\"first\"", "<" + assetResourcePath + "?page=3&per_page=10>; rel=\"last\"",
                        "<" + assetResourcePath + "?page=2&per_page=10>; rel=\"prev\"");

        builder = new ResponseSpecBuilder();
        builder.expectStatusCode(200);
        builder.expectBody("page", equalTo(4));
        builder.expectBody("per_page", equalTo(10));
        builder.expectBody("total_pages", equalTo(3));
        builder.expectBody("total_count", equalTo(30));
        builder.expectBody("data", hasSize(0));
        builder.expectBody("links.first.href", equalTo(assetResourcePath + "?page=1&per_page=10"));
        builder.expectBody("links.prev", nullValue());
        builder.expectBody("links.next", nullValue());
        response = RestIntegrationTests.get(assetResourcePath + "?page=4&per_page=10", "reader", "password", Optional.of("v1"), Optional.of(builder.build()));
        // Validate Link Headers
        assertThat(response.headers().getList("Link")).extracting("value")
                .containsExactly("<" + assetResourcePath + "?page=1&per_page=10>; rel=\"first\"", "<" + assetResourcePath + "?page=3&per_page=10>; rel=\"last\"");
    }

    @Test
    public void shouldReturnOkOnGetAssetsWithResultsUsingOnlyCount() throws Exception {
        String assetResourcePath = RestIntegrationTests.getAssetsResourcePath(baseURL);

        for (int i = 0; i < 30; i++) {
            String json = "{ \"name\" : \"test1\", \"external_ids\" : [\"id1\", \"id2\"]}";
            RestIntegrationTests.post(assetResourcePath, json, "editor", "password", Optional.of("v1"), Optional.empty());
        }

        ResponseSpecBuilder responseSpecBuilder = new ResponseSpecBuilder();
        responseSpecBuilder.expectStatusCode(200);
        responseSpecBuilder.expectBody("page", nullValue());
        responseSpecBuilder.expectBody("per_page", nullValue());
        responseSpecBuilder.expectBody("total_pages", nullValue());
        responseSpecBuilder.expectBody("total_count", equalTo(30));
        responseSpecBuilder.expectBody("data", hasSize(0));
        responseSpecBuilder.expectBody("links", nullValue());
        com.jayway.restassured.response.Response response = RestIntegrationTests.get(assetResourcePath + "?only_count=true", "reader", "password", Optional.of("v1"), Optional.of(responseSpecBuilder.build()));
        // Validate Link Headers
        assertThat(response.header("Link")).isNull();
    }

    @Test
    public void shouldReturnOkOnGetAssetsDefaultSort() throws Exception {
        String assetResourcePath = RestIntegrationTests.getAssetsResourcePath(baseURL);

        for (int i = 0; i < 5; i++) {
            String json = "{ \"name\" : \"test" + i + "\", \"external_ids\" : [\"id1\", \"id2\"]}";
            RestIntegrationTests.post(assetResourcePath, json, "editor", "password", Optional.of("v1"), Optional.empty());
        }

        ResponseSpecBuilder responseSpecBuilder = new ResponseSpecBuilder();
        responseSpecBuilder.expectStatusCode(200);
        responseSpecBuilder.expectBody("data.name", contains("test4", "test3", "test2", "test1", "test0"));
        RestIntegrationTests.get(assetResourcePath, "reader", "password", Optional.of("v1"), Optional.of(responseSpecBuilder.build()));
    }

    @Test
    public void shouldReturnOkOnGetAssetsSortNameAsc() throws Exception {
        String assetResourcePath = RestIntegrationTests.getAssetsResourcePath(baseURL);

        for (int i = 0; i < 5; i++) {
            String json = "{ \"name\" : \"test" + i + "\", \"external_ids\" : [\"id1\", \"id2\"]}";
            RestIntegrationTests.post(assetResourcePath, json, "editor", "password", Optional.of("v1"), Optional.empty());
        }

        ResponseSpecBuilder responseSpecBuilder = new ResponseSpecBuilder();
        responseSpecBuilder.expectStatusCode(200);
        responseSpecBuilder.expectBody("data.name", contains("test0", "test1", "test2", "test3", "test4"));
        RestIntegrationTests.get(assetResourcePath + "?sort=name&order=ASC", "reader", "password", Optional.of("v1"), Optional.of(responseSpecBuilder.build()));
    }

    @Test
    public void shouldReturnOkOnGetAssetsFilteredByNameEqual() throws Exception {
        String assetResourcePath = RestIntegrationTests.getAssetsResourcePath(baseURL);

        for (int i = 0; i < 5; i++) {
            String json = "{ \"name\" : \"test" + i + "\", \"external_ids\" : [\"id1\", \"id2\"]}";
            RestIntegrationTests.post(assetResourcePath, json, "editor", "password", Optional.of("v1"), Optional.empty());
        }

        ResponseSpecBuilder responseSpecBuilder = new ResponseSpecBuilder();
        responseSpecBuilder.expectStatusCode(200);
        responseSpecBuilder.expectBody("data.name", contains("test0"));
        RestIntegrationTests.get(assetResourcePath + "?name[eq]=test0", "reader", "password", Optional.of("v1"), Optional.of(responseSpecBuilder.build()));
    }

    @Test
    public void shouldReturnOkOnGetAssetsFilteredByNameWithSpaces() throws Exception {
        String assetResourcePath = RestIntegrationTests.getAssetsResourcePath(baseURL);

        for (int i = 0; i < 5; i++) {
            String json = "{ \"name\" : \"test " + i + "\", \"external_ids\" : [\"id1\", \"id2\"]}";
            RestIntegrationTests.post(assetResourcePath, json, "editor", "password", Optional.of("v1"), Optional.empty());
        }

        ResponseSpecBuilder responseSpecBuilder = new ResponseSpecBuilder();
        responseSpecBuilder.expectStatusCode(200);
        responseSpecBuilder.expectBody("data.name", contains("test 0"));
        // the RestAssured framework auto escapes the space to %20
        RestIntegrationTests.get(assetResourcePath + "?name[eq]=test 0", "reader", "password", Optional.of("v1"), Optional.of(responseSpecBuilder.build()));
    }

    @Test
    public void shouldReturnOkOnGetAssetsFilteredByNameLike() throws Exception {
        String assetResourcePath = RestIntegrationTests.getAssetsResourcePath(baseURL);

        for (int i = 0; i < 5; i++) {
            String json = "{ \"name\" : \"test" + i + "\", \"external_ids\" : [\"id1\", \"id2\"]}";
            RestIntegrationTests.post(assetResourcePath, json, "editor", "password", Optional.of("v1"), Optional.empty());
        }

        ResponseSpecBuilder responseSpecBuilder = new ResponseSpecBuilder();
        responseSpecBuilder.expectStatusCode(200);
        responseSpecBuilder.expectBody("data.name", contains("test3"));
        RestIntegrationTests.get(assetResourcePath + "?name[like]=3", "reader", "password", Optional.of("v1"), Optional.of(responseSpecBuilder.build()));
    }

    @Test
    public void shouldReturnOkOnGetAssetsFilteredByExternalIdEqual() throws Exception {
        String assetResourcePath = RestIntegrationTests.getAssetsResourcePath(baseURL);

        String json = "{ \"name\" : \"test\", \"external_ids\" : [\"id1\", \"id2\"]}";
        RestIntegrationTests.post(assetResourcePath, json, "editor", "password", Optional.of("v1"), Optional.empty());

        ResponseSpecBuilder responseSpecBuilder = new ResponseSpecBuilder();
        responseSpecBuilder.expectStatusCode(200);
        responseSpecBuilder.expectBody("data.name", contains("test"));
        RestIntegrationTests.get(assetResourcePath + "?external_ids[eq]=id1", "reader", "password", Optional.of("v1"), Optional.of(responseSpecBuilder.build()));
    }

    @Test
    public void shouldReturnBadRequestOnGetAssetsFilteredByInvalidDateFormat() throws Exception {
        ResponseSpecBuilder responseSpecBuilder = new ResponseSpecBuilder();
        responseSpecBuilder.expectStatusCode(200);
        responseSpecBuilder.expectBody("data.name", contains("test"));
        RestIntegrationTests.get(RestIntegrationTests.getAssetsResourcePath(baseURL) + "?created[eq]=3", "reader", "password", Optional.of("v1"),
                                 Optional.of(RestIntegrationTests.badRequestResponseSpecification(Optional.of("Invalid date format for attribute created"))));
    }

    @Test
    public void shouldReturnBadRequestOnGetAssetsFilteredUnsupportedOperator() throws Exception {
        ResponseSpecBuilder responseSpecBuilder = new ResponseSpecBuilder();
        responseSpecBuilder.expectStatusCode(200);
        responseSpecBuilder.expectBody("data.name", contains("test"));
        RestIntegrationTests.get(RestIntegrationTests.getAssetsResourcePath(baseURL) + "?name[ne]=bad", "reader", "password", Optional.of("v1"),
                                 Optional.of(RestIntegrationTests.badRequestResponseSpecification(Optional.of("Invalid search condition operator NE for condition attribute name"))));
    }

    @Test
    public void shouldReturnOkOnGetAsset() throws Exception {
        String assetResourcePath = RestIntegrationTests.getAssetsResourcePath(baseURL);

        String json = "{ \"name\" : \"test1\", \"external_ids\" : [\"id1\", \"id2\"]}";
        String assetId = RestIntegrationTests.post(assetResourcePath, json, "editor", "password", Optional.of("v1"), Optional.empty()).header("Location").replace(assetResourcePath + "/", "");
        RestIntegrationTests.get(RestIntegrationTests.getAssetResourcePath(baseURL, assetId), "reader", "password", Optional.of("v1"), Optional.of(getAssetOkResponseSpecification(assetId, "test1", "editor", "editor", "id1", "id2")));
    }

    @Test
    public void shouldReturnNotFoundOnGetAsset() throws Exception {
        RestIntegrationTests.get(RestIntegrationTests.getAssetResourcePath(baseURL, UUID.randomUUID().toString()), "reader", "password", Optional.of("v1"), Optional.of(
                RestIntegrationTests.notFoundResponseSpecification()));
    }

    @Test
    public void shouldReturnNotFoundOnGetAssetWithInvalidId() throws Exception {
        RestIntegrationTests.get(RestIntegrationTests.getAssetResourcePath(baseURL, "BAD-ID"), "reader", "password", Optional.of("v1"), Optional.of(RestIntegrationTests.notFoundResponseSpecification()));
    }

    @Test
    public void shouldReturnNotModifiedOnGETWithEtag() throws Exception {
        String assetResourcePath = RestIntegrationTests.getAssetsResourcePath(baseURL);
        String json = "{ \"name\" : \"test1\", \"external_ids\" : [\"id1\", \"id2\"]}";
        String assetId = RestIntegrationTests.post(assetResourcePath, json, "editor", "password", Optional.of("v1"), Optional.empty()).header("Location").replace(assetResourcePath + "/", "");
        com.jayway.restassured.response.Response response =
                RestIntegrationTests.get(RestIntegrationTests.getAssetResourcePath(baseURL, assetId), "reader", "password", Optional.of("v1"), Optional.of(getAssetOkResponseSpecification(assetId, "test1", "editor", "editor", "id1", "id2")));
        String etag = response.getHeader("ETag");
        given().spec(RestIntegrationTests.requestSpecification("reader", "password", Optional.of("v1"))).header("If-None-Match", etag).then().statusCode(Response.Status.NOT_MODIFIED.getStatusCode()).when()
                .header("ETag", notNullValue()).get(RestIntegrationTests.getAssetResourcePath(baseURL, assetId)).then().assertThat().body(isEmptyOrNullString());
    }

    @Test
    public void shouldReturnOkOnPutAssetWithMinimalPayload() {
        String assetResourcePath = RestIntegrationTests.getAssetsResourcePath(baseURL);
        String json = "{ \"name\" : \"test1\", \"external_ids\" : [\"id1\", \"id2\"]}";
        String assetId = RestIntegrationTests.post(assetResourcePath, json, "editor", "password", Optional.of("v1"), Optional.empty()).header("Location").replace(assetResourcePath + "/", "");

        ResponseBody before =
                RestIntegrationTests.get(RestIntegrationTests.getAssetResourcePath(baseURL, assetId), "reader", "password", Optional.of("v1"), Optional.of(getAssetOkResponseSpecification(assetId, "test1", "editor", "editor", "id1", "id2")))
                        .body();

        String updateJson = "{ \"name\" : \"test2\", \"external_ids\" : [\"id1\", \"id2\", \"id3\"]}";
        RestIntegrationTests.put(RestIntegrationTests.getAssetResourcePath(baseURL, assetId), updateJson, "admin", "password", Optional.of("v1"), Optional.empty());

        ResponseBody after = RestIntegrationTests.get(RestIntegrationTests.getAssetResourcePath(baseURL, assetId), "reader", "password", Optional.of("v1"),
                                                      Optional.of(getAssetOkResponseSpecification(assetId, "test2", "editor", "admin", "id1", "id2", "id3"))).body();
        checkAssetReadOnlyFieldsAreTheSame(before, after);
    }

    @Test
    public void shouldReturnOkOnPutAssetWithFullPayload() {
        String assetResourcePath = RestIntegrationTests.getAssetsResourcePath(baseURL);
        String json = "{ \"name\" : \"test1\", \"external_ids\" : [\"id1\", \"id2\"]}";
        String assetId = RestIntegrationTests.post(assetResourcePath, json, "editor", "password", Optional.of("v1"), Optional.empty()).header("Location").replace(assetResourcePath + "/", "");

        ResponseBody before =
                RestIntegrationTests.get(RestIntegrationTests.getAssetResourcePath(baseURL, assetId), "reader", "password", Optional.of("v1"), Optional.of(getAssetOkResponseSpecification(assetId, "test1", "editor", "editor", "id1", "id2")))
                        .body();

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
                "    \"last_modified_by\": \"bad\"," +
                "    \"links\": {\n" +
                "           \"self\": {\n" +
                "               \"href\": \"http://127.0.0.1:8081/omakase/api/assets/72a25cfa_7bfd_44aa_95fc_61b8cb3f846c\"\n" +
                "           },\n" +
                "           \"related\": {\n" +
                "               \"href\": \"http://127.0.0.1:8081/omakase/api/assets/72a25cfa_7bfd_44aa_95fc_61b8cb3f846c/related\"\n" +
                "           },\n" +
                "           \"parent\": {\n" +
                "               \"href\": \"http://127.0.0.1:8081/omakase/api/assets/72a25cfa_7bfd_44aa_95fc_61b8cb3f846c/parent\"\n" +
                "           },\n" +
                "           \"children\": {\n" +
                "               \"href\": \"http://127.0.0.1:8081/omakase/api/assets/72a25cfa_7bfd_44aa_95fc_61b8cb3f846c/children\"\n" +
                "           },\n" +
                "           \"variants\": {\n" +
                "               \"href\": \"http://127.0.0.1:8081/omakase/api/assets/72a25cfa_7bfd_44aa_95fc_61b8cb3f846c/variants\"\n" +
                "           },\n" +
                "           \"metadata\": {\n" +
                "               \"href\": \"http://127.0.0.1:8081/omakase/api/assets/72a25cfa_7bfd_44aa_95fc_61b8cb3f846c/metadata\"\n" +
                "           }\n" +
                "       }" +
                "}";
        RestIntegrationTests.put(RestIntegrationTests.getAssetResourcePath(baseURL, assetId), updateJson, "admin", "password", Optional.of("v1"), Optional.empty());

        ResponseBody after = RestIntegrationTests.get(RestIntegrationTests.getAssetResourcePath(baseURL, assetId), "reader", "password", Optional.of("v1"),
                                                      Optional.of(getAssetOkResponseSpecification(assetId, "test2", "editor", "admin", "id1", "id2", "id3"))).body();
        checkAssetReadOnlyFieldsAreTheSame(before, after);
    }

    @Test
    public void shouldReturnNotFoundOnPutAsset() {
        String updateJson = "{ \"name\" : \"test2\", \"external_ids\" : [\"id1\", \"id2\", \"id3\"]}";
        RestIntegrationTests.put(RestIntegrationTests.getAssetResourcePath(baseURL, UUID.randomUUID().toString()), updateJson, "admin", "password", Optional.of("v1"), Optional.of(
                RestIntegrationTests.notFoundResponseSpecification()));
    }

    @Test
    public void shouldReturnForbiddenOnPutAsset() {
        String assetResourcePath = RestIntegrationTests.getAssetsResourcePath(baseURL);
        String json = "{ \"name\" : \"test1\", \"external_ids\" : [\"id1\", \"id2\"]}";
        String assetId = RestIntegrationTests.post(assetResourcePath, json, "editor", "password", Optional.of("v1"), Optional.empty()).header("Location").replace(assetResourcePath + "/", "");
        RestIntegrationTests
                .put(RestIntegrationTests.getAssetResourcePath(baseURL, assetId), json, "reader", "password", Optional.of("v1"), Optional.of(RestIntegrationTests.forbiddenResponseSpecification()));
    }

    @Test
    public void shouldReturnOkOnPatchAsset() {
        String assetResourcePath = RestIntegrationTests.getAssetsResourcePath(baseURL);
        String json = "{ \"name\" : \"test1\", \"external_ids\" : [\"id1\", \"id2\"]}";
        String assetId = RestIntegrationTests.post(assetResourcePath, json, "editor", "password", Optional.of("v1"), Optional.empty()).header("Location").replace(assetResourcePath + "/", "");

        ResponseBody before =
                RestIntegrationTests.get(RestIntegrationTests.getAssetResourcePath(baseURL, assetId), "reader", "password", Optional.of("v1"), Optional.of(getAssetOkResponseSpecification(assetId, "test1", "editor", "editor", "id1", "id2")))
                        .body();

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
        RestIntegrationTests.patch(RestIntegrationTests.getAssetResourcePath(baseURL, assetId), patchJson, "admin", "password", Optional.of("v1"), Optional.empty());

        ResponseBody after = RestIntegrationTests.get(RestIntegrationTests.getAssetResourcePath(baseURL, assetId), "reader", "password", Optional.of("v1"),
                                                      Optional.of(getAssetOkResponseSpecification(assetId, "test2", "editor", "admin", "id1", "id2", "id3"))).body();
        checkAssetReadOnlyFieldsAreTheSame(before, after);
    }

    @Test
    public void shouldIgnoreReadOnlyFieldsOnPatchAsset() {
        String assetResourcePath = RestIntegrationTests.getAssetsResourcePath(baseURL);
        String json = "{ \"name\" : \"test1\", \"external_ids\" : [\"id1\", \"id2\"]}";
        String assetId = RestIntegrationTests.post(assetResourcePath, json, "editor", "password", Optional.of("v1"), Optional.empty()).header("Location").replace(assetResourcePath + "/", "");

        ResponseBody before =
                RestIntegrationTests.get(RestIntegrationTests.getAssetResourcePath(baseURL, assetId), "reader", "password", Optional.of("v1"), Optional.of(getAssetOkResponseSpecification(assetId, "test1", "editor", "editor", "id1", "id2")))
                        .body();

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
        RestIntegrationTests.patch(RestIntegrationTests.getAssetResourcePath(baseURL, assetId), patchJson, "admin", "password", Optional.of("v1"), Optional.empty());

        ResponseBody after =
                RestIntegrationTests.get(RestIntegrationTests.getAssetResourcePath(baseURL, assetId), "reader", "password", Optional.of("v1"), Optional.of(getAssetOkResponseSpecification(assetId, "test1", "editor", "admin", "id1", "id2")))
                        .body();
        checkAssetReadOnlyFieldsAreTheSame(before, after);
    }

    @Test
    public void shouldReturnBadRequestOnPatchAssetIfExternalIdsIsReplacedWithASingleValue() {
        String assetResourcePath = RestIntegrationTests.getAssetsResourcePath(baseURL);
        String json = "{ \"name\" : \"test1\", \"external_ids\" : [\"id1\", \"id2\"]}";
        String assetId = RestIntegrationTests.post(assetResourcePath, json, "editor", "password", Optional.of("v1"), Optional.empty()).header("Location").replace(assetResourcePath + "/", "");

        String patchJson = "[" +
                "    {" +
                "        \"op\": \"replace\"," +
                "        \"path\": \"/external_ids\"," +
                "        \"value\": \"id1\"" +
                "    }" +
                "]";
        RestIntegrationTests
                .patch(RestIntegrationTests.getAssetResourcePath(baseURL, assetId), patchJson, "admin", "password", Optional.of("v1"), Optional.of(RestIntegrationTests.badRequestResponseSpecification()));
    }

    @Test
    public void shouldReturnNotFoundOnPatchAsset() {
        String patchJson = "[" +
                "    {" +
                "        \"op\": \"replace\"," +
                "        \"path\": \"/name\"," +
                "        \"value\": \"test2\"" +
                "    }" +
                "]";
        RestIntegrationTests.patch(RestIntegrationTests.getAssetResourcePath(baseURL, UUID.randomUUID().toString()), patchJson, "admin", "password", Optional.of("v1"), Optional.of(
                RestIntegrationTests.notFoundResponseSpecification()));
    }

    @Test
    public void shouldReturnForbiddenOnPatchAsset() {
        String assetResourcePath = RestIntegrationTests.getAssetsResourcePath(baseURL);
        String json = "{ \"name\" : \"test1\", \"external_ids\" : [\"id1\", \"id2\"]}";
        String assetId = RestIntegrationTests.post(assetResourcePath, json, "editor", "password", Optional.of("v1"), Optional.empty()).header("Location").replace(assetResourcePath + "/", "");
        String patchJson = "[" +
                "    {" +
                "        \"op\": \"replace\"," +
                "        \"path\": \"/name\"," +
                "        \"value\": \"test2\"" +
                "    }" +
                "]";
        RestIntegrationTests
                .patch(RestIntegrationTests.getAssetResourcePath(baseURL, assetId), patchJson, "reader", "password", Optional.of("v1"), Optional.of(RestIntegrationTests.forbiddenResponseSpecification()));
    }

    @Test
    public void shouldReturnOkOnDeleteAsset() throws Exception {
        String assetResourcePath = RestIntegrationTests.getAssetsResourcePath(baseURL);
        String json = "{ \"name\" : \"test1\", \"external_ids\" : [\"id1\", \"id2\"]}";
        String assetId = RestIntegrationTests.post(assetResourcePath, json, "editor", "password", Optional.of("v1"), Optional.empty()).header("Location").replace(assetResourcePath + "/", "");
        RestIntegrationTests.delete(RestIntegrationTests.getAssetResourcePath(baseURL, assetId), "editor", "password", Optional.of("v1"), Optional.empty());
    }

    @Test
    public void shouldReturnNotFoundOnDeleteAsset() throws Exception {
        RestIntegrationTests.delete(RestIntegrationTests.getAssetResourcePath(baseURL, UUID.randomUUID().toString()), "editor", "password", Optional.of("v1"), Optional.of(
                RestIntegrationTests.notFoundResponseSpecification()));
    }

    @Test
    public void shouldReturnForbiddenOnDeleteAssetNotAuthorized() throws Exception {
        String assetResourcePath = RestIntegrationTests.getAssetsResourcePath(baseURL);
        String json = "{ \"name\" : \"test1\", \"external_ids\" : [\"id1\", \"id2\"]}";
        String assetId = RestIntegrationTests.post(assetResourcePath, json, "editor", "password", Optional.of("v1"), Optional.empty()).header("Location").replace(assetResourcePath + "/", "");
        RestIntegrationTests
                .delete(RestIntegrationTests.getAssetResourcePath(baseURL, assetId), "reader", "password", Optional.of("v1"), Optional.of(RestIntegrationTests.forbiddenResponseSpecification()));
    }

    @Test
    public void shouldReturnForbiddenOnDeleteAssetHasVariants() throws Exception {
        String assetResourcePath = RestIntegrationTests.getAssetsResourcePath(baseURL);
        String json = "{ \"name\" : \"test1\", \"external_ids\" : [\"id1\", \"id2\"]}";
        String assetId = RestIntegrationTests.post(assetResourcePath, json, "editor", "password", Optional.of("v1"), Optional.empty()).header("Location").replace(assetResourcePath + "/", "");
        RestIntegrationTests.post(RestIntegrationTests.getVariantsResourcePath(baseURL, assetId), json, "editor", "password", Optional.of("v1"), Optional.empty());
        RestIntegrationTests.delete(RestIntegrationTests.getAssetResourcePath(baseURL, assetId), "editor", "password", Optional.of("v1"), Optional.of(
                RestIntegrationTests.forbiddenResponseSpecification("One or more variants are associated to asset " + assetId)));
    }

    private ResponseSpecification getAssetOkResponseSpecification(String assetId, String name, String createdUsername, String modifiedUsername, String... externalIds) {
        String assetResourcePath = RestIntegrationTests.getAssetsResourcePath(baseURL);
        ResponseSpecBuilder builder = new ResponseSpecBuilder();
        builder.expectStatusCode(200);
        builder.expectBody("name", equalTo(name));
        builder.expectBody("external_ids", contains(externalIds));
        builder.expectBody("created", notNullValue(String.class));
        builder.expectBody("created_by", equalTo(createdUsername));
        builder.expectBody("last_modified", notNullValue(String.class));
        builder.expectBody("last_modified_by", equalTo(modifiedUsername));
        builder.expectBody("links.self.href", equalTo(assetResourcePath + "/" + assetId));
        builder.expectBody("links.related.href", equalTo(assetResourcePath + "/" + assetId + "/related"));
        builder.expectBody("links.parent.href", equalTo(assetResourcePath + "/" + assetId + "/parent"));
        builder.expectBody("links.children.href", equalTo(assetResourcePath + "/" + assetId + "/children"));
        builder.expectBody("links.variants.href", equalTo(assetResourcePath + "/" + assetId + "/variants"));
        builder.expectBody("links.metadata.href", equalTo(assetResourcePath + "/" + assetId + "/metadata"));
        return builder.build();
    }

    private void checkAssetReadOnlyFieldsAreTheSame(ResponseBody before, ResponseBody after) {
        assertThat(before.jsonPath().getString("id")).isEqualTo(after.jsonPath().getString("id"));
        assertThat(before.jsonPath().getString("created_by")).isEqualTo(after.jsonPath().getString("created_by"));
        assertThat(before.jsonPath().getString("created")).isEqualTo(after.jsonPath().getString("created"));
        assertThat(before.jsonPath().getString("links.self.href")).isEqualTo(after.jsonPath().getString("links.self.href"));
        assertThat(before.jsonPath().getString("links.related.href")).isEqualTo(after.jsonPath().getString("links.related.href"));
        assertThat(before.jsonPath().getString("links.parent.href")).isEqualTo(after.jsonPath().getString("links.parent.href"));
        assertThat(before.jsonPath().getString("links.children.href")).isEqualTo(after.jsonPath().getString("links.children.href"));
        assertThat(before.jsonPath().getString("links.variants.href")).isEqualTo(after.jsonPath().getString("links.variants.href"));
        assertThat(before.jsonPath().getString("links.metadata.href")).isEqualTo(after.jsonPath().getString("links.metadata.href"));
    }
}
