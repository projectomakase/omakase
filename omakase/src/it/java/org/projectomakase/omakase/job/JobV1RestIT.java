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
package org.projectomakase.omakase.job;

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
import static org.projectomakase.omakase.RestIntegrationTests.delete;
import static org.projectomakase.omakase.RestIntegrationTests.forbiddenResponseSpecification;
import static org.projectomakase.omakase.RestIntegrationTests.get;
import static org.projectomakase.omakase.RestIntegrationTests.getJobResourcePath;
import static org.projectomakase.omakase.RestIntegrationTests.notFoundResponseSpecification;
import static org.projectomakase.omakase.RestIntegrationTests.paginationResponseSpecification;
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
 * @author Richard Lucas
 */
@RunAsClient
@RunWith(Arquillian.class)
public class JobV1RestIT {

    @ArquillianResource
    URL baseURL;

    private String repositoryId;
    private String variantId;

    @Deployment
    public static WebArchive deploy() {
        return Archives.omakaseRESTITWar();
    }

    @Before
    public void before() throws Exception {
        RestAssured.replaceFiltersWith(ImmutableList.of());
        RestIntegrationTests.cleanup(baseURL);
        setup();
        RestAssured.replaceFiltersWith(ResponseLoggingFilter.responseLogger(), new RequestLoggingFilter());
    }

    @After
    public void after() throws Exception {
        RestAssured.replaceFiltersWith(ImmutableList.of());
        RestIntegrationTests.cleanup(baseURL);
    }

    @Test
    public void shouldReturnCreatedOnPostJobsWithRequiredFields() throws Exception {
        post(getJobResourcePath(baseURL), getCreateJobJsonRequired(variantId, repositoryId), "editor", "password", Optional.of("v1"), Optional.empty());
    }

    @Test
    public void shouldReturnCreatedOnPostJobsWithOptionalFields() throws Exception {
        post(getJobResourcePath(baseURL), getCreateJobJsonOptional(variantId, repositoryId), "editor", "password", Optional.of("v1"), Optional.empty());
    }

    @Test
    public void shouldReturnForbiddenOnPostJobsWithUnauthorizedUser() throws Exception {
        post(getJobResourcePath(baseURL), getCreateJobJsonRequired(variantId, repositoryId), "reader", "password", Optional.of("v1"), Optional.of(forbiddenResponseSpecification()));
    }

    @Test
    public void shouldReturnBadRequestOnPostJobsWithoutType() throws Exception {
        String json = "{ \"configuration\" : " + getJobConfigurationJson(variantId, repositoryId) + "}";
        post(getJobResourcePath(baseURL), json, "editor", "password", Optional.of("v1"), Optional.of(badRequestResponseSpecification(Optional.of("'type' is a required property"))));
    }

    @Test
    public void shouldReturnBadRequestOnPostJobsWithInvalidType() throws Exception {
        String json = "{ \"type\": \"BAD\", \"configuration\" : " + getJobConfigurationJson(variantId, repositoryId) + "}";
        post(getJobResourcePath(baseURL), json, "editor", "password", Optional.of("v1"), Optional.of(badRequestResponseSpecification(Optional.of("Unsupported job type"))));
    }

    @Test
    public void shouldReturnBadRequestOnPostJobsWithInvalidPriority() throws Exception {
        String json = "{ \"type\": \"" + "INGEST" + "\", \"priority\": 23, \"configuration\" : " + getJobConfigurationJson(variantId, repositoryId) + "}";
        post(getJobResourcePath(baseURL), json, "editor", "password", Optional.of("v1"),
             Optional.of(badRequestResponseSpecification(Optional.of("Invalid priority 23, the value must be between 1 and 10"))));
    }

    @Test
    public void shouldReturnBadRequestOnPostJobsWithoutConfiguration() throws Exception {
        String json = "{ \"type\": \"" + "INGEST" + "\" }";
        post(getJobResourcePath(baseURL), json, "editor", "password", Optional.of("v1"), Optional.of(badRequestResponseSpecification(Optional.of("'configuration' is a required property"))));
    }

