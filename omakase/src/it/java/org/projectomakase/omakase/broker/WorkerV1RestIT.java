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
package org.projectomakase.omakase.broker;

import com.google.common.collect.ImmutableList;
import com.jayway.restassured.RestAssured;
import com.jayway.restassured.builder.ResponseSpecBuilder;
import com.jayway.restassured.filter.log.RequestLoggingFilter;
import com.jayway.restassured.filter.log.ResponseLoggingFilter;
import com.jayway.restassured.specification.ResponseSpecification;
import org.projectomakase.omakase.Archives;
import org.projectomakase.omakase.RestIntegrationTests;
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
import static org.projectomakase.omakase.RestIntegrationTests.getWorkersResourcePath;
import static org.projectomakase.omakase.RestIntegrationTests.notFoundResponseSpecification;
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
 * Worker V1 REST API Black Box Tests
 *
 * @author Richard Lucas
 */
@RunAsClient
@RunWith(Arquillian.class)
public class WorkerV1RestIT {

    DateTimeFormatter dateTimeFormatter = DateTimeFormatters.getDefaultZonedDateTimeFormatter();

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
    public void shouldReturnCreatedOnPostWorkers() throws Exception {
        String json = "{ \"name\" : \"test1\", \"external_ids\" : [\"id1\", \"id2\"]}";
        post(getWorkersResourcePath(baseURL), json, "editor", "password", Optional.of("v1"), Optional.empty());
    }

    @Test
    public void shouldReturnOkOnGetWorkersWithResultsUsingPagination() throws Exception {
        String workerResourcePath = getWorkersResourcePath(baseURL);

        for (int i = 0; i < 30; i++) {
            String json = "{ \"name\" : \"test1\", \"external_ids\" : [\"id1\", \"id2\"]}";
            post(workerResourcePath, json, "editor", "password", Optional.of("v1"), Optional.empty());
        }

        ResponseSpecBuilder builder = new ResponseSpecBuilder();
        builder.expectStatusCode(200);
        builder.expectBody("page", equalTo(1));
        builder.expectBody("per_page", equalTo(10));
        builder.expectBody("total_pages", equalTo(3));
        builder.expectBody("total_count", equalTo(30));
        builder.expectBody("data", hasSize(10));
        builder.expectBody("links.first.href", equalTo(workerResourcePath + "?page=1&per_page=10"));
        builder.expectBody("links.last.href", equalTo(workerResourcePath + "?page=3&per_page=10"));
        builder.expectBody("links.next.href", equalTo(workerResourcePath + "?page=2&per_page=10"));
        com.jayway.restassured.response.Response response = get(workerResourcePath, "reader", "password", Optional.of("v1"), Optional.of(builder.build()));
        // Validate Link Headers
        assertThat(response.headers().getList("Link")).extracting("value")
                .containsExactly("<" + workerResourcePath + "?page=1&per_page=10>; rel=\"first\"", "<" + workerResourcePath + "?page=3&per_page=10>; rel=\"last\"",
                        "<" + workerResourcePath + "?page=2&per_page=10>; rel=\"next\"");

        builder = new ResponseSpecBuilder();
        builder.expectStatusCode(200);
        builder.expectBody("page", equalTo(2));
        builder.expectBody("per_page", equalTo(10));
        builder.expectBody("total_pages", equalTo(3));
        builder.expectBody("total_count", equalTo(30));
        builder.expectBody("data", hasSize(10));
        builder.expectBody("links.first.href", equalTo(workerResourcePath + "?page=1&per_page=10"));
        builder.expectBody("links.last.href", equalTo(workerResourcePath + "?page=3&per_page=10"));
        builder.expectBody("links.next.href", equalTo(workerResourcePath + "?page=3&per_page=10"));
        builder.expectBody("links.prev.href", equalTo(workerResourcePath + "?page=1&per_page=10"));
        response = get(workerResourcePath + "?page=2&per_page=10", "reader", "password", Optional.of("v1"), Optional.of(builder.build()));
        // Validate Link Headers
        assertThat(response.headers().getList("Link")).extracting("value")
                .containsExactly("<" + workerResourcePath + "?page=1&per_page=10>; rel=\"first\"", "<" + workerResourcePath + "?page=3&per_page=10>; rel=\"last\"",
                        "<" + workerResourcePath + "?page=1&per_page=10>; rel=\"prev\"", "<" + workerResourcePath + "?page=3&per_page=10>; rel=\"next\"");


        builder = new ResponseSpecBuilder();
        builder.expectStatusCode(200);
        builder.expectBody("page", equalTo(3));
        builder.expectBody("per_page", equalTo(10));
        builder.expectBody("total_pages", equalTo(3));
        builder.expectBody("total_count", equalTo(30));
        builder.expectBody("data", hasSize(10));
        builder.expectBody("links.first.href", equalTo(workerResourcePath + "?page=1&per_page=10"));
        builder.expectBody("links.last.href", equalTo(workerResourcePath + "?page=3&per_page=10"));
        builder.expectBody("links.prev.href", equalTo(workerResourcePath + "?page=2&per_page=10"));
        response = get(workerResourcePath + "?page=3&per_page=10", "reader", "password", Optional.of("v1"), Optional.of(builder.build()));
        // Validate Link Headers
        assertThat(response.headers().getList("Link")).extracting("value")
                .containsExactly("<" + workerResourcePath + "?page=1&per_page=10>; rel=\"first\"", "<" + workerResourcePath + "?page=3&per_page=10>; rel=\"last\"",
                        "<" + workerResourcePath + "?page=2&per_page=10>; rel=\"prev\"");

        builder = new ResponseSpecBuilder();
        builder.expectStatusCode(200);
        builder.expectBody("page", equalTo(4));
        builder.expectBody("per_page", equalTo(10));
        builder.expectBody("total_pages", equalTo(3));
        builder.expectBody("total_count", equalTo(30));
        builder.expectBody("data", hasSize(0));
        builder.expectBody("links.first.href", equalTo(workerResourcePath + "?page=1&per_page=10"));
        builder.expectBody("links.prev", nullValue());
        builder.expectBody("links.next", nullValue());
        response = get(workerResourcePath + "?page=4&per_page=10", "reader", "password", Optional.of("v1"), Optional.of(builder.build()));
        // Validate Link Headers
        assertThat(response.headers().getList("Link")).extracting("value")
                .containsExactly("<" + workerResourcePath + "?page=1&per_page=10>; rel=\"first\"", "<" + workerResourcePath + "?page=3&per_page=10>; rel=\"last\"");
    }

