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
import com.google.common.collect.ImmutableSet;
import org.projectomakase.omakase.Archives;
import org.projectomakase.omakase.IntegrationTests;
import org.projectomakase.omakase.TestRunner;
import org.projectomakase.omakase.commons.functions.Throwables;
import org.projectomakase.omakase.exceptions.NotFoundException;
import org.projectomakase.omakase.jcr.Template;
import org.projectomakase.omakase.location.api.Location;
import org.projectomakase.omakase.location.api.LocationConfigurationException;
import org.projectomakase.omakase.location.provider.file.FileLocationConfiguration;
import org.projectomakase.omakase.search.Operator;
import org.projectomakase.omakase.search.SearchCondition;
import org.projectomakase.omakase.search.SortOrder;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.ejb.EJBException;
import javax.inject.Inject;
import java.net.URI;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author Richard Lucas
 */
@RunWith(Arquillian.class)
public class LocationManagerIT {

    @Inject
    LocationManager locationManager;
    @Inject
    IntegrationTests integrationTests;

    @Deployment
    public static WebArchive deploy() {
        return Archives.omakaseITWar();
    }

    @Before
    public void before() {
        TestRunner.runAsUser("admin", "password", integrationTests::cleanup);
    }

    @After
    public void after() {
        TestRunner.runAsUser("admin", "password", integrationTests::cleanup);
    }

    @Test
    public void shouldCreateLocation() {
        TestRunner.runAsUser("admin", "password", () -> {
            Location location = createFileLocation();
            assertThat(location).isEqualToComparingOnlyGivenFields(new Location("test", "test", "FILE"), "locationName", "description", "type");
            assertThat(location.getLocationConfiguration()).isNull();
            assertGeneratedFieldsAreCorrect(location, "admin");
        });
    }

    @Test
    public void shouldCreateLocationWithConfiguration() {
        TestRunner.runAsUser("admin", "password", () -> {
            Location location = createFileLocation();
            location.setLocationConfiguration(new FileLocationConfiguration("file://test"));
            assertThat(location).isEqualToComparingOnlyGivenFields(new Location("test", "test", "FILE"), "locationName", "description", "type");
            assertThat(location.getLocationConfiguration()).isNotNull().isInstanceOf(FileLocationConfiguration.class);
            assertThat(((FileLocationConfiguration) location.getLocationConfiguration()).getRoot()).isEqualTo("file://test");
            assertGeneratedFieldsAreCorrect(location, "admin");
        });
    }

    @Test
    public void shouldFailToCreateLocationWithInvalidLocationType() {
        TestRunner.runAsUser("admin", "password", () -> assertThatThrownBy(() -> locationManager.createLocation(new Location("test", "test description", "BAD")))
                .isExactlyInstanceOf(EJBException.class)
                .hasCauseExactlyInstanceOf(LocationConfigurationException.class)
                .hasMessageEndingWith("Invalid location type BAD"));
    }

    @Test
    public void shouldFindLocationsOrderedByDefault() {
        // default is created DESC
        TestRunner.runAsUser("admin", "password", () -> {
            IntStream.range(0, 5).forEach(i -> createFileLocation("test" + i));
            assertThat(locationManager.findLocations(new LocationSearchBuilder().build()).getRecords()).extracting("locationName").containsExactly("test4", "test3", "test2", "test1", "test0");
        });
    }

    @Test
    public void shouldFindLocationsOrderedByNameAscending() {
        TestRunner.runAsUser("admin", "password", () -> {
            IntStream.range(0, 5).forEach(i -> createFileLocation("test" + i));
            assertThat(locationManager.findLocations(new LocationSearchBuilder().orderBy(Location.LOCATION_NAME).sortOrder(SortOrder.ASC).build()).getRecords()).extracting("locationName")
                    .containsExactly("test0", "test1", "test2", "test3", "test4");
        });
    }

    @Test
    public void shouldFindLocationsOrderedByNameDescending() {
        TestRunner.runAsUser("admin", "password", () -> {
            IntStream.range(0, 5).forEach(i -> createFileLocation("test" + i));
            assertThat(locationManager.findLocations(new LocationSearchBuilder().orderBy(Location.LOCATION_NAME).sortOrder(SortOrder.DESC).build()).getRecords()).extracting("locationName")
                    .containsExactly("test4", "test3", "test2", "test1", "test0");
        });
    }

    @Test
    public void shouldFindLocationsWhereNameEquals() {
        TestRunner.runAsUser("admin", "password", () -> {
            IntStream.range(0, 5).forEach(i -> createFileLocation("test" + i));
            assertThat(locationManager.findLocations(new LocationSearchBuilder().conditions(ImmutableList.of(new SearchCondition(Location.LOCATION_NAME, Operator.EQ, "test3"))).build()).getRecords())
                    .extracting("locationName").containsExactly("test3");
        });
    }

    @Test
    public void shouldFindLocationsWhereTypeEquals() {
        TestRunner.runAsUser("admin", "password", () -> {
            createFileLocation();
            assertThat(locationManager.findLocations(new LocationSearchBuilder().conditions(ImmutableList.of(new SearchCondition(Location.TYPE, Operator.EQ, "FILE"))).build()).getRecords())
                    .extracting("locationName").containsExactly("test");
        });
    }

    @Test
    public void shouldReturnEmptyOptionalForIdThatDoesNotExist() {
        TestRunner.runAsUser("admin", "password", () -> assertThat(locationManager.getLocation("test")).isEmpty());
    }

    @Test
    public void shouldReturnEmptyOptionalForBlankId() {
        TestRunner.runAsUser("admin", "password", () -> assertThat(locationManager.getLocation("")).isEmpty());
    }