    @Test
    public void shouldReturnBadRequestOnPostJobsWithInvalidConfiguration() throws Exception {
        String json = "{ \"type\": \"" + "INGEST" + "\",  \"configuration\" : {} }";
        post(getJobResourcePath(baseURL), json, "editor", "password", Optional.of("v1"), Optional.of(badRequestResponseSpecification(Optional.of(
                "The job configuration contains validation errors: ['variant' is a required property, Variant does not exist or is inaccessible, One or more repositories must be provided, One or more files must be provided]"))));
    }

    @Test
    public void shouldReturnOkOnGetJobsWithNoResults() throws Exception {
        get(getJobResourcePath(baseURL), "reader", "password", Optional.of("v1"), Optional.empty());
    }

    @Test
    public void shouldReturnOkOnGetJobs() throws Exception {
        post(getJobResourcePath(baseURL), getCreateJobJsonOptional(variantId, repositoryId), "editor", "password", Optional.of("v1"), Optional.empty());

        ResponseSpecBuilder builder = new ResponseSpecBuilder();
        builder.expectStatusCode(200);
        builder.expectBody("data.name", contains("test"));

        get(getJobResourcePath(baseURL), "reader", "password", Optional.of("v1"), Optional.of(builder.build()));
    }

    @Test
    public void shouldReturnOkOnGetJobsUsingPagination() throws Exception {

        String jobResource = getJobResourcePath(baseURL);

        for (int i = 0; i < 30; i++) {
            post(jobResource, getCreateJobJsonRequired(variantId, repositoryId), "editor", "password", Optional.of("v1"), Optional.empty());
        }

        ResponseSpecification responseSpecification =
                paginationResponseSpecification(1, 10, 3, 30, 10, jobResource, Optional.of("?page=1&per_page=10"), Optional.of("?page=3&per_page=10"), Optional.of("?page=2&per_page=10"),
                                                Optional.empty());
        com.jayway.restassured.response.Response response = get(jobResource, "reader", "password", Optional.of("v1"), Optional.of(responseSpecification));
        // Validate Link Headers
        assertThat(response.headers().getList("Link")).extracting("value")
                .containsExactly("<" + jobResource + "?page=1&per_page=10>; rel=\"first\"", "<" + jobResource + "?page=3&per_page=10>; rel=\"last\"",
                                 "<" + jobResource + "?page=2&per_page=10>; rel=\"next\"");

        responseSpecification =
                paginationResponseSpecification(2, 10, 3, 30, 10, jobResource, Optional.of("?page=1&per_page=10"), Optional.of("?page=3&per_page=10"), Optional.of("?page=3&per_page=10"),
                                                Optional.of("?page=1&per_page=10"));
        response = get(jobResource + "?page=2&per_page=10", "reader", "password", Optional.of("v1"), Optional.of(responseSpecification));
        // Validate Link Headers
        assertThat(response.headers().getList("Link")).extracting("value")
                .containsExactly("<" + jobResource + "?page=1&per_page=10>; rel=\"first\"", "<" + jobResource + "?page=3&per_page=10>; rel=\"last\"",
                                 "<" + jobResource + "?page=1&per_page=10>; rel=\"prev\"", "<" + jobResource + "?page=3&per_page=10>; rel=\"next\"");

        responseSpecification = paginationResponseSpecification(3, 10, 3, 30, 10, jobResource, Optional.of("?page=1&per_page=10"), Optional.of("?page=3&per_page=10"), Optional.empty(),
                                                                Optional.of("?page=2&per_page=10"));
        response = get(jobResource + "?page=3&per_page=10", "reader", "password", Optional.of("v1"), Optional.of(responseSpecification));
        // Validate Link Headers
        assertThat(response.headers().getList("Link")).extracting("value")
                .containsExactly("<" + jobResource + "?page=1&per_page=10>; rel=\"first\"", "<" + jobResource + "?page=3&per_page=10>; rel=\"last\"",
                                 "<" + jobResource + "?page=2&per_page=10>; rel=\"prev\"");

        responseSpecification =
                paginationResponseSpecification(4, 10, 3, 30, 0, jobResource, Optional.of("?page=1&per_page=10"), Optional.of("?page=3&per_page=10"), Optional.empty(), Optional.empty());
        response = get(jobResource + "?page=4&per_page=10", "reader", "password", Optional.of("v1"), Optional.of(responseSpecification));
        // Validate Link Headers
        assertThat(response.headers().getList("Link")).extracting("value")
                .containsExactly("<" + jobResource + "?page=1&per_page=10>; rel=\"first\"", "<" + jobResource + "?page=3&per_page=10>; rel=\"last\"");
    }

