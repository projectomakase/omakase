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
package org.projectomakase.omakase.content;

import org.jcrom.annotations.JcrChildNode;
import org.jcrom.annotations.JcrName;
import org.jcrom.annotations.JcrNode;
import org.jcrom.annotations.JcrPath;
import org.jcrom.annotations.JcrProperty;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

/**
 * @author Richard Lucas
 */
@JcrNode(mixinTypes = {"omakase:variantFile"})
public class VariantFile {

    public static final String ID = "jcr:name";
    public static final String NAME = "omakase:name";
    public static final String TYPE = "omakase:type";
    public static final String SIZE = "omakase:fileSize";
    public static final String ORIGINAL_FILENAME = "omakase:originalFilename";
    public static final String CREATED = "omakase:fileCreated";

    private static final String ORIGINAL_FILEPATH = "omakase:originalFilePath";

    @JcrName
    protected String id;
    @JcrPath
    protected String path;
    @JcrProperty(name = NAME)
    private String variantFilename;
    @JcrProperty(name = SIZE)
    private Long size;
    @JcrProperty(name = ORIGINAL_FILENAME)
    private String originalFilename;
    @JcrProperty(name = ORIGINAL_FILEPATH)
    private String originalFilepath;
    @JcrProperty(name = CREATED)
    private Date created = new Date();
    @JcrProperty(name = TYPE)
    private VariantFileType type;
    @JcrChildNode
    private List<VariantFileHash> hashes;

    public VariantFile() {
        // required by jcrom
    }

    /**
     * Creates a new virtual variant file. A virtual variant is used to represent a group of files as a single entity e.g. HLS segments are represented as a single stream.
     *
     * @param variantFilename
     *         the virtual variant file name
     */
    public VariantFile(String variantFilename) {
        this.variantFilename = variantFilename;
        this.type = VariantFileType.VIRTUAL;
    }

    /**
     * Creates a new variant file that represents a single file that is NOT a child of a virtual variant file.
     *
     * @param variantFilename
     *         the virtual variant file name
     * @param size
     *         the file size
     * @param originalFilename
     *         the original filename
     * @param originalFilepath
     *         the original filepath
     * @param hashes
     *         hashes calculated for the file
     */
    public VariantFile(String variantFilename, Long size, String originalFilename, String originalFilepath, List<VariantFileHash> hashes) {
        this.variantFilename = variantFilename;
        this.type = VariantFileType.FILE;
        this.size = size;
        this.originalFilename = originalFilename;
        this.originalFilepath = originalFilepath;
        this.hashes = hashes;
    }


    /**
     * Creates a new variant file.
     *
     * @param variantFilename
     *         the virtual variant file name
     * @param type
     *         the virtual file type
     * @param size
     *         the file size
     * @param originalFilename
     *         the original filename
     * @param originalFilepath
     *         the original filepath
     * @param hashes
     *         hashes calculated for the file
     */
    public VariantFile(String variantFilename, VariantFileType type, Long size, String originalFilename, String originalFilepath, List<VariantFileHash> hashes) {
        this.variantFilename = variantFilename;
        this.type = type;
        this.size = size;
        this.originalFilename = originalFilename;
        this.originalFilepath = originalFilepath;
        this.hashes = hashes;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getNodePath() {
        return path;
    }

    public String getVariantFilename() {
        return variantFilename;
    }

    public Long getSize() {
        return size;
    }

    public String getOriginalFilename() {
        return originalFilename;
    }

    public String getOriginalFilepath() {
        return originalFilepath;
    }

    public List<VariantFileHash> getHashes() {
        return Optional.ofNullable(hashes).orElse(new ArrayList<>());
    }

    public Date getCreated() {
        return created;
    }

    public VariantFileType getType() {
        return type;
    }

    public void setType(VariantFileType type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return "VariantFile{" +
                "id='" + id + '\'' +
                ", path='" + path + '\'' +
                ", variantFilename='" + variantFilename + '\'' +
                ", size=" + size +
                ", originalFilename='" + originalFilename + '\'' +
                ", originalFilepath='" + originalFilepath + '\'' +
                ", created=" + created +
                ", type=" + type +
                ", hashes=" + hashes +
                '}';
    }
}
