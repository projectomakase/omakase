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
package org.projectomakase.omakase.location;

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

import static com.jayway.restassured.RestAssured.given;
import static org.projectomakase.omakase.RestIntegrationTests.badRequestResponseSpecification;
import static org.projectomakase.omakase.RestIntegrationTests.delete;
import static org.projectomakase.omakase.RestIntegrationTests.forbiddenResponseSpecification;
import static org.projectomakase.omakase.RestIntegrationTests.get;
import static org.projectomakase.omakase.RestIntegrationTests.getLocationConfigurationResourcePath;
import static org.projectomakase.omakase.RestIntegrationTests.getLocationResourcePath;
import static org.projectomakase.omakase.RestIntegrationTests.notFoundResponseSpecification;
import static org.projectomakase.omakase.RestIntegrationTests.post;
import static org.projectomakase.omakase.RestIntegrationTests.put;
import static org.projectomakase.omakase.RestIntegrationTests.requestSpecification;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.isEmptyOrNullString;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

/**
 * Location V1 REST API Black Box Tests
 *
 * @author Richard Lucas
 */
@RunAsClient
@RunWith(Arquillian.class)
public class LocationV1RestIT {

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
    public void shouldReturnCreatedOnPostLocations() throws Exception {
        String json = "{ \"name\" : \"test1\", \"type\" : \"FILE\"}";
        post(getLocationResourcePath(baseURL), json, "editor", "password", Optional.of("v1"), Optional.empty());
    }

    @Test
    public void shouldReturnForbiddenOnPostLocationsWithUnauthorizedUser() throws Exception {
        String json = "{ \"name\" : \"test1\", \"type\" : \"FILE\"}";
        post(getLocationResourcePath(baseURL), json, "reader", "password", Optional.of("v1"), Optional.of(forbiddenResponseSpecification()));
    }