    @Test
    public void shouldReturnOkOnGetJobsUsingOnlyCount() throws Exception {

        String jobResource = getJobResourcePath(baseURL);

        for (int i = 0; i < 30; i++) {
            post(jobResource, getCreateJobJsonRequired(variantId, repositoryId), "editor", "password", Optional.of("v1"), Optional.empty());
        }

        ResponseSpecBuilder responseSpecBuilder = new ResponseSpecBuilder();
        responseSpecBuilder.expectStatusCode(200);
        responseSpecBuilder.expectBody("page", nullValue());
        responseSpecBuilder.expectBody("per_page", nullValue());
        responseSpecBuilder.expectBody("total_pages", nullValue());
        responseSpecBuilder.expectBody("total_count", equalTo(30));
        responseSpecBuilder.expectBody("data", hasSize(0));
        responseSpecBuilder.expectBody("links", nullValue());
        com.jayway.restassured.response.Response response = get(getJobResourcePath(baseURL) + "?only_count=true", "reader", "password", Optional.of("v1"), Optional.of(responseSpecBuilder.build()));
        // Validate Link Headers
        assertThat(response.header("Link")).isNull();
    }

    @Test
    public void shouldReturnOkOnGetJob() throws Exception {
        String jobId = post(getJobResourcePath(baseURL), getCreateJobJsonOptional(variantId, repositoryId), "editor", "password", Optional.of("v1"), Optional.empty()).header("Location")
                .replace(getJobResourcePath(baseURL) + "/", "");
        get(getJobResourcePath(baseURL, "{jobId}", jobId), "reader", "password", Optional.of("v1"),
            Optional.of(getJobOkResponseSpecification(jobId, "test", "INGEST", "UNSUBMITTED", 4, "editor", "editor", "id")));
    }

    @Test
    public void shouldReturnNotFoundOnGetAsset() throws Exception {
        get(getJobResourcePath(baseURL, "{jobId}", UUID.randomUUID().toString()), "reader", "password", Optional.of("v1"), Optional.of(notFoundResponseSpecification()));
    }

    @Test
    public void shouldReturnNotFoundOnGetAssetWithInvalidId() throws Exception {
        get(getJobResourcePath(baseURL, "{jobId}", "BAD-ID"), "reader", "password", Optional.of("v1"), Optional.of(notFoundResponseSpecification()));
    }

    @Test
    public void shouldReturnNotModifiedOnGetWithEtag() throws Exception {
        String jobId = post(getJobResourcePath(baseURL), getCreateJobJsonOptional(variantId, repositoryId), "editor", "password", Optional.of("v1"), Optional.empty()).header("Location")
                .replace(getJobResourcePath(baseURL) + "/", "");
        com.jayway.restassured.response.Response response = get(getJobResourcePath(baseURL, "{jobId}", jobId), "reader", "password", Optional.of("v1"),
                                                                Optional.of(getJobOkResponseSpecification(jobId, "test", "INGEST", "UNSUBMITTED", 4, "editor", "editor", "id")));
        String etag = response.getHeader("ETag");
        given().spec(requestSpecification("reader", "password", Optional.of("v1"))).header("If-None-Match", etag).then().statusCode(Response.Status.NOT_MODIFIED.getStatusCode()).when()
                .header("ETag", notNullValue()).get(getJobResourcePath(baseURL, "{jobId}", jobId)).then().assertThat().body(isEmptyOrNullString());
    }

