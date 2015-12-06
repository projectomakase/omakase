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

import com.jayway.restassured.builder.RequestSpecBuilder;
import com.jayway.restassured.builder.ResponseSpecBuilder;
import com.jayway.restassured.http.ContentType;
import com.jayway.restassured.response.Response;
import com.jayway.restassured.specification.RequestSpecification;
import com.jayway.restassured.specification.ResponseSpecification;
import org.projectomakase.omakase.commons.functions.Throwables;

import javax.ws.rs.core.UriBuilder;
import java.net.URL;
import java.util.Optional;

import static com.jayway.restassured.RestAssured.given;
import static com.jayway.restassured.path.json.JsonPath.from;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.startsWith;

/**
 * Provides common functionality for the the REST Integration Tests.
 *
 * @author Richard Lucas
 */
public class RestIntegrationTests {

    private RestIntegrationTests() {
        // hides the implicit public constructor
    }

    /**
     * Performs a POST request against the specified path and returns a {@link com.jayway.restassured.response.Response}.
     *
     * @param path
     *         the path
     * @param json
     *         the requests JSON payload
     * @param username
     *         the username used to authenticate the request
     * @param password
     *         the password used to authenticate the request
     * @param version
     *         an Optional containing the request api version to use or empty if the default version should be used.
     * @param responseSpecification
     *         an Optional containing the {@link com.jayway.restassured.specification.ResponseSpecification} that should be used to validate the response or empty if the default specification should
     *         be
     *         used.
     * @return a {@link com.jayway.restassured.response.Response}
     */
    public static Response post(String path, String json, String username, String password, Optional<String> version, Optional<ResponseSpecification> responseSpecification) {
        ResponseSpecBuilder defaultSpecBuilder = new ResponseSpecBuilder();
        defaultSpecBuilder.expectStatusCode(201);
        defaultSpecBuilder.expectHeader("Location", startsWith(path));
        defaultSpecBuilder.expectBody("status", equalTo("OK"));

        Response response = given().spec(requestSpecification(username, password, version)).content(json).when().post(path);
        response.then().spec(responseSpecification.orElse(defaultSpecBuilder.build()));
        return response;
    }

    public static Response post(String path, String json, RequestSpecification requestSpecification, Optional<ResponseSpecification> responseSpecification) {
        ResponseSpecBuilder defaultSpecBuilder = new ResponseSpecBuilder();
        defaultSpecBuilder.expectStatusCode(201);
        defaultSpecBuilder.expectHeader("Location", startsWith(path));
        defaultSpecBuilder.expectBody("status", equalTo("OK"));

        Response response = given().spec(requestSpecification).content(json).when().post(path);
        response.then().spec(responseSpecification.orElse(defaultSpecBuilder.build()));
        return response;
    }

    /**
     * Performs a GET request against the specified path and returns a {@link com.jayway.restassured.response.Response}.
     *
     * @param path
     *         the path
     * @param username
     *         the username used to authenticate the request
     * @param password
     *         the password used to authenticate the request
     * @param version
     *         an Optional containing the request api version to use or empty if the default version should be used.
     * @param responseSpecification
     *         an Optional containing the {@link com.jayway.restassured.specification.ResponseSpecification} that should be used to validate the response or empty if the default specification should
     *         be
     *         used.
     * @return a {@link com.jayway.restassured.response.Response}
     */
    public static Response get(String path, String username, String password, Optional<String> version, Optional<ResponseSpecification> responseSpecification) {
        ResponseSpecBuilder defaultSpecBuilder = new ResponseSpecBuilder();
        defaultSpecBuilder.expectStatusCode(200);

        Response response = given().spec(requestSpecification(username, password, version)).when().get(path);
        response.then().spec(responseSpecification.orElse(defaultSpecBuilder.build()));
        return response;
    }

