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
package org.projectomakase.omakase.location.api;

import org.projectomakase.omakase.jcr.JcrEntity;
import org.projectomakase.omakase.location.spi.LocationConfiguration;
import org.jcrom.annotations.JcrChildNode;
import org.jcrom.annotations.JcrNode;
import org.jcrom.annotations.JcrProperty;

/**
 * Represents an Omakase location.
 *
 * @author Scott Sharp
 */
@JcrNode(mixinTypes = {"omakase:location"})
public class Location extends JcrEntity {

    public static final String LOCATION_NAME = "omakase:name";
    public static final String DESCRIPTION = "omakase:description";
    public static final String TYPE = "omakase:type";

    @JcrProperty(name = LOCATION_NAME)
    private String locationName;
    @JcrProperty(name = DESCRIPTION)
    private String description;
    @JcrProperty(name = Location.TYPE)
    private String type;
    @JcrChildNode(createContainerNode = false)
    private LocationConfiguration locationConfiguration;

    public Location() {
        // required by Jcrom
    }

    public Location(String locationName, String description, String type) {
        this.locationName = locationName;
        this.description = description;
        this.type = type;
    }

    public String getLocationName() {
        return locationName;
    }

    public void setLocationName(String locationName) {
        this.locationName = locationName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public LocationConfiguration getLocationConfiguration() {
        return locationConfiguration;
    }

    public void setLocationConfiguration(LocationConfiguration locationConfiguration) {
        this.locationConfiguration = locationConfiguration;
    }
}