    @Test
    public void shouldReturnOKOnPutJob() throws Exception {
        String jobId = post(getJobResourcePath(baseURL), getCreateJobJsonOptional(variantId, repositoryId), "editor", "password", Optional.of("v1"), Optional.empty()).header("Location")
                .replace(getJobResourcePath(baseURL) + "/", "");
        ResponseBody before = get(getJobResourcePath(baseURL, "{jobId}", jobId), "reader", "password", Optional.of("v1"),
                                  Optional.of(getJobOkResponseSpecification(jobId, "test", "INGEST", "UNSUBMITTED", 4, "editor", "editor", "id")));

        String json = "{ \"name\" : \"test2\", \"external_ids\" : [\"id\", \"id2\"], \"status\" : {\"current\": \"UNSUBMITTED\"}, \"type\" : \"" + "INGEST" + "\", \"priority\": 2}";
        put(getJobResourcePath(baseURL, "{jobId}", jobId), json, "editor", "password", Optional.of("v1"), Optional.empty());
        ResponseBody after = get(getJobResourcePath(baseURL, "{jobId}", jobId), "reader", "password", Optional.of("v1"),
                                 Optional.of(getJobOkResponseSpecification(jobId, "test2", "INGEST", "UNSUBMITTED", 2, "editor", "editor", "id", "id2")));
        checkJobReadOnlyFieldsAreTheSame(before, after);
    }

    @Test
    public void shouldReturnBadRequestOnPutJobDifferentTypes() throws Exception {
        String jobId = post(getJobResourcePath(baseURL), getCreateJobJsonOptional(variantId, repositoryId), "editor", "password", Optional.of("v1"), Optional.empty()).header("Location")
                .replace(getJobResourcePath(baseURL) + "/", "");
        String json = "{ \"name\" : \"test2\", \"external_ids\" : [\"id\", \"id2\"], \"status\" : {\"current\": \"UNSUBMITTED\"}, \"type\" : \"TRANSFORMATION\", \"priority\": 2}";
        put(getJobResourcePath(baseURL, "{jobId}", jobId), json, "editor", "password", Optional.of("v1"), Optional.of(forbiddenResponseSpecification("Changing the job type is not supported")));
    }

    @Test
    public void shouldReturnBadRequestOnPutJobInvalidPriority() throws Exception {
        String jobId = post(getJobResourcePath(baseURL), getCreateJobJsonOptional(variantId, repositoryId), "editor", "password", Optional.of("v1"), Optional.empty()).header("Location")
                .replace(getJobResourcePath(baseURL) + "/", "");
        String json = "{ \"name\" : \"test2\", \"external_ids\" : [\"id\", \"id2\"], \"status\" : {\"current\": \"UNSUBMITTED\"}, \"type\" : \"" + "INGEST" + "\", \"priority\": 23}";
        put(getJobResourcePath(baseURL, "{jobId}", jobId), json, "editor", "password", Optional.of("v1"),
            Optional.of(badRequestResponseSpecification(Optional.of("Invalid priority 23, the value must be between 1 and 10"))));
    }

    @Test
    public void shouldReturnBadRequestOnPutJobStatusDoesNotMatchCurrentStatus() throws Exception {
        String jobId = post(getJobResourcePath(baseURL), getCreateJobJsonOptional(variantId, repositoryId), "editor", "password", Optional.of("v1"), Optional.empty()).header("Location")
                .replace(getJobResourcePath(baseURL) + "/", "");
        String json = "{ \"name\" : \"test2\", \"external_ids\" : [\"id\", \"id2\"], \"status\" : {\"current\": \"QUEUED\"}, \"type\" : \"" + "INGEST" + "\", \"priority\": 2}";
        put(getJobResourcePath(baseURL, "{jobId}", jobId), json, "editor", "password", Optional.of("v1"),
            Optional.of(forbiddenResponseSpecification("The new status does not match the current status")));
    }