    /**
     * Performs a PUT request against the specified path and returns a {@link com.jayway.restassured.response.Response}.
     *
     * @param path
     *         the path
     * @param json
     *         the requests JSON payload
     * @param username
     *         the username used to authenticate the request
     * @param password
     *         the password used to authenticate the request
     * @param version
     *         an Optional containing the request api version to use or empty if the default version should be used.
     * @param responseSpecification
     *         an Optional containing the {@link com.jayway.restassured.specification.ResponseSpecification} that should be used to validate the response or empty if the default specification should
     *         be
     *         used.
     * @return a {@link com.jayway.restassured.response.Response}
     */
    public static Response put(String path, String json, String username, String password, Optional<String> version, Optional<ResponseSpecification> responseSpecification) {
        ResponseSpecBuilder defaultSpecBuilder = new ResponseSpecBuilder();
        defaultSpecBuilder.expectStatusCode(200);
        defaultSpecBuilder.expectBody("status", equalTo("OK"));

        Response response = given().spec(requestSpecification(username, password, version)).content(json).when().put(path);
        response.then().spec(responseSpecification.orElse(defaultSpecBuilder.build()));
        return response;
    }

    /**
     * Performs a PATCH request against the specified path and returns a {@link com.jayway.restassured.response.Response}.
     *
     * @param path
     *         the path
     * @param json
     *         the requests JSON payload
     * @param username
     *         the username used to authenticate the request
     * @param password
     *         the password used to authenticate the request
     * @param version
     *         an Optional containing the request api version to use or empty if the default version should be used.
     * @param responseSpecification
     *         an Optional containing the {@link com.jayway.restassured.specification.ResponseSpecification} that should be used to validate the response or empty if the default specification should
     *         be
     *         used.
     * @return a {@link com.jayway.restassured.response.Response}
     */
    public static Response patch(String path, String json, String username, String password, Optional<String> version, Optional<ResponseSpecification> responseSpecification) {
        ResponseSpecBuilder defaultSpecBuilder = new ResponseSpecBuilder();
        defaultSpecBuilder.expectStatusCode(200);
        defaultSpecBuilder.expectBody("status", equalTo("OK"));

        Response response = given().spec(patchRequestSpecification(username, password, version)).content(json).when().patch(path);
        response.then().spec(responseSpecification.orElse(defaultSpecBuilder.build()));
        return response;
    }

    /**
     * Performs a DELETE request against the specified path and returns a {@link com.jayway.restassured.response.Response}.
     *
     * @param path
     *         the path
     * @param username
     *         the username used to authenticate the request
     * @param password
     *         the password used to authenticate the request
     * @param version
     *         an Optional containing the request api version to use or empty if the default version should be used.
     * @param responseSpecification
     *         an Optional containing the {@link com.jayway.restassured.specification.ResponseSpecification} that should be used to validate the response or empty if the default specification should
     *         be
     *         used.
     * @return a {@link com.jayway.restassured.response.Response}
     */
    public static Response delete(String path, String username, String password, Optional<String> version, Optional<ResponseSpecification> responseSpecification) {
        ResponseSpecBuilder defaultSpecBuilder = new ResponseSpecBuilder();
        defaultSpecBuilder.expectStatusCode(200);
        defaultSpecBuilder.expectBody("status", equalTo("OK"));

        Response response = given().spec(requestSpecification(username, password, version)).when().delete(path);
        response.then().spec(responseSpecification.orElse(defaultSpecBuilder.build()));
        return response;
    }

    /**
     * Returns the default {@link com.jayway.restassured.specification.ResponseSpecification} used when submitting requests.
     *
     * @param username
     *         the username used to authenticate the request
     * @param password
     *         the password used to authenticate the request
     * @param version
     *         an Optional containing the request api version to use or empty if the default version should be used.
     * @return the default {@link com.jayway.restassured.specification.ResponseSpecification} used when submitting requests.
     */
    public static RequestSpecification requestSpecification(String username, String password, Optional<String> version) {
        RequestSpecBuilder builder = new RequestSpecBuilder();
        String contentType = ContentType.JSON.toString();
        if (version.isPresent()) {
            contentType = "application/" + version.get() + "+json";
        }
        builder.addHeader("Accept", "application/json");
        builder.addRequestSpecification(given().auth().preemptive().basic(username, password).contentType(contentType));
        return builder.build();
    }

    /**
     * Returns the PATCH {@link com.jayway.restassured.specification.ResponseSpecification} used when submitting PATCH requests.
     *
     * @param username
     *         the username used to authenticate the request
     * @param password
     *         the password used to authenticate the request
     * @param version
     *         an Optional containing the request api version to use or empty if the default version should be used.
     * @return the PATCH {@link com.jayway.restassured.specification.ResponseSpecification} used when submitting PATCH requests.
     */
    public static RequestSpecification patchRequestSpecification(String username, String password, Optional<String> version) {
        RequestSpecBuilder builder = new RequestSpecBuilder();
        String contentType = "application/json-patch+json";
        if (version.isPresent()) {
            contentType = "application/json-patch." + version.get() + "+json";
        }
        builder.addHeader("Accept", "application/json");
        builder.addRequestSpecification(given().auth().preemptive().basic(username, password).contentType(contentType));
        return builder.build();
    }

    /**
     * Returns a {@link com.jayway.restassured.specification.ResponseSpecification} that can be used to validate a 404 NOT FOUND was returned.
     *
     * @return a {@link com.jayway.restassured.specification.ResponseSpecification} that can be used to validate a 404 NOT FOUND was returned.
     */
    public static ResponseSpecification notFoundResponseSpecification() {
        ResponseSpecBuilder builder = new ResponseSpecBuilder();
        builder.expectStatusCode(404);
        builder.expectHeader("Warning", "HTTP 404 Not Found");
        builder.expectBody("status", equalTo("ERROR"));
        builder.expectBody("message", equalTo("HTTP 404 Not Found"));
        return builder.build();
    }

    /**
     * Returns a {@link com.jayway.restassured.specification.ResponseSpecification} that can be used to validate a 403 NOT AUTHORIZED was returned.
     *
     * @return a {@link com.jayway.restassured.specification.ResponseSpecification} that can be used to validate a 403 NOT AUTHORIZED was returned.
     */
    public static ResponseSpecification forbiddenResponseSpecification() {
        ResponseSpecBuilder builder = new ResponseSpecBuilder();
        builder.expectStatusCode(403);
        builder.expectHeader("Warning", "Not authorized");
        builder.expectBody("status", equalTo("ERROR"));
        builder.expectBody("message", equalTo("Not authorized"));
        return builder.build();
    }

    /**
     * Returns a {@link com.jayway.restassured.specification.ResponseSpecification} that can be used to validate a 403 was returned.
     *
     * @param message
     *         the expected message
     * @return a {@link com.jayway.restassured.specification.ResponseSpecification} that can be used to validate a 403 was returned.
     */
    public static ResponseSpecification forbiddenResponseSpecification(String message) {
        ResponseSpecBuilder builder = new ResponseSpecBuilder();
        builder.expectStatusCode(403);
        builder.expectHeader("Warning", message);
        builder.expectBody("status", equalTo("ERROR"));
        builder.expectBody("message", equalTo(message));
        return builder.build();
    }

    /**
     * Returns a {@link com.jayway.restassured.specification.ResponseSpecification} that can be used to validate a 400 BAD REQUEST was returned.
     *
     * @return a {@link com.jayway.restassured.specification.ResponseSpecification} that can be used to validate a 400 BAD REQUEST was returned.
     */
    public static ResponseSpecification badRequestResponseSpecification() {
        return badRequestResponseSpecification(Optional.empty());
    }

    /**
     * Returns a {@link com.jayway.restassured.specification.ResponseSpecification} that can be used to validate a 400 BAD REQUEST was returned.
     *
     * @param message
     *         an Optional containing the expected error message, if empty the message will be validated to make sure it is not null.
     * @return a {@link com.jayway.restassured.specification.ResponseSpecification} that can be used to validate a 400 BAD REQUEST was returned.
     */
    public static ResponseSpecification badRequestResponseSpecification(Optional<String> message) {
        ResponseSpecBuilder builder = new ResponseSpecBuilder();
        builder.expectStatusCode(400);
        builder.expectHeader("Warning", notNullValue(String.class));
        builder.expectBody("status", equalTo("ERROR"));
        if (message.isPresent()) {
            builder.expectBody("message", equalTo(message.get()));
        } else {
            builder.expectBody("message", notNullValue(String.class));
        }
        return builder.build();
    }

    /**
     * Returns a {@link com.jayway.restassured.specification.ResponseSpecification} that can be used to validate a 501 NOT IMPLEMENTED was returned.
     *
     * @param message
     *         an Optional containing the expected error message, if empty the message will be validated to make sure it is not null.
     * @return a {@link com.jayway.restassured.specification.ResponseSpecification} that can be used to validate a 501 NOT IMPLEMENTED was returned.
     */
    public static ResponseSpecification notImplementedResponseSpecification(Optional<String> message) {
        ResponseSpecBuilder builder = new ResponseSpecBuilder();
        builder.expectStatusCode(501);
        builder.expectHeader("Warning", notNullValue(String.class));
        builder.expectBody("status", equalTo("ERROR"));
        if (message.isPresent()) {
            builder.expectBody("message", equalTo(message.get()));
        } else {
            builder.expectBody("message", notNullValue(String.class));
        }
        return builder.build();
    }

    /**
     * Returns a {@link com.jayway.restassured.specification.ResponseSpecification} that can be used to validate paginated results
     *
     * @param page
     *         the expected page
     * @param perPage
     *         the expected results per page
     * @param totalPages
     *         the expected total number of pages
     * @param totalCount
     *         the expected total number of results
     * @param dataSize
     *         the expected number of results returned in the data payload
     * @param resourcePath
     *         the resource path
     * @param first
     *         an Optional containing the expected query parameters for the first link, if empty the result is checked if it is null
     * @param last
     *         an Optional containing the expected query parameters for the last link, if empty the result is checked if it is null
     * @param next
     *         an Optional containing the expected query parameters for the next link, if empty the result is checked if it is null
     * @param prev
     *         an Optional containing the expected query parameters for the prev link, if empty the result is checked if it is null
     * @return a {@link com.jayway.restassured.specification.ResponseSpecification} that can be used to validate paginated results
     */
    public static ResponseSpecification paginationResponseSpecification(int page, int perPage, int totalPages, int totalCount, int dataSize, String resourcePath, Optional<String> first,
                                                                        Optional<String> last, Optional<String> next, Optional<String> prev) {
        ResponseSpecBuilder builder = new ResponseSpecBuilder();
        builder.expectStatusCode(200);
        builder.expectBody("page", equalTo(page));
        builder.expectBody("per_page", equalTo(perPage));
        builder.expectBody("total_pages", equalTo(totalPages));
        builder.expectBody("total_count", equalTo(totalCount));
        builder.expectBody("data", hasSize(dataSize));
        if (first.isPresent()) {
            builder.expectBody("links.first.href", equalTo(resourcePath + first.get()));
        } else {
            builder.expectBody("links.first", nullValue());
        }
        if (last.isPresent()) {
            builder.expectBody("links.last.href", equalTo(resourcePath + last.get()));
        } else {
            builder.expectBody("links.last", nullValue());
        }
        if (next.isPresent()) {
            builder.expectBody("links.next.href", equalTo(resourcePath + next.get()));
        } else {
            builder.expectBody("links.next", nullValue());
        }
        if (prev.isPresent()) {
            builder.expectBody("links.prev.href", equalTo(resourcePath + prev.get()));
        } else {
            builder.expectBody("links.prev", nullValue());
        }
        return builder.build();
    }

    /**
     * Returns the job resource path.
     *
     * @param baseURL
     *         the base URL of the REST API server e.g. http://localhost:8080/omakase
     * @return the job resource path.
     */
    public static String getJobResourcePath(URL baseURL) {
        return getResourcePath(baseURL, "jobs", Optional.empty());
    }

    /**
     * Returns the job resource path.
     *
     * @param baseURL
     *         the base URL of the REST API server e.g. http://localhost:8080/omakase
     * @param path
     *         additional paths that should be appended after the base resource path, this can be either a concrete value or a template parameter
     * @param templateParameters
     *         a list of URI template parameter values that will be used to substitute template parameters in the path
     * @return the job resource path.
     */
    public static String getJobResourcePath(URL baseURL, String path, Object... templateParameters) {
        return getResourcePath(baseURL, "jobs", Optional.ofNullable(path), templateParameters);
    }

    /**
     * Returns the asset resource path.
     *
     * @param baseURL
     *         the base URL of the REST API server e.g. http://localhost:8080/omakase
     * @return the asset resource path.
     */
    public static String getAssetsResourcePath(URL baseURL) {
        return getResourcePath(baseURL, "assets", Optional.empty());
    }