    @Test
    public void shouldGetLocation() {
        TestRunner.runAsUser("admin", "password", () -> {
            Location fileLocation = createFileLocation();
            assertThat(locationManager.getLocation(fileLocation.getId())).isPresent().usingFieldByFieldValueComparator().contains(fileLocation);
        });
    }

    @Test
    public void shouldGetLocationConfigurationType() {
        TestRunner.runAsUser("admin", "password", () -> assertThat(locationManager.getLocationConfigurationType(createFileLocation().getId())).isEqualTo(FileLocationConfiguration.class));
    }

    @Test
    public void shouldUpdateLocationConfiguration() {
        TestRunner.runAsUser("admin", "password", () -> {
            Location location = createFileLocation();
            assertThat(location.getLocationConfiguration()).isNull();

            // add new configuration
            locationManager.updateLocationConfiguration(location.getId(), new FileLocationConfiguration("/test"));
            location = locationManager.getLocation(location.getId()).get();
            assertThat(location.getLocationConfiguration()).isNotNull().isInstanceOf(FileLocationConfiguration.class);
            assertThat(((FileLocationConfiguration) location.getLocationConfiguration()).getRoot()).isEqualTo("/test");

            // replace existing configuration
            locationManager.updateLocationConfiguration(location.getId(), new FileLocationConfiguration("/test2"));
            location = locationManager.getLocation(location.getId()).get();
            assertThat(location.getLocationConfiguration()).isNotNull().isInstanceOf(FileLocationConfiguration.class);
            assertThat(((FileLocationConfiguration) location.getLocationConfiguration()).getRoot()).isEqualTo("/test2");

            // update existing configuration
            location = locationManager.getLocation(location.getId()).get();
            ((FileLocationConfiguration) location.getLocationConfiguration()).setRoot("/test3");
            locationManager.updateLocationConfiguration(location.getId(), location.getLocationConfiguration());
            assertThat(location.getLocationConfiguration()).isNotNull().isInstanceOf(FileLocationConfiguration.class);
            assertThat(((FileLocationConfiguration) location.getLocationConfiguration()).getRoot()).isEqualTo("/test3");
        });
    }

    @Test
    public void shouldDeleteLocation() {
        TestRunner.runAsUser("admin", "password", () -> {
            Location fileLocation = createFileLocation();
            locationManager.deleteLocation(fileLocation.getId());
            assertThat(locationManager.getLocation(fileLocation.getId())).isEmpty();
        });
    }

    @Test
    public void shouldGetLocationTemplates() {
        TestRunner.runAsUser("admin", "password", () -> {
            ImmutableSet<Template> templates = locationManager.getLocationTemplates();
            assertThat(templates).extracting("type").contains("FILE", "FTP", "SFTP", "HTTP", "HTTPS", "S3");
        });
    }

    @Test
    public void shouldExpandNonLocationUri() {
        TestRunner.runAsUser("admin", "password", () -> assertThat(locationManager.expandLocationUri(Throwables.returnableInstance(() -> new URI("file:/test"))))
                .isEqualTo(Throwables.returnableInstance(() -> new URI("file:/test"))));
    }

    @Test
    public void shouldExpandLocationUri() {
        TestRunner.runAsUser("admin", "password", () -> {
            Location location = createFileLocation();
            locationManager.updateLocationConfiguration(location.getId(), new FileLocationConfiguration("/test"));

            assertThat(locationManager.expandLocationUri(Throwables.returnableInstance(() -> new URI("oma-location://" + location.getId()))))
                    .isEqualTo(Throwables.returnableInstance(() -> new URI("file:/test")));
        });
    }

    @Test
    public void shouldExpandLocationUriWithPath() {
        TestRunner.runAsUser("admin", "password", () -> {
            Location location = createFileLocation();
            locationManager.updateLocationConfiguration(location.getId(), new FileLocationConfiguration("/test"));

            assertThat(locationManager.expandLocationUri(Throwables.returnableInstance(() -> new URI("oma-location://" + location.getId() + "/file1.txt"))))
                    .isEqualTo(Throwables.returnableInstance(() -> new URI("file:/test/file1.txt")));
        });
    }

    @Test
    public void shouldFailToExpandLocationUriNotFound() {
        TestRunner.runAsUser("admin", "password", () -> assertThatThrownBy(() -> locationManager.expandLocationUri(Throwables.returnableInstance(() -> new URI("oma-location://123"))))
                .isExactlyInstanceOf(EJBException.class).hasCauseExactlyInstanceOf(NotFoundException.class));
    }

    @Test
    public void shouldFailToExpandLocationUriNotConfigured() {
        TestRunner.runAsUser("admin", "password", () -> {
            Location location = createFileLocation();
            assertThatThrownBy(() -> locationManager.expandLocationUri(Throwables.returnableInstance(() -> new URI("oma-location://" + location.getId()))))
                    .isExactlyInstanceOf(EJBException.class).hasCauseExactlyInstanceOf(LocationConfigurationException.class);
        });
    }

    private Location createFileLocation() {
        return locationManager.createLocation(new Location("test", "test", "FILE"));
    }

    private Location createFileLocation(String name) {
        return locationManager.createLocation(new Location(name, "test", "FILE"));
    }

    private void assertGeneratedFieldsAreCorrect(Location location, String expectedUser) {
        assertThat(location.getCreated()).isNotNull();
        assertThat(location.getCreatedBy()).isEqualTo(expectedUser);
        assertThat(location.getLastModified()).isNotNull();
        assertThat(location.getLastModifiedBy()).isEqualTo(expectedUser);
    }

}