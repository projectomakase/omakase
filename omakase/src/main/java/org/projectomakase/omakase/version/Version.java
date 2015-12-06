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
package org.projectomakase.omakase.version;

import org.jboss.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.Properties;

/**
 * Application version.
 *
 * @author Richard Lucas
 */
@ApplicationScoped
public class Version implements Serializable {

    private static final Logger LOGGER = Logger.getLogger(Version.class);

    private String version = "1.1-SNAPSHOT";
    private String buildTag = "DEV";
    private String buildTime = "";

    public Version() {
        try (InputStream inputStream = Version.class.getResourceAsStream("/version.properties")) {
            if (inputStream != null) {
                Properties properties = new Properties();
                properties.load(inputStream);
                version = properties.getProperty("version");
                buildTag = properties.getProperty("buildTag");
                buildTime = properties.getProperty("buildTime");
            }
        } catch (IOException e) {
            LOGGER.error("Failed to read version.properties", e);
        }

    }


    public String getVersion() {
        return version;
    }

    public String getBuildTag() {
        return buildTag;
    }

    public String getBuildTime() {
        return buildTime;
    }
}
