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

import com.google.common.collect.ImmutableSet;
import org.projectomakase.omakase.Omakase;
import org.projectomakase.omakase.jcr.AbstractJcrDAO;
import org.projectomakase.omakase.jcr.JcrThrowables;
import org.jcrom.Jcrom;

import javax.inject.Inject;
import javax.jcr.Node;
import javax.jcr.Session;
import java.nio.file.Paths;
import java.util.Set;

/**
 * Variant File DAO.
 *
 * @author Richard Lucas
 */
public class VariantFileDAO extends AbstractJcrDAO<VariantFile> {

    @Inject
    public VariantFileDAO(Session session, @Omakase Jcrom jcrom) {
        super(session, jcrom);
    }

    @Override
    public VariantFile distributedCreate(String parentNodePath, VariantFile entity, String uuid) {
        JcrThrowables.wrapJcrExceptions(() -> {
            Node parentNode = session.getNode(parentNodePath);
            if (!parentNode.hasNode("files")) {
                parentNode.addNode("files");
            }
        });
        return super.distributedCreate(parentNodePath + "/files", entity, uuid);
    }

    @Override
    public String getDistributedNodePath(String parentNodePath, String uuid) {
        return super.getDistributedNodePath(parentNodePath + "/files", uuid);
    }

    /**
     * Returns all of the child {@link VariantFile} instances for the give parent.
     *
     * @param parentNodePath
     *         the parent's node path
     * @return all of the child {@link VariantFile} instances for the give parent.
     */
    public Set<VariantFile> getChildVariantFiles(String parentNodePath) {
        String sql = "SELECT file.* FROM [omakase:variantFile] AS file " + "WHERE ISDESCENDANTNODE('" + parentNodePath + "')";
        return ImmutableSet.copyOf(findBySql(sql, null));
    }

    /**
     * Efficiently removes all files from the variant at the specified path.
     *
     * @param variantPath
     *         the variant path
     */
    public void removeAllFilesFromVariant(String variantPath) {
        super.remove(variantPath + "/files");
    }

    /**
     * Returns the asset id of the {@link VariantFile}.
     *
     * @param variantFile
     *         the {@link VariantFile}
     * @return the asset id of the {@link VariantFile}.
     */
    public String getAssetId(VariantFile variantFile) {
        return getIdFromPath(variantFile, 6, 7);
    }

    /**
     * Returns the variant id of the {@link VariantFile}.
     *
     * @param variantFile
     *         the {@link VariantFile}
     * @return the variant id of the {@link VariantFile}.
     */
    public String getVariantId(VariantFile variantFile) {
        return getIdFromPath(variantFile, 7, 8);
    }

    private static String getIdFromPath(VariantFile variantFile, int beginIndex, int endIndex) {
        return Paths.get(variantFile.getNodePath()).subpath(beginIndex, endIndex).toString();
    }
}