    /**
     * Returns the asset resource path.
     *
     * @param baseURL
     *         the base URL of the REST API server e.g. http://localhost:8080/omakase
     * @param assetId
     *         the asset id
     * @return the asset resource path.
     */
    public static String getAssetResourcePath(URL baseURL, String assetId) {
        return getResourcePath(baseURL, "assets", Optional.ofNullable("{assetId}"), assetId);
    }

    /**
     * Returns the variant resource path
     *
     * @param baseURL
     *         the base URL of the REST API server e.g. http://localhost:8080/omakase
     * @param assetId
     *         the variant's parent asset id
     * @return the variant resource path
     */
    public static String getVariantsResourcePath(URL baseURL, String assetId) {
        return getResourcePath(baseURL, "assets/{assetId}/variants", Optional.empty(), assetId);
    }

    /**
     * Returns the variant resource path
     *
     * @param baseURL
     *         the base URL of the REST API server e.g. http://localhost:8080/omakase
     * @param assetId
     *         the variant's parent asset id
     * @param variantId
     *         the variant's id
     * @return the variant resource path
     */
    public static String getVariantResourcePath(URL baseURL, String assetId, String variantId) {
        return getResourcePath(baseURL, "assets", Optional.of("/{assetId}/variants/{variantId}"), assetId, variantId);
    }

    /**
     * Returns the repository resource path.
     *
     * @param baseURL
     *         the base URL of the REST API server e.g. http://localhost:8080/omakase
     * @return the repository resource path.
     */
    public static String getRepositoryResourcePath(URL baseURL) {
        return getResourcePath(baseURL, "repositories", Optional.empty());
    }

    /**
     * Returns the repository resource path.
     *
     * @param baseURL
     *         the base URL of the REST API server e.g. http://localhost:8080/omakase
     * @param path
     *         additional paths that should be appended after the base resource path, this can be either a concrete value or a template parameter
     * @param templateParameters
     *         a list of URI template parameter values that will be used to substitute template parameters in the path
     * @return the repository resource path.
     */
    public static String getRepositoryResourcePath(URL baseURL, String path, Object... templateParameters) {
        return getResourcePath(baseURL, "repositories", Optional.ofNullable(path), templateParameters);
    }

    /**
     * Returns the repository configuration resource path.
     *
     * @param baseURL
     *         the base URL of the REST API server e.g. http://localhost:8080/omakase
     * @param repositoryId
     *         the repository id
     * @return the repository configuration resource path.
     */
    public static String getRepositoryConfigurationResourcePath(URL baseURL, String repositoryId) {
        return getResourcePath(baseURL, "repositories/{repositoryId}/configuration", Optional.empty(), repositoryId);
    }

    /**
     * Returns the location resource path.
     *
     * @param baseURL
     *         the base URL of the REST API server e.g. http://localhost:8080/omakase
     * @return the location resource path.
     */
    public static String getLocationResourcePath(URL baseURL) {
        return getResourcePath(baseURL, "locations", Optional.empty());
    }

    /**
     * Returns the location resource path.
     *
     * @param baseURL
     *         the base URL of the REST API server e.g. http://localhost:8080/omakase
     * @param path
     *         additional paths that should be appended after the base resource path, this can be either a concrete value or a template parameter
     * @param templateParameters
     *         a list of URI template parameter values that will be used to substitute template parameters in the path
     * @return the location resource path.
     */
    public static String getLocationResourcePath(URL baseURL, String path, Object... templateParameters) {
        return getResourcePath(baseURL, "locations", Optional.ofNullable(path), templateParameters);
    }

    /**
     * Returns the location configuration resource path.
     *
     * @param baseURL
     *         the base URL of the REST API server e.g. http://localhost:8080/omakase
     * @param locationId
     *         the location id
     * @return the location configuration resource path.
     */
    public static String getLocationConfigurationResourcePath(URL baseURL, String locationId) {
        return getResourcePath(baseURL, "locations/{locationId}/configuration", Optional.empty(), locationId);
    }

    /**
     * Returns the worker resource path.
     *
     * @param baseURL
     *         the base URL of the REST API server e.g. http://localhost:8080/omakase
     * @return the worker resource path.
     */
    public static String getWorkersResourcePath(URL baseURL) {
        return getResourcePath(baseURL, "broker", Optional.of("workers"));
    }