    @Test
    public void shouldReturnOkOnPatchJob() {
        String jobId = post(getJobResourcePath(baseURL), getCreateJobJsonOptional(variantId, repositoryId), "editor", "password", Optional.of("v1"), Optional.empty()).header("Location")
                .replace(getJobResourcePath(baseURL) + "/", "");
        ResponseBody before = get(getJobResourcePath(baseURL, "{jobId}", jobId), "reader", "password", Optional.of("v1"),
                                  Optional.of(getJobOkResponseSpecification(jobId, "test", "INGEST", "UNSUBMITTED", 4, "editor", "editor", "id")));

        String patchJson = "[" +
                "    {" +
                "        \"op\": \"replace\"," +
                "        \"path\": \"/name\"," +
                "        \"value\": \"test2\"" +
                "    }," +
                "    {" +
                "        \"op\": \"add\"," +
                "        \"path\": \"/external_ids/-\"," +
                "        \"value\": \"id2\"" +
                "    }," +
                "    {" +
                "        \"op\": \"replace\"," +
                "        \"path\": \"/priority\"," +
                "        \"value\": 2" +
                "    }" +
                "]";
        patch(getJobResourcePath(baseURL, "{jobId}", jobId), patchJson, "editor", "password", Optional.of("v1"), Optional.empty());

        ResponseBody after = get(getJobResourcePath(baseURL, "{jobId}", jobId), "reader", "password", Optional.of("v1"),
                                 Optional.of(getJobOkResponseSpecification(jobId, "test2", "INGEST", "UNSUBMITTED", 2, "editor", "editor", "id", "id2")));
        checkJobReadOnlyFieldsAreTheSame(before, after);
    }

    @Test
    public void shouldReturnBadRequestOnPatchJobDifferentTypes() throws Exception {
        String jobId = post(getJobResourcePath(baseURL), getCreateJobJsonOptional(variantId, repositoryId), "editor", "password", Optional.of("v1"), Optional.empty()).header("Location")
                .replace(getJobResourcePath(baseURL) + "/", "");
        String patchJson = "[" +
                "    {" +
                "        \"op\": \"replace\"," +
                "        \"path\": \"/type\"," +
                "        \"value\": \"TRANSFORMATION\"" +
                "    }" +
                "]";
        patch(getJobResourcePath(baseURL, "{jobId}", jobId), patchJson, "editor", "password", Optional.of("v1"), Optional.of(forbiddenResponseSpecification("Changing the job type is not supported")));
    }

    @Test
    public void shouldReturnBadRequestOnPatchJobInvalidPriority() throws Exception {
        String jobId = post(getJobResourcePath(baseURL), getCreateJobJsonOptional(variantId, repositoryId), "editor", "password", Optional.of("v1"), Optional.empty()).header("Location")
                .replace(getJobResourcePath(baseURL) + "/", "");
        String patchJson = "[" +
                "    {" +
                "        \"op\": \"replace\"," +
                "        \"path\": \"/priority\"," +
                "        \"value\": 23" +
                "    }" +
                "]";
        patch(getJobResourcePath(baseURL, "{jobId}", jobId), patchJson, "editor", "password", Optional.of("v1"),
              Optional.of(badRequestResponseSpecification(Optional.of("Invalid priority 23, the value must be between 1 and 10"))));
    }

    @Test
    public void shouldReturnBadRequestOnPatchJobStatusDoesNotMatchCurrentStatus() throws Exception {
        String jobId = post(getJobResourcePath(baseURL), getCreateJobJsonOptional(variantId, repositoryId), "editor", "password", Optional.of("v1"), Optional.empty()).header("Location")
                .replace(getJobResourcePath(baseURL) + "/", "");
        String patchJson = "[" +
                "    {" +
                "        \"op\": \"replace\"," +
                "        \"path\": \"/status/current\"," +
                "        \"value\": \"EXECUTING\"" +
                "    }" +
                "]";
        patch(getJobResourcePath(baseURL, "{jobId}", jobId), patchJson, "editor", "password", Optional.of("v1"),
              Optional.of(forbiddenResponseSpecification("The new status does not match the current status")));
    }

    @Test
    public void shouldReturnOkOnDeleteJob() throws Exception {
        String jobId = post(getJobResourcePath(baseURL), getCreateJobJsonOptional(variantId, repositoryId), "editor", "password", Optional.of("v1"), Optional.empty()).header("Location")
                .replace(getJobResourcePath(baseURL) + "/", "");
        delete(getJobResourcePath(baseURL, "{jobId}", jobId), "editor", "password", Optional.of("v1"), Optional.empty());
    }