    @Test
    public void shouldReturnOkOnGetWorkersWithResultsUsingOnlyCount() throws Exception {
        String workerResourcePath = getWorkersResourcePath(baseURL);

        for (int i = 0; i < 30; i++) {
            String json = "{ \"name\" : \"test1\", \"external_ids\" : [\"id1\", \"id2\"]}";
            post(workerResourcePath, json, "editor", "password", Optional.of("v1"), Optional.empty());
        }

        ResponseSpecBuilder responseSpecBuilder = new ResponseSpecBuilder();
        responseSpecBuilder.expectStatusCode(200);
        responseSpecBuilder.expectBody("page", nullValue());
        responseSpecBuilder.expectBody("per_page", nullValue());
        responseSpecBuilder.expectBody("total_pages", nullValue());
        responseSpecBuilder.expectBody("total_count", equalTo(30));
        responseSpecBuilder.expectBody("data", hasSize(0));
        responseSpecBuilder.expectBody("links", nullValue());
        com.jayway.restassured.response.Response response = get(workerResourcePath + "?only_count=true", "reader", "password", Optional.of("v1"), Optional.of(responseSpecBuilder.build()));
        // Validate Link Headers
        assertThat(response.header("Link")).isNull();
    }

    @Test
    public void shouldReturnOkOnGetWorkersDefaultSort() throws Exception {
        String workerResourcePath = getWorkersResourcePath(baseURL);

        for (int i = 0; i < 5; i++) {
            String json = "{ \"name\" : \"test" + i + "\", \"external_ids\" : [\"id1\", \"id2\"]}";
            post(workerResourcePath, json, "editor", "password", Optional.of("v1"), Optional.empty());
        }

        ResponseSpecBuilder responseSpecBuilder = new ResponseSpecBuilder();
        responseSpecBuilder.expectStatusCode(200);
        responseSpecBuilder.expectBody("data.name", contains("test4", "test3", "test2", "test1", "test0"));
        get(workerResourcePath, "reader", "password", Optional.of("v1"), Optional.of(responseSpecBuilder.build()));
    }

    @Test
    public void shouldReturnOkOnGetWorkersSortNameAsc() throws Exception {
        String workerResourcePath = getWorkersResourcePath(baseURL);

        for (int i = 0; i < 5; i++) {
            String json = "{ \"name\" : \"test" + i + "\", \"external_ids\" : [\"id1\", \"id2\"]}";
            post(workerResourcePath, json, "editor", "password", Optional.of("v1"), Optional.empty());
        }

        ResponseSpecBuilder responseSpecBuilder = new ResponseSpecBuilder();
        responseSpecBuilder.expectStatusCode(200);
        responseSpecBuilder.expectBody("data.name", contains("test0", "test1", "test2", "test3", "test4"));
        get(workerResourcePath + "?sort=name&order=ASC", "reader", "password", Optional.of("v1"), Optional.of(responseSpecBuilder.build()));
    }

