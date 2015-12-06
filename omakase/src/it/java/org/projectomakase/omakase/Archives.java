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

import org.jboss.shrinkwrap.api.Filters;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;

import java.io.File;

/**
 * Archives
 *
 * @author Richard Lucas
 */
public class Archives {

    private Archives() {
        // hide default constructor
    }

    public static WebArchive omakaseITWar() {
        File[] libraries = Maven.configureResolver().loadPomFromFile("pom.xml").importRuntimeDependencies().resolve("org.assertj:assertj-core").withTransitivity().asFile();
        return omakaseWar(libraries);
    }

    public static WebArchive omakaseRESTITWar() {
        File[] libraries = Maven.configureResolver().loadPomFromFile("pom.xml").importRuntimeDependencies().resolve("org.assertj:assertj-core", "com.jayway.restassured:rest-assured").withTransitivity().asFile();
        return omakaseWar(libraries).addAsWebInfResource(new File("src/main/webapp/WEB-INF/web.xml"), "web.xml");
    }

    private static WebArchive omakaseWar(File[] libraries) {
        return ShrinkWrap.create(WebArchive.class, "omakase-1.1-SNAPSHOT.war")
                .addPackages(true, Filters.exclude(".*IT.*"), OmakaseLifecycle.class.getPackage())
                .deletePackages(true, "org.projectomakase.omakase.commons")
                .deletePackages(true, "org.projectomakase.omakase.task")
                .addAsResource("omakase.properties", "omakase.properties")
                .addAsResource("META-INF/services", "META-INF/services")
                .addAsWebInfResource(new File("src/main/webapp/WEB-INF/jboss-web.xml"), "jboss-web.xml")
                .addAsWebInfResource(new File("src/main/webapp/WEB-INF/jboss-deployment-structure.xml"), "jboss-deployment-structure.xml")
                .addAsWebInfResource(new File("src/main/webapp/WEB-INF/beans.xml"), "beans.xml").addAsLibraries(libraries);
    }


}