    @Test
    public void shouldReturnNotFoundOnDeleteJob() throws Exception {
        delete(getJobResourcePath(baseURL, "{jobId}", UUID.randomUUID().toString()), "editor", "password", Optional.of("v1"), Optional.of(notFoundResponseSpecification()));
    }

    @Test
    public void shouldReturnForbiddenOnDeleteJobNotAuthorized() throws Exception {
        String jobId = post(getJobResourcePath(baseURL), getCreateJobJsonOptional(variantId, repositoryId), "editor", "password", Optional.of("v1"), Optional.empty()).header("Location")
                .replace(getJobResourcePath(baseURL) + "/", "");
        delete(getJobResourcePath(baseURL, "{jobId}", jobId), "reader", "password", Optional.of("v1"), Optional.of(forbiddenResponseSpecification()));
    }


    @Test
    public void shouldReturnForbiddenOnPutJobStatusInvalidStatus() throws Exception {
        String jobId = post(getJobResourcePath(baseURL), getCreateJobJsonOptional(variantId, repositoryId), "editor", "password", Optional.of("v1"), Optional.empty()).header("Location")
                .replace(getJobResourcePath(baseURL) + "/", "");
        String json = "{\"current\" : \"COMPLETED\"}";
        put(getJobResourcePath(baseURL, "{jobId}/status", jobId), json, "editor", "password", Optional.of("v1"),
            Optional.of(forbiddenResponseSpecification("Job status can not be updated to COMPLETED")));
    }

    @Test
    public void shouldReturnOkOnGetJobsDefaultSort() throws Exception {

        String jobResource = getJobResourcePath(baseURL);

        for (int i = 0; i < 5; i++) {
            String json = "{ \"name\" : \"test" + i + "\", \"external_ids\" : [\"id\"], \"status\" : \"UNSUBMITTED\", \"type\" : \"INGEST\", \"priority\": 4,  \"configuration\" : " +
                    getJobConfigurationJson(variantId, repositoryId) + "}";
            post(jobResource, json, "editor", "password", Optional.of("v1"), Optional.empty());
        }

        ResponseSpecBuilder responseSpecBuilder = new ResponseSpecBuilder();
        responseSpecBuilder.expectStatusCode(200);
        responseSpecBuilder.expectBody("data.name", contains("test4", "test3", "test2", "test1", "test0"));
        get(getJobResourcePath(baseURL), "reader", "password", Optional.of("v1"), Optional.of(responseSpecBuilder.build()));
    }

    @Test
    public void shouldReturnOkOnGetJobsSortNameAsc() throws Exception {

        String jobResource = getJobResourcePath(baseURL);

        for (int i = 0; i < 5; i++) {
            String json = "{ \"name\" : \"test" + i + "\", \"external_ids\" : [\"id\"], \"status\" : \"UNSUBMITTED\", \"type\" : \"INGEST\", \"priority\": 4,  \"configuration\" : " +
                    getJobConfigurationJson(variantId, repositoryId) + "}";
            post(jobResource, json, "editor", "password", Optional.of("v1"), Optional.empty());
        }

        ResponseSpecBuilder responseSpecBuilder = new ResponseSpecBuilder();
        responseSpecBuilder.expectStatusCode(200);
        responseSpecBuilder.expectBody("data.name", contains("test0", "test1", "test2", "test3", "test4"));
        get(getJobResourcePath(baseURL) + "?sort=name&order=ASC", "reader", "password", Optional.of("v1"), Optional.of(responseSpecBuilder.build()));
    }

    @Test
    public void shouldReturnOkOnGetJobsFilteredByNameEqual() throws Exception {

        String jobResource = getJobResourcePath(baseURL);

        for (int i = 0; i < 5; i++) {
            String json = "{ \"name\" : \"test" + i + "\", \"external_ids\" : [\"id\"], \"status\" : \"UNSUBMITTED\", \"type\" : \"INGEST\", \"priority\": 4,  \"configuration\" : " +
                    getJobConfigurationJson(variantId, repositoryId) + "}";
            post(jobResource, json, "editor", "password", Optional.of("v1"), Optional.empty());
        }

        ResponseSpecBuilder responseSpecBuilder = new ResponseSpecBuilder();
        responseSpecBuilder.expectStatusCode(200);
        responseSpecBuilder.expectBody("data.name", contains("test0"));
        get(getJobResourcePath(baseURL) + "?name[eq]=test0", "reader", "password", Optional.of("v1"), Optional.of(responseSpecBuilder.build()));
    }

    @Test
    public void shouldReturnOkOnGetJobsFilteredByNameWithSpaces() throws Exception {

        String jobResource = getJobResourcePath(baseURL);

        for (int i = 0; i < 5; i++) {
            String json = "{ \"name\" : \"test " + i + "\", \"external_ids\" : [\"id\"], \"status\" : \"UNSUBMITTED\", \"type\" : \"INGEST\", \"priority\": 4,  \"configuration\" : " +
                    getJobConfigurationJson(variantId, repositoryId) + "}";
            post(jobResource, json, "editor", "password", Optional.of("v1"), Optional.empty());
        }

        ResponseSpecBuilder responseSpecBuilder = new ResponseSpecBuilder();
        responseSpecBuilder.expectStatusCode(200);
        responseSpecBuilder.expectBody("data.name", contains("test 0"));
        // the RestAssured framework auto escapes the space to %20
        get(getJobResourcePath(baseURL) + "?name[eq]=test 0", "reader", "password", Optional.of("v1"), Optional.of(responseSpecBuilder.build()));
    }

    @Test
    public void shouldReturnOkOnGetJobsFilteredByNameLike() throws Exception {

        String jobResource = getJobResourcePath(baseURL);

        for (int i = 0; i < 5; i++) {
            String json = "{ \"name\" : \"test" + i + "\", \"external_ids\" : [\"id\"], \"status\" : \"UNSUBMITTED\", \"type\" : \"INGEST\", \"priority\": 4,  \"configuration\" : " +
                    getJobConfigurationJson(variantId, repositoryId) + "}";
            post(jobResource, json, "editor", "password", Optional.of("v1"), Optional.empty());
        }

        ResponseSpecBuilder responseSpecBuilder = new ResponseSpecBuilder();
        responseSpecBuilder.expectStatusCode(200);
        responseSpecBuilder.expectBody("data.name", contains("test3"));
        get(getJobResourcePath(baseURL) + "?name[like]=3", "reader", "password", Optional.of("v1"), Optional.of(responseSpecBuilder.build()));
    }

    @Test
    public void shouldReturnBadRequestOnGetJobsFilteredByInvalidDateFormat() throws Exception {
        ResponseSpecBuilder responseSpecBuilder = new ResponseSpecBuilder();
        responseSpecBuilder.expectStatusCode(200);
        responseSpecBuilder.expectBody("data.name", contains("test"));
        get(getJobResourcePath(baseURL) + "?created[eq]=3", "reader", "password", Optional.of("v1"),
            Optional.of(badRequestResponseSpecification(Optional.of("Invalid date format for attribute created"))));
    }

    @Test
    public void shouldReturnBadRequestOnGetJobsFilteredUnsupportedOperator() throws Exception {
        ResponseSpecBuilder responseSpecBuilder = new ResponseSpecBuilder();
        responseSpecBuilder.expectStatusCode(200);
        responseSpecBuilder.expectBody("data.name", contains("test"));
        get(getJobResourcePath(baseURL) + "?type[ne]=EXPORT", "reader", "password", Optional.of("v1"),
            Optional.of(badRequestResponseSpecification(Optional.of("Invalid search condition operator NE for condition attribute type"))));
    }

    private void setup() {
        String assetId = createAsset();
        variantId = createVariant(assetId);
        repositoryId = createFileRepository();
    }

    private String createAsset() {
        String json = "{ \"name\" : \"test1\", \"external_ids\" : [\"id1\", \"id2\"]}";
        return post(RestIntegrationTests.getAssetsResourcePath(baseURL), json, "editor", "password", Optional.of("v1"), Optional.empty()).header("Location")
                .replace(RestIntegrationTests.getAssetsResourcePath(baseURL) + "/", "");
    }