    @Test
    public void shouldReturnOkOnGetWorkersFilteredByNameEqual() throws Exception {
        String workerResourcePath = getWorkersResourcePath(baseURL);

        for (int i = 0; i < 5; i++) {
            String json = "{ \"name\" : \"test" + i + "\", \"external_ids\" : [\"id1\", \"id2\"]}";
            post(workerResourcePath, json, "editor", "password", Optional.of("v1"), Optional.empty());
        }

        ResponseSpecBuilder responseSpecBuilder = new ResponseSpecBuilder();
        responseSpecBuilder.expectStatusCode(200);
        responseSpecBuilder.expectBody("data.name", contains("test0"));
        get(workerResourcePath + "?name[eq]=test0", "reader", "password", Optional.of("v1"), Optional.of(responseSpecBuilder.build()));
    }

    @Test
    public void shouldReturnOkOnGetWorkersFilteredByNameWithSpaces() throws Exception {
        String workerResourcePath = getWorkersResourcePath(baseURL);

        for (int i = 0; i < 5; i++) {
            String json = "{ \"name\" : \"test " + i + "\", \"external_ids\" : [\"id1\", \"id2\"]}";
            post(workerResourcePath, json, "editor", "password", Optional.of("v1"), Optional.empty());
        }

        ResponseSpecBuilder responseSpecBuilder = new ResponseSpecBuilder();
        responseSpecBuilder.expectStatusCode(200);
        responseSpecBuilder.expectBody("data.name", contains("test 0"));
        // the RestAssured framework auto escapes the space to %20
        get(workerResourcePath + "?name[eq]=test 0", "reader", "password", Optional.of("v1"), Optional.of(responseSpecBuilder.build()));
    }

    @Test
    public void shouldReturnOkOnGetWorkersFilteredByNameLike() throws Exception {
        String workerResourcePath = getWorkersResourcePath(baseURL);

        for (int i = 0; i < 5; i++) {
            String json = "{ \"name\" : \"test" + i + "\", \"external_ids\" : [\"id1\", \"id2\"]}";
            post(workerResourcePath, json, "editor", "password", Optional.of("v1"), Optional.empty());
        }

        ResponseSpecBuilder responseSpecBuilder = new ResponseSpecBuilder();
        responseSpecBuilder.expectStatusCode(200);
        responseSpecBuilder.expectBody("data.name", contains("test3"));
        get(workerResourcePath + "?name[like]=3", "reader", "password", Optional.of("v1"), Optional.of(responseSpecBuilder.build()));
    }

    @Test
    public void shouldReturnBadRequestOnGetWorkersFilteredByInvalidDateFormat() throws Exception {
        ResponseSpecBuilder responseSpecBuilder = new ResponseSpecBuilder();
        responseSpecBuilder.expectStatusCode(200);
        responseSpecBuilder.expectBody("data.name", contains("test"));
        get(getWorkersResourcePath(baseURL) + "?created[eq]=3", "reader", "password", Optional.of("v1"),
                Optional.of(badRequestResponseSpecification(Optional.of("Invalid date format for attribute created"))));
    }

    @Test
    public void shouldReturnBadRequestOnGetWorkersFilteredUnsupportedOperator() throws Exception {
        ResponseSpecBuilder responseSpecBuilder = new ResponseSpecBuilder();
        responseSpecBuilder.expectStatusCode(200);
        responseSpecBuilder.expectBody("data.name", contains("test"));
        get(getWorkersResourcePath(baseURL) + "?name[ne]=bad", "reader", "password", Optional.of("v1"),
                Optional.of(badRequestResponseSpecification(Optional.of("Invalid search condition operator NE for condition attribute name"))));
    }

    @Test
    public void shouldReturnOkOnGetWorker() throws Exception {
        String workerResourcePath = getWorkersResourcePath(baseURL);

        String json = "{ \"name\" : \"test1\", \"external_ids\" : [\"id1\", \"id2\"]}";
        String workerId = post(workerResourcePath, json, "editor", "password", Optional.of("v1"), Optional.empty()).header("Location").replace(workerResourcePath + "/", "");
        get(getWorkersResourcePath(baseURL, workerId), "reader", "password", Optional.of("v1"),
                Optional.of(getWorkerOkResponseSpecification(workerId, "test1", "ACTIVE", "editor", "editor", "id1", "id2")));
    }

    @Test
    public void shouldReturnNotFoundOnGetWorker() throws Exception {
        get(getWorkersResourcePath(baseURL, UUID.randomUUID().toString()), "reader", "password", Optional.of("v1"), Optional.of(notFoundResponseSpecification()));
    }

    @Test
    public void shouldReturnNotFoundOnGetWorkerWithInvalidId() throws Exception {
        get(getWorkersResourcePath(baseURL, "BAD-ID"), "reader", "password", Optional.of("v1"), Optional.of(notFoundResponseSpecification()));
    }