    @Test
    public void shouldReturnOkOnGetLocationsWithResultsUsingPagination() throws Exception {
        String resourcePath = getLocationResourcePath(baseURL);

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
    public void shouldReturnOkOnGetLocationsWithResultsUsingOnlyCount() throws Exception {
        String resourcePath = getLocationResourcePath(baseURL);

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
    public void shouldReturnOkOnGetLocationsDefaultSort() throws Exception {
        String resourcePath = getLocationResourcePath(baseURL);

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
    public void shouldReturnOkOnGetLocationsSortNameAsc() throws Exception {
        String resourcePath = getLocationResourcePath(baseURL);

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
    public void shouldReturnOkOnGetLocationsFilteredByNameEqual() throws Exception {
        String resourcePath = getLocationResourcePath(baseURL);

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
    public void shouldReturnBadRequestOnGetLocationsFilteredByInvalidDateFormat() throws Exception {
        ResponseSpecBuilder responseSpecBuilder = new ResponseSpecBuilder();
        responseSpecBuilder.expectStatusCode(200);
        responseSpecBuilder.expectBody("data.name", contains("test"));
        get(getLocationResourcePath(baseURL) + "?created[eq]=3", "reader", "password", Optional.of("v1"),
                Optional.of(badRequestResponseSpecification(Optional.of("Invalid date format for attribute created"))));
    }

    @Test
    public void shouldReturnBadRequestOnGetLocationsFilteredUnsupportedOperator() throws Exception {
        ResponseSpecBuilder responseSpecBuilder = new ResponseSpecBuilder();
        responseSpecBuilder.expectStatusCode(200);
        responseSpecBuilder.expectBody("data.name", contains("test"));
        get(getLocationResourcePath(baseURL) + "?name[ne]=bad", "reader", "password", Optional.of("v1"),
                Optional.of(badRequestResponseSpecification(Optional.of("Invalid search condition operator NE for condition attribute name"))));
    }

    @Test
    public void shouldReturnOkOnGetLocation() throws Exception {
        String json = "{ \"name\" : \"test1\", \"type\" : \"FILE\"}";
        String locationId =
                post(getLocationResourcePath(baseURL), json, "editor", "password", Optional.of("v1"), Optional.empty()).header("Location").replace(getLocationResourcePath(baseURL) + "/", "");
        get(getLocationResourcePath(baseURL, "{locationId}", locationId), "reader", "password", Optional.of("v1"),
                Optional.of(getLocationOkResponseSpecification(locationId, "test1", "FILE", "editor", "editor")));
    }

    @Test
    public void shouldReturnNotFoundOnGetLocation() throws Exception {
        get(getLocationResourcePath(baseURL, "{locationId}", UUID.randomUUID().toString()), "reader", "password", Optional.of("v1"), Optional.of(notFoundResponseSpecification()));
    }

    @Test
    public void shouldReturnNotModifiedOnGETWithEtag() throws Exception {
        String json = "{ \"name\" : \"test1\", \"type\" : \"FILE\"}";
        String locationId =
                post(getLocationResourcePath(baseURL), json, "editor", "password", Optional.of("v1"), Optional.empty()).header("Location").replace(getLocationResourcePath(baseURL) + "/", "");
        com.jayway.restassured.response.Response response = get(getLocationResourcePath(baseURL, "{locationId}", locationId), "reader", "password", Optional.of("v1"),
                Optional.of(getLocationOkResponseSpecification(locationId, "test1", "FILE", "editor", "editor")));
        String etag = response.getHeader("ETag");
        given().spec(requestSpecification("reader", "password", Optional.of("v1"))).header("If-None-Match", etag).then().statusCode(Response.Status.NOT_MODIFIED.getStatusCode()).when()
                .header("ETag", notNullValue()).get(getLocationResourcePath(baseURL, "{locationId}", locationId)).then().assertThat().body(isEmptyOrNullString());
    }

    @Test
    public void shouldReturnOkOnDeleteLocation() throws Exception {
        String json = "{ \"name\" : \"test1\", \"type\" : \"FILE\"}";
        String locationId =
                post(getLocationResourcePath(baseURL), json, "editor", "password", Optional.of("v1"), Optional.empty()).header("Location").replace(getLocationResourcePath(baseURL) + "/", "");
        delete(getLocationResourcePath(baseURL, "{locationId}", locationId), "editor", "password", Optional.of("v1"), Optional.empty());
    }

    @Test
    public void shouldReturnNotFoundOnDeleteLocation() throws Exception {
        delete(getLocationResourcePath(baseURL, "{locationId}", UUID.randomUUID().toString()), "editor", "password", Optional.of("v1"), Optional.of(notFoundResponseSpecification()));
    }

    @Test
    public void shouldReturnForbiddenOnDeleteLocation() throws Exception {
        String json = "{ \"name\" : \"test1\", \"type\" : \"FILE\"}";
        String locationId =
                post(getLocationResourcePath(baseURL), json, "editor", "password", Optional.of("v1"), Optional.empty()).header("Location").replace(getLocationResourcePath(baseURL) + "/", "");
        delete(getLocationResourcePath(baseURL, "{locationId}", locationId), "reader", "password", Optional.of("v1"), Optional.of(forbiddenResponseSpecification()));
    }

    @Test
    public void shouldReturnOkOnGetLocationConfigurationWhenUnConfigured() throws Exception {
        String json = "{ \"name\" : \"test1\", \"type\" : \"FILE\"}";
        String locationId =
                post(getLocationResourcePath(baseURL), json, "editor", "password", Optional.of("v1"), Optional.empty()).header("Location").replace(getLocationResourcePath(baseURL) + "/", "");
        get(getLocationConfigurationResourcePath(baseURL, locationId), "reader", "password", Optional.of("v1"), Optional.empty());
    }

    @Test
    public void shouldReturnOkOnPutLocationConfiguration() throws Exception {
        String json = "{ \"name\" : \"test1\", \"type\" : \"FILE\"}";
        String locationId =
                post(getLocationResourcePath(baseURL), json, "editor", "password", Optional.of("v1"), Optional.empty()).header("Location").replace(getLocationResourcePath(baseURL) + "/", "");
        String configJson = "{\"root\" : \"/test\"}";
        put(getLocationConfigurationResourcePath(baseURL, locationId), configJson, "editor", "password", Optional.of("v1"), Optional.empty());

        ResponseSpecBuilder builder = new ResponseSpecBuilder();
        builder.expectStatusCode(200);
        builder.expectBody("root", equalTo("/test"));
        get(getLocationConfigurationResourcePath(baseURL, locationId), "reader", "password", Optional.of("v1"), Optional.of(builder.build()));
    }

    @Test
    public void shouldReturnNotFoundOnPutLocationConfiguration() {
        String updateJson = "{ \"name\" : \"test2\", \"external_ids\" : [\"id1\", \"id2\", \"id3\"]}";
        put(getLocationConfigurationResourcePath(baseURL, "bad-location"), updateJson, "admin", "password", Optional.of("v1"), Optional.of(notFoundResponseSpecification()));
    }

    @Test
    public void shouldReturnForbiddenOnPutLocationConfiguration() {
        String json = "{ \"name\" : \"test1\", \"type\" : \"FILE\"}";
        String locationId =
                post(getLocationResourcePath(baseURL), json, "editor", "password", Optional.of("v1"), Optional.empty()).header("Location").replace(getLocationResourcePath(baseURL) + "/", "");
        String configJson = "{\"root\" : \"/test\"}";
        put(getLocationConfigurationResourcePath(baseURL, locationId), configJson, "reader", "password", Optional.of("v1"), Optional.of(forbiddenResponseSpecification()));
    }

    @Test
    public void shouldReturnBadRequestOnPutLocationConfiguration() {
        String json = "{ \"name\" : \"test1\", \"type\" : \"FILE\"}";
        String locationId =
                post(getLocationResourcePath(baseURL), json, "editor", "password", Optional.of("v1"), Optional.empty()).header("Location").replace(getLocationResourcePath(baseURL) + "/", "");
        String configJson = "{\"bad\" : \"/test\"}";
        put(getLocationConfigurationResourcePath(baseURL, locationId), configJson, "reader", "password", Optional.of("v1"), Optional.of(badRequestResponseSpecification()));
    }

    @Test
    public void shouldReturnBadRequestOnPutLocationConfigurationWithEmptyBody() {
        String json = "{ \"name\" : \"test1\", \"type\" : \"FILE\"}";
        String locationId =
                post(getLocationResourcePath(baseURL), json, "editor", "password", Optional.of("v1"), Optional.empty()).header("Location").replace(getLocationResourcePath(baseURL) + "/", "");
        String configJson = "{}";
        put(getLocationConfigurationResourcePath(baseURL, locationId), configJson, "reader", "password", Optional.of("v1"), Optional.of(badRequestResponseSpecification()));
    }

    @Test
    public void shouldReturnOkOnGetLocationTemplates() throws Exception {
        ResponseSpecBuilder builder = new ResponseSpecBuilder();
        builder.expectStatusCode(200);
        builder.expectBody("type", containsInAnyOrder("FILE", "FTP", "SFTP", "HTTP", "HTTPS", "S3"));

        get(getLocationTemplateResourceUrlAsString(), "reader", "password", Optional.of("v1"), Optional.of(builder.build()));
    }

    @Test
    public void shouldReturnOkOnGetLocationTemplate() throws Exception {
        ResponseSpecBuilder builder = new ResponseSpecBuilder();
        builder.expectStatusCode(200);
        builder.expectBody("type", equalTo("FILE"));
        builder.expectBody("properties.name", contains("root"));

        get(getLocationTemplateResourceUrlAsString() + "/FILE", "reader", "password", Optional.of("v1"), Optional.of(builder.build()));
    }

    private String getLocationTemplateResourceUrlAsString() {
        UriBuilder builder = UriBuilder.fromUri(Throwables.returnableInstance(baseURL::toURI));
        builder.path("api/locations/templates");
        return builder.build().toString();
    }

    private ResponseSpecification getLocationOkResponseSpecification(String id, String name, String type, String createdUsername, String modifiedUsername) {
        String resourcePath = getLocationResourcePath(baseURL);
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
        builder.expectBody("links.self.href", equalTo(resourcePath + "/" + id));
        builder.expectBody("links.configuration.href", equalTo(resourcePath + "/" + id + "/configuration"));
        builder.expectBody("links.template.href", equalTo(resourcePath + "/templates/" + type));
        return builder.build();
    }
}