    private String createVariant(String assetId) {
        String json = "{ \"name\" : \"test1\", \"external_ids\" : [\"id1\", \"id2\"]}";
        return post(RestIntegrationTests.getVariantsResourcePath(baseURL, assetId), json, "editor", "password", Optional.of("v1"), Optional.empty()).header("Location")
                .replace(RestIntegrationTests.getVariantsResourcePath(baseURL, assetId) + "/", "");
    }

    private String createFileRepository() {
        String json = "{ \"name\" : \"configured\", \"type\" : \"FILE\"}";
        String repositoryId =
                post(RestIntegrationTests.getRepositoryResourcePath(baseURL), json, "editor", "password", Optional.of("v1"), Optional.empty()).header("Location").replace(
                        RestIntegrationTests.getRepositoryResourcePath(baseURL) + "/", "");
        String configJson = "{\"root\" : \"/test\"}";
        put(RestIntegrationTests.getRepositoryConfigurationResourcePath(baseURL, repositoryId), configJson, "editor", "password", Optional.of("v1"), Optional.empty());
        return repositoryId;
    }

    private String getCreateJobJsonRequired(String variant, String repositoryName) {
        return "{ \"type\" : \"INGEST\", \"configuration\" : {\"variant\":\"" + variant + "\", \"repositories\":[\"" + repositoryName + "\"], \"files\":[{\"uri\":\"file:/test\"}]}}";

    }

    private String getCreateJobJsonOptional(String variant, String repositoryName) {
        return "{ \"name\" : \"test\", \"external_ids\" : [\"id\"], \"status\" : \"UNSUBMITTED\", \"type\" : \"INGEST\", \"priority\": 4,  \"configuration\" : " +
                getJobConfigurationJson(variant, repositoryName) + "}";
    }

    private String getJobConfigurationJson(String variant, String repositoryName) {
        return "{\"variant\" : \"" + variant + "\", \"repositories\":[\"" + repositoryName + "\"], \"files\":[{\"uri\":\"file:/test\", \"hash\":\"abc-def\", \"hash_algorithm\":\"MD5\"}]}";
    }

    private ResponseSpecification getJobOkResponseSpecification(String jobId, String name, String type, String status, int priority, String createdUsername, String modifiedUsername,
                                                                String... externalIds) {
        String jobResource = getJobResourcePath(baseURL);
        ResponseSpecBuilder builder = new ResponseSpecBuilder();
        builder.expectStatusCode(200);
        builder.expectBody("name", equalTo(name));
        builder.expectBody("external_ids", contains(externalIds));
        builder.expectBody("type", equalTo(type));
        builder.expectBody("status.current", equalTo(status));
        builder.expectBody("status.timestamp", notNullValue(String.class));
        builder.expectBody("priority", equalTo(priority));
        builder.expectBody("created", notNullValue(String.class));
        builder.expectBody("created_by", equalTo(createdUsername));
        builder.expectBody("last_modified", notNullValue(String.class));
        builder.expectBody("last_modified_by", equalTo(modifiedUsername));
        builder.expectBody("links.self.href", equalTo(jobResource + "/" + jobId));
        builder.expectBody("links.configuration.href", equalTo(jobResource + "/" + jobId + "/configuration"));
        builder.expectBody("links.status.href", equalTo(jobResource + "/" + jobId + "/status"));
        return builder.build();
    }

    private void checkJobReadOnlyFieldsAreTheSame(ResponseBody before, ResponseBody after) {
        assertThat(before.jsonPath().getString("id")).isEqualTo(after.jsonPath().getString("id"));
        assertThat(before.jsonPath().getString("created_by")).isEqualTo(after.jsonPath().getString("created_by"));
        assertThat(before.jsonPath().getString("created")).isEqualTo(after.jsonPath().getString("created"));
        assertThat(before.jsonPath().getString("links.self.href")).isEqualTo(after.jsonPath().getString("links.self.href"));
        assertThat(before.jsonPath().getString("links.configuration.href")).isEqualTo(after.jsonPath().getString("links.configuration.href"));
        assertThat(before.jsonPath().getString("links.status.href")).isEqualTo(after.jsonPath().getString("links.status.href"));
    }
}
