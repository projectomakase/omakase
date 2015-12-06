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
package org.projectomakase.omakase.repository;

import com.google.common.collect.ImmutableList;
import com.jayway.restassured.RestAssured;
import com.jayway.restassured.builder.ResponseSpecBuilder;
import com.jayway.restassured.filter.log.RequestLoggingFilter;
import com.jayway.restassured.filter.log.ResponseLoggingFilter;
import com.jayway.restassured.specification.ResponseSpecification;
import org.projectomakase.omakase.Archives;
import org.projectomakase.omakase.RestIntegrationTests;
import org.projectomakase.omakase.commons.functions.Throwables;
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
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static com.jayway.restassured.RestAssured.given;
import static org.projectomakase.omakase.RestIntegrationTests.badRequestResponseSpecification;
import static org.projectomakase.omakase.RestIntegrationTests.delete;
import static org.projectomakase.omakase.RestIntegrationTests.forbiddenResponseSpecification;
import static org.projectomakase.omakase.RestIntegrationTests.get;
import static org.projectomakase.omakase.RestIntegrationTests.notFoundResponseSpecification;
import static org.projectomakase.omakase.RestIntegrationTests.post;
import static org.projectomakase.omakase.RestIntegrationTests.put;
import static org.projectomakase.omakase.RestIntegrationTests.requestSpecification;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.isEmptyOrNullString;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

/**
 * Repository V1 REST API Black Box Tests
 *
 * @author Richard Lucas
 */
@RunAsClient
@RunWith(Arquillian.class)
public class RepositoryV1RestIT {

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
    public void shouldReturnCreatedOnPostRepositories() throws Exception {
        String json = "{ \"name\" : \"test1\", \"type\" : \"FILE\"}";
        post(getRepositoriesResourcePath(), json, "editor", "password", Optional.of("v1"), Optional.empty());
    }

    @Test
    public void shouldReturnForbiddenOnPostRepositoriesWithUnauthorizedUser() throws Exception {
        String json = "{ \"name\" : \"test1\", \"type\" : \"FILE\"}";
        post(getRepositoriesResourcePath(), json, "reader", "password", Optional.of("v1"), Optional.of(forbiddenResponseSpecification()));
    }