    @Test
    public void shouldReturnNotModifiedOnGETWithEtag() throws Exception {
        String workerResourcePath = getWorkersResourcePath(baseURL);
        String json = "{ \"name\" : \"test1\", \"external_ids\" : [\"id1\", \"id2\"]}";
        String workerId = post(workerResourcePath, json, "editor", "password", Optional.of("v1"), Optional.empty()).header("Location").replace(workerResourcePath + "/", "");
        com.jayway.restassured.response.Response response = get(getWorkersResourcePath(baseURL, workerId), "reader", "password", Optional.of("v1"),
                Optional.of(getWorkerOkResponseSpecification(workerId, "test1", "ACTIVE", "editor", "editor", "id1", "id2")));
        String etag = response.getHeader("ETag");
        given().spec(requestSpecification("reader", "password", Optional.of("v1"))).header("If-None-Match", etag).then().statusCode(Response.Status.NOT_MODIFIED.getStatusCode()).when()
                .header("ETag", notNullValue()).get(getWorkersResourcePath(baseURL, workerId)).then().assertThat().body(isEmptyOrNullString());
    }

    @Test
    public void shouldReturnOkOnDeleteWorker() throws Exception {
        String workerResourcePath = getWorkersResourcePath(baseURL);
        String json = "{ \"name\" : \"test1\", \"external_ids\" : [\"id1\", \"id2\"]}";
        String workerId = post(workerResourcePath, json, "editor", "password", Optional.of("v1"), Optional.empty()).header("Location").replace(workerResourcePath + "/", "");
        delete(getWorkersResourcePath(baseURL, workerId), "editor", "password", Optional.of("v1"), Optional.empty());
    }

    @Test
    public void shouldReturnNotFoundOnDeleteWorker() throws Exception {
        delete(getWorkersResourcePath(baseURL, UUID.randomUUID().toString()), "editor", "password", Optional.of("v1"), Optional.of(notFoundResponseSpecification()));
    }

    @Test
    public void shouldReturnForbiddenOnDeleteWorkerNotAuthorized() throws Exception {
        String workerResourcePath = getWorkersResourcePath(baseURL);
        String json = "{ \"name\" : \"test1\", \"external_ids\" : [\"id1\", \"id2\"]}";
        String workerId = post(workerResourcePath, json, "editor", "password", Optional.of("v1"), Optional.empty()).header("Location").replace(workerResourcePath + "/", "");
        delete(getWorkersResourcePath(baseURL, workerId), "reader", "password", Optional.of("v1"), Optional.of(forbiddenResponseSpecification()));
    }

    @Test
    public void shouldReturnOkOnUpdateWorker() throws Exception {
        String workerResourcePath = getWorkersResourcePath(baseURL);
        String json = "{}";
        String workerId = post(workerResourcePath, json, "editor", "password", Optional.of("v1"), Optional.empty()).header("Location").replace(workerResourcePath + "/", "");
        put(getWorkersResourcePath(baseURL, workerId), "{ \"name\" : \"test1\", \"external_ids\" : [\"id1\", \"id2\"], \"status\": {\"current\": \"ACTIVE\"}}", "editor", "password", Optional.of("v1"), Optional.empty());

        get(getWorkersResourcePath(baseURL, workerId), "reader", "password", Optional.of("v1"),
            Optional.of(getWorkerOkResponseSpecification(workerId, "test1", "ACTIVE", "editor", "editor", "id1", "id2")));
    }

    private ResponseSpecification getWorkerOkResponseSpecification(String workerId, String name, String status, String createdUsername, String modifiedUsername, String... externalIds) {
        String workerResourcePath = getWorkersResourcePath(baseURL);
        ResponseSpecBuilder builder = new ResponseSpecBuilder();
        builder.expectStatusCode(200);
        builder.expectBody("name", equalTo(name));
        builder.expectBody("external_ids", contains(externalIds));
        builder.expectBody("status.current", equalTo(status));
        builder.expectBody("status.timestamp", notNullValue(String.class));
        builder.expectBody("created", notNullValue(String.class));
        builder.expectBody("created_by", equalTo(createdUsername));
        builder.expectBody("last_modified", notNullValue(String.class));
        builder.expectBody("last_modified_by", equalTo(modifiedUsername));
        builder.expectBody("links.self.href", equalTo(workerResourcePath + "/" + workerId));
        builder.expectBody("links.messages.href", equalTo(workerResourcePath + "/" + workerId + "/messages"));
        builder.expectBody("links.status.href", equalTo(workerResourcePath + "/" + workerId + "/status"));
        builder.expectBody("links.tasks.href", equalTo(workerResourcePath + "/" + workerId + "/tasks"));
        return builder.build();
    }
}