    /**
     * Returns the worker resource path.
     *
     * @param baseURL
     *         the base URL of the REST API server e.g. http://localhost:8080/omakase
     * @param workerId
     *         the worker id
     * @return the worker resource path.
     */
    public static String getWorkersResourcePath(URL baseURL, String workerId) {
        return getResourcePath(baseURL, "broker", Optional.of("workers/{workerId}"), workerId);
    }

    /**
     * Returns the full resource path for the given base URL, resourceName and additional paths.
     *
     * @param baseURL
     *         the base URL of the REST API server e.g. http://localhost:8080/omakase
     * @param resourceName
     *         the resource name e.g. assets
     * @param path
     *         an Optional containing additional paths that should be appended after the base resource path, this can be either a concrete value or a template parameter e.g. {assetId}
     * @param templateParameters
     *         a list of URI template parameter values that will be used to substitute template parameters in the path
     * @return the full resource path for the given base URL, resourceName and additional paths.
     */
    public static String getResourcePath(URL baseURL, String resourceName, Optional<String> path, Object... templateParameters) {
        UriBuilder builder = UriBuilder.fromUri(Throwables.returnableInstance(baseURL::toURI));
        builder.path("api/" + resourceName);
        path.ifPresent(builder::path);
        return builder.build(templateParameters).toString();
    }

    /**
     * Removes all assets, jobs and repositories along with their children.
     *
     * @param baseURL
     *         the base URL of the REST API server e.g. http://localhost:8080/omakase
     */
    public static void cleanup(URL baseURL) {

        String pagingQueryParams = "?page=1&per_page=100";

        // remove assets and variants
        from(get(RestIntegrationTests.getAssetsResourcePath(baseURL) + pagingQueryParams, "reader", "password", Optional.empty(), Optional.empty()).body().asString()).getList("data.id", String.class)
                .forEach(assetId -> {
                    from(get(RestIntegrationTests.getVariantsResourcePath(baseURL, assetId) + pagingQueryParams, "reader", "password", Optional.empty(), Optional.empty()).body().asString())
                            .getList("data.id", String.class)
                            .forEach(variantId -> delete(RestIntegrationTests.getVariantResourcePath(baseURL, assetId, variantId), "editor", "password", Optional.of("v1"), Optional.empty()));
                    delete(RestIntegrationTests.getAssetResourcePath(baseURL, assetId), "editor", "password", Optional.of("v1"), Optional.empty());
                });

        // remove jobs
        from(get(RestIntegrationTests.getJobResourcePath(baseURL) + pagingQueryParams, "reader", "password", Optional.empty(), Optional.empty()).body().asString()).getList("data.id", String.class)
                .forEach(id -> delete(RestIntegrationTests.getJobResourcePath(baseURL, "{assetId}", id), "editor", "password", Optional.of("v1"), Optional.empty()));

        // remove repositories
        from(get(RestIntegrationTests.getRepositoryResourcePath(baseURL) + pagingQueryParams, "reader", "password", Optional.of("v1"), Optional.empty()).body().asString()).getList("data.id", String.class)
                .forEach(id -> delete(RestIntegrationTests.getRepositoryResourcePath(baseURL, "{repositoryId}", id), "editor", "password", Optional.of("v1"), Optional.empty()));

        // remove repositories
        from(get(RestIntegrationTests.getLocationResourcePath(baseURL) + pagingQueryParams, "reader", "password", Optional.of("v1"), Optional.empty()).body().asString()).getList("data.id", String.class)
                .forEach(id -> delete(RestIntegrationTests.getLocationResourcePath(baseURL, "{locationId}", id), "editor", "password", Optional.of("v1"), Optional.empty()));

        // remove workers
        from(get(RestIntegrationTests.getWorkersResourcePath(baseURL) + pagingQueryParams, "reader", "password", Optional.empty(), Optional.empty()).body().asString()).getList("data.id", String.class)
                .forEach(id -> delete(RestIntegrationTests.getWorkersResourcePath(baseURL, id), "editor", "password", Optional.of("v1"), Optional.empty()));
    }
}