    @Test
    public void shouldReturnOkOnGetRepositoriesWithResultsUsingPagination() throws Exception {
        String resourcePath = getRepositoriesResourcePath();

        for (int i = 0; i < 30; i++) {
            String json = "{ \"name\" : \"test" + i + "\", \"type\" : \"FILE\"}";
            post(resourcePath, json, "editor", "password", Optional.of("v1"), Optional.empty());
        }

        ResponseSpecBuilder builder = new ResponseSpecBuilder();
        builder.expectStatusCode(200);
        builder.expectBody("page", equalTo(1));
        builder.expectBody("per_page", equalTo(10));
        builder.expectBody("total_pages", equalTo(3));
        builder.expectBody("total_count", equalTo(30));
        builder.expectBody("data", hasSize(10));
        builder.expectBody("links.first.href", equalTo(resourcePath + "?page=1&per_page=10"));
        builder.expectBody("links.last.href", equalTo(resourcePath + "?page=3&per_page=10"));
        builder.expectBody("links.next.href", equalTo(resourcePath + "?page=2&per_page=10"));
        com.jayway.restassured.response.Response response = get(resourcePath, "reader", "password", Optional.of("v1"), Optional.of(builder.build()));
        // Validate Link Headers
        assertThat(response.headers().getList("Link")).extracting("value")
                .containsExactly("<" + resourcePath + "?page=1&per_page=10>; rel=\"first\"", "<" + resourcePath + "?page=3&per_page=10>; rel=\"last\"",
                        "<" + resourcePath + "?page=2&per_page=10>; rel=\"next\"");

        builder = new ResponseSpecBuilder();
        builder.expectStatusCode(200);
        builder.expectBody("page", equalTo(2));
        builder.expectBody("per_page", equalTo(10));
        builder.expectBody("total_pages", equalTo(3));
        builder.expectBody("total_count", equalTo(30));
        builder.expectBody("data", hasSize(10));
        builder.expectBody("links.first.href", equalTo(resourcePath + "?page=1&per_page=10"));
        builder.expectBody("links.last.href", equalTo(resourcePath + "?page=3&per_page=10"));
        builder.expectBody("links.next.href", equalTo(resourcePath + "?page=3&per_page=10"));
        builder.expectBody("links.prev.href", equalTo(resourcePath + "?page=1&per_page=10"));
        response = get(resourcePath + "?page=2&per_page=10", "reader", "password", Optional.of("v1"), Optional.of(builder.build()));
        // Validate Link Headers
        assertThat(response.headers().getList("Link")).extracting("value")
                .containsExactly("<" + resourcePath + "?page=1&per_page=10>; rel=\"first\"", "<" + resourcePath + "?page=3&per_page=10>; rel=\"last\"",
                        "<" + resourcePath + "?page=1&per_page=10>; rel=\"prev\"", "<" + resourcePath + "?page=3&per_page=10>; rel=\"next\"");


        builder = new ResponseSpecBuilder();
        builder.expectStatusCode(200);
        builder.expectBody("page", equalTo(3));
        builder.expectBody("per_page", equalTo(10));
        builder.expectBody("total_pages", equalTo(3));
        builder.expectBody("total_count", equalTo(30));
        builder.expectBody("data", hasSize(10));
        builder.expectBody("links.first.href", equalTo(resourcePath + "?page=1&per_page=10"));
        builder.expectBody("links.last.href", equalTo(resourcePath + "?page=3&per_page=10"));
        builder.expectBody("links.prev.href", equalTo(resourcePath + "?page=2&per_page=10"));
        response = get(resourcePath + "?page=3&per_page=10", "reader", "password", Optional.of("v1"), Optional.of(builder.build()));
        // Validate Link Headers
        assertThat(response.headers().getList("Link")).extracting("value")
                .containsExactly("<" + resourcePath + "?page=1&per_page=10>; rel=\"first\"", "<" + resourcePath + "?page=3&per_page=10>; rel=\"last\"",
                        "<" + resourcePath + "?page=2&per_page=10>; rel=\"prev\"");

        builder = new ResponseSpecBuilder();
        builder.expectStatusCode(200);
        builder.expectBody("page", equalTo(4));
        builder.expectBody("per_page", equalTo(10));
        builder.expectBody("total_pages", equalTo(3));
        builder.expectBody("total_count", equalTo(30));
        builder.expectBody("data", hasSize(0));
        builder.expectBody("links.first.href", equalTo(resourcePath + "?page=1&per_page=10"));
        builder.expectBody("links.prev", nullValue());
        builder.expectBody("links.next", nullValue());
        response = get(resourcePath + "?page=4&per_page=10", "reader", "password", Optional.of("v1"), Optional.of(builder.build()));
        // Validate Link Headers
        assertThat(response.headers().getList("Link")).extracting("value")
                .containsExactly("<" + resourcePath + "?page=1&per_page=10>; rel=\"first\"", "<" + resourcePath + "?page=3&per_page=10>; rel=\"last\"");
    }

    @Test
    public void shouldReturnOkOnGetRepositoriesWithResultsUsingOnlyCount() throws Exception {
        String resourcePath = getRepositoriesResourcePath();

        for (int i = 0; i < 30; i++) {
            String json = "{ \"name\" : \"test" + i + "\", \"type\" : \"FILE\"}";
            post(resourcePath, json, "editor", "password", Optional.of("v1"), Optional.empty());
        }

        ResponseSpecBuilder responseSpecBuilder = new ResponseSpecBuilder();
        responseSpecBuilder.expectStatusCode(200);
        responseSpecBuilder.expectBody("page", nullValue());
        responseSpecBuilder.expectBody("per_page", nullValue());
        responseSpecBuilder.expectBody("total_pages", nullValue());
        responseSpecBuilder.expectBody("total_count", equalTo(30));
        responseSpecBuilder.expectBody("data", hasSize(0));
        responseSpecBuilder.expectBody("links", nullValue());
        com.jayway.restassured.response.Response response = get(resourcePath + "?only_count=true", "reader", "password", Optional.of("v1"), Optional.of(responseSpecBuilder.build()));
        // Validate Link Headers
        assertThat(response.header("Link")).isNull();
    }

    @Test
    public void shouldReturnOkOnGetRepositoriesDefaultSort() throws Exception {
        String resourcePath = getRepositoriesResourcePath();

        for (int i = 0; i < 5; i++) {
            String json = "{ \"name\" : \"test" + i + "\", \"type\" : \"FILE\"}";
            post(resourcePath, json, "editor", "password", Optional.of("v1"), Optional.empty());
        }

        ResponseSpecBuilder responseSpecBuilder = new ResponseSpecBuilder();
        responseSpecBuilder.expectStatusCode(200);
        responseSpecBuilder.expectBody("data.name", contains("test4", "test3", "test2", "test1", "test0"));
        get(resourcePath, "reader", "password", Optional.of("v1"), Optional.of(responseSpecBuilder.build()));
    }

