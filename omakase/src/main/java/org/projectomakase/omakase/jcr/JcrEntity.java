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
package org.projectomakase.omakase.jcr;

import org.jcrom.annotations.JcrName;
import org.jcrom.annotations.JcrPath;
import org.jcrom.annotations.JcrProtectedProperty;

import java.util.Date;

/**
 * Abstract JCR entity implementation with protected variables for name, nodePath, created, createdBy, lastModified, and lastModifiedBy.
 * <p>
 * Implements public getters for nodePath, created, createdBy, lastModified, and lastModifiedBy.
 * </p>
 *
 * @author Richard Lucas
 */
public class JcrEntity {
    public static final String ID = "jcr:name";
    public static final String CREATED = "jcr:created";
    public static final String CREATED_BY = "jcr:createdBy";
    public static final String LAST_MODIFIED = "jcr:lastModified";
    public static final String LAST_MODIFIED_BY = "jcr:lastModifiedBy";

    @JcrName
    protected String name;
    @JcrPath
    protected String path;
    @JcrProtectedProperty(name = CREATED)
    protected Date created;
    @JcrProtectedProperty(name = CREATED_BY)
    protected String createdBy;
    @JcrProtectedProperty(name = LAST_MODIFIED)
    protected Date lastModified;
    @JcrProtectedProperty(name = LAST_MODIFIED_BY)
    protected String lastModifiedBy;

    public String getId() {
        return this.name;
    }

    public void setId(String id) {
        this.name = id;
    }

    public String getNodePath() {
        return path;
    }

    public Date getCreated() {
        return created;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public Date getLastModified() {
        return lastModified;
    }

    public String getLastModifiedBy() {
        return lastModifiedBy;
    }
}