    @Test
    public void shouldReturnOkOnGetRepositoriesSortNameAsc() throws Exception {
        String resourcePath = getRepositoriesResourcePath();

        for (int i = 0; i < 5; i++) {
            String json = "{ \"name\" : \"test" + i + "\", \"type\" : \"FILE\"}";
            post(resourcePath, json, "editor", "password", Optional.of("v1"), Optional.empty());
        }

        ResponseSpecBuilder responseSpecBuilder = new ResponseSpecBuilder();
        responseSpecBuilder.expectStatusCode(200);
        responseSpecBuilder.expectBody("data.name", contains("test0", "test1", "test2", "test3", "test4"));
        get(resourcePath + "?sort=name&order=ASC", "reader", "password", Optional.of("v1"), Optional.of(responseSpecBuilder.build()));
    }

    @Test
    public void shouldReturnOkOnGetRepositoriesFilteredByNameEqual() throws Exception {
        String resourcePath = getRepositoriesResourcePath();

        for (int i = 0; i < 5; i++) {
            String json = "{ \"name\" : \"test" + i + "\", \"type\" : \"FILE\"}";
            post(resourcePath, json, "editor", "password", Optional.of("v1"), Optional.empty());
        }

        ResponseSpecBuilder responseSpecBuilder = new ResponseSpecBuilder();
        responseSpecBuilder.expectStatusCode(200);
        responseSpecBuilder.expectBody("data.name", contains("test0"));
        get(resourcePath + "?name[eq]=test0", "reader", "password", Optional.of("v1"), Optional.of(responseSpecBuilder.build()));
    }

    @Test
    public void shouldReturnBadRequestOnGetRepositoriesFilteredByInvalidDateFormat() throws Exception {
        ResponseSpecBuilder responseSpecBuilder = new ResponseSpecBuilder();
        responseSpecBuilder.expectStatusCode(200);
        responseSpecBuilder.expectBody("data.name", contains("test"));
        get(getRepositoriesResourcePath() + "?created[eq]=3", "reader", "password", Optional.of("v1"),
                Optional.of(badRequestResponseSpecification(Optional.of("Invalid date format for attribute created"))));
    }

    @Test
    public void shouldReturnBadRequestOnGetRepositoriesFilteredUnsupportedOperator() throws Exception {
        ResponseSpecBuilder responseSpecBuilder = new ResponseSpecBuilder();
        responseSpecBuilder.expectStatusCode(200);
        responseSpecBuilder.expectBody("data.name", contains("test"));
        get(getRepositoriesResourcePath() + "?name[ne]=bad", "reader", "password", Optional.of("v1"),
                Optional.of(badRequestResponseSpecification(Optional.of("Invalid search condition operator NE for condition attribute name"))));
    }

    @Test
    public void shouldReturnOkOnGetRepository() throws Exception {
        String json = "{ \"name\" : \"test1\", \"type\" : \"FILE\"}";
        String repositoryId =
                post(getRepositoriesResourcePath(), json, "editor", "password", Optional.of("v1"), Optional.empty()).header("Location").replace(getRepositoriesResourcePath() + "/", "");
        get(getRepositoriesResourcePath("{repositoryId}", repositoryId), "reader", "password", Optional.of("v1"),
                Optional.of(getRepositoryOkResponseSpecification(repositoryId, "test1", "FILE", "editor", "editor")));
    }

    @Test
    public void shouldReturnNotFoundOnGetRepository() throws Exception {
        get(getRepositoriesResourcePath("{repositoryId}", UUID.randomUUID().toString()), "reader", "password", Optional.of("v1"), Optional.of(notFoundResponseSpecification()));
    }

    @Test
    public void shouldReturnNotModifiedOnGETWithEtag() throws Exception {
        String json = "{ \"name\" : \"test1\", \"type\" : \"FILE\"}";
        String repositoryId =
                post(getRepositoriesResourcePath(), json, "editor", "password", Optional.of("v1"), Optional.empty()).header("Location").replace(getRepositoriesResourcePath() + "/", "");
        com.jayway.restassured.response.Response response = get(getRepositoriesResourcePath("{repositoryId}", repositoryId), "reader", "password", Optional.of("v1"),
                Optional.of(getRepositoryOkResponseSpecification(repositoryId, "test1", "FILE", "editor", "editor")));
        String etag = response.getHeader("ETag");
        given().spec(requestSpecification("reader", "password", Optional.of("v1"))).header("If-None-Match", etag).then().statusCode(Response.Status.NOT_MODIFIED.getStatusCode()).when()
                .header("ETag", notNullValue()).get(getRepositoriesResourcePath("{repositoryId}", repositoryId)).then().assertThat().body(isEmptyOrNullString());
    }

    @Test
    public void shouldReturnOkOnDeleteRepository() throws Exception {
        String json = "{ \"name\" : \"test1\", \"type\" : \"FILE\"}";
        String repositoryId =
                post(getRepositoriesResourcePath(), json, "editor", "password", Optional.of("v1"), Optional.empty()).header("Location").replace(getRepositoriesResourcePath() + "/", "");
        delete(getRepositoriesResourcePath("{repositoryId}", repositoryId), "editor", "password", Optional.of("v1"), Optional.empty());
    }

    @Test
    public void shouldReturnNotFoundOnDeleteRepository() throws Exception {
        delete(getRepositoriesResourcePath("{repositoryId}", UUID.randomUUID().toString()), "editor", "password", Optional.of("v1"), Optional.of(notFoundResponseSpecification()));
    }

    @Test
    public void shouldReturnForbiddenOnDeleteRepository() throws Exception {
        String json = "{ \"name\" : \"test1\", \"type\" : \"FILE\"}";
        String repositoryId =
                post(getRepositoriesResourcePath(), json, "editor", "password", Optional.of("v1"), Optional.empty()).header("Location").replace(getRepositoriesResourcePath() + "/", "");
        delete(getRepositoriesResourcePath("{repositoryId}", repositoryId), "reader", "password", Optional.of("v1"), Optional.of(forbiddenResponseSpecification()));
    }

    @Test
    public void shouldReturnOkOnGetRepositoryConfigurationWhenUnConfigured() throws Exception {
        String json = "{ \"name\" : \"test1\", \"type\" : \"FILE\"}";
        String repositoryId =
                post(getRepositoriesResourcePath(), json, "editor", "password", Optional.of("v1"), Optional.empty()).header("Location").replace(getRepositoriesResourcePath() + "/", "");
        get(getRepositoryConfigurationResourceUrlAsString(repositoryId), "reader", "password", Optional.of("v1"), Optional.empty());
    }

    @Test
    public void shouldReturnOkOnPutRepositoryConfiguration() throws Exception {
        String json = "{ \"name\" : \"test1\", \"type\" : \"FILE\"}";
        String repositoryId =
                post(getRepositoriesResourcePath(), json, "editor", "password", Optional.of("v1"), Optional.empty()).header("Location").replace(getRepositoriesResourcePath() + "/", "");
        String configJson = "{\"root\" : \"/test\"}";
        put(getRepositoryConfigurationResourceUrlAsString(repositoryId), configJson, "editor", "password", Optional.of("v1"), Optional.empty());

        ResponseSpecBuilder builder = new ResponseSpecBuilder();
        builder.expectStatusCode(200);
        builder.expectBody("root", equalTo("/test"));
        get(getRepositoryConfigurationResourceUrlAsString(repositoryId), "reader", "password", Optional.of("v1"), Optional.of(builder.build()));
    }

    @Test
    public void shouldReturnNotFoundOnPutRepositoryConfiguration() {
        String updateJson = "{ \"name\" : \"test2\", \"external_ids\" : [\"id1\", \"id2\", \"id3\"]}";
        put(getRepositoryConfigurationResourceUrlAsString("bad-repo"), updateJson, "admin", "password", Optional.of("v1"), Optional.of(notFoundResponseSpecification()));
    }

    @Test
    public void shouldReturnForbiddenOnPutRepositoryConfiguration() {
        String json = "{ \"name\" : \"test1\", \"type\" : \"FILE\"}";
        String repositoryId =
                post(getRepositoriesResourcePath(), json, "editor", "password", Optional.of("v1"), Optional.empty()).header("Location").replace(getRepositoriesResourcePath() + "/", "");
        String configJson = "{\"root\" : \"/test\"}";
        put(getRepositoryConfigurationResourceUrlAsString(repositoryId), configJson, "reader", "password", Optional.of("v1"), Optional.of(forbiddenResponseSpecification()));
    }

    @Test
    public void shouldReturnBadRequestOnPutRepositoryConfiguration() {
        String json = "{ \"name\" : \"test1\", \"type\" : \"FILE\"}";
        String repositoryId =
                post(getRepositoriesResourcePath(), json, "editor", "password", Optional.of("v1"), Optional.empty()).header("Location").replace(getRepositoriesResourcePath() + "/", "");
        String configJson = "{\"bad\" : \"/test\"}";
        put(getRepositoryConfigurationResourceUrlAsString(repositoryId), configJson, "reader", "password", Optional.of("v1"), Optional.of(badRequestResponseSpecification()));
    }

    @Test
    public void shouldReturnBadRequestOnPutRepositoryConfigurationWithEmptyBody() {
        String json = "{ \"name\" : \"test1\", \"type\" : \"FILE\"}";
        String repositoryId =
                post(getRepositoriesResourcePath(), json, "editor", "password", Optional.of("v1"), Optional.empty()).header("Location").replace(getRepositoriesResourcePath() + "/", "");
        String configJson = "{}";
        put(getRepositoryConfigurationResourceUrlAsString(repositoryId), configJson, "reader", "password", Optional.of("v1"), Optional.of(badRequestResponseSpecification()));
    }

    @Test
    public void shouldReturnOkOnGetRepositoryTemplates() throws Exception {
        ResponseSpecBuilder builder = new ResponseSpecBuilder();
        builder.expectStatusCode(200);
        builder.expectBody("type", containsInAnyOrder("GLACIER", "FILE", "S3", "FTP", "SFTP"));

        get(getRepositoryTemplateResourceUrlAsString(), "reader", "password", Optional.of("v1"), Optional.of(builder.build()));
    }

    @Test
    public void shouldReturnOkOnGetRepositoryTemplate() throws Exception {
        ResponseSpecBuilder builder = new ResponseSpecBuilder();
        builder.expectStatusCode(200);
        builder.expectBody("type", equalTo("FILE"));
        builder.expectBody("properties.name", contains("root"));

        get(getRepositoryTemplateResourceUrlAsString() + "/FILE", "reader", "password", Optional.of("v1"), Optional.of(builder.build()));
    }

    private String getRepositoriesResourcePath() {
        UriBuilder builder = UriBuilder.fromUri(Throwables.returnableInstance(baseURL::toURI));
        builder.path("api/repositories");
        return builder.build().toString();
    }

    private String getRepositoriesResourcePath(String path, Object pathParameters) {
        UriBuilder builder = UriBuilder.fromUri(Throwables.returnableInstance(baseURL::toURI));
        builder.path("api/repositories");
        builder.path(path);
        return builder.build(pathParameters).toString();
    }

    private String getRepositoryConfigurationResourceUrlAsString(String repositoryId) {
        UriBuilder builder = UriBuilder.fromUri(Throwables.returnableInstance(baseURL::toURI));
        builder.path("api/repositories/{repositoryId}/configuration");
        return builder.build(repositoryId).toString();
    }

    private String getRepositoryTemplateResourceUrlAsString() {
        UriBuilder builder = UriBuilder.fromUri(Throwables.returnableInstance(baseURL::toURI));
        builder.path("api/repositories/templates");
        return builder.build().toString();
    }

    private ResponseSpecification getRepositoryOkResponseSpecification(String id, String name, String type, String createdUsername, String modifiedUsername) {
        ResponseSpecBuilder builder = new ResponseSpecBuilder();
        builder.expectStatusCode(200);
        builder.expectBody("id", equalTo(id));
        builder.expectBody("name", equalTo(name));
        builder.expectBody("description", nullValue());
        builder.expectBody("type", equalTo(type));
        builder.expectBody("created", notNullValue(String.class));
        builder.expectBody("created_by", equalTo(createdUsername));
        builder.expectBody("last_modified", notNullValue(String.class));
        builder.expectBody("last_modified_by", equalTo(modifiedUsername));
        builder.expectBody("links.self.href", equalTo(getRepositoriesResourcePath() + "/" + id));
        builder.expectBody("links.configuration.href", equalTo(getRepositoriesResourcePath() + "/" + id + "/configuration"));
        builder.expectBody("links.template.href", equalTo(getRepositoriesResourcePath() + "/templates/" + type));
        return builder.build();
    }
}
