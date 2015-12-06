/*
 * #%L
 * omakase-tool-manifest
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
package org.projectomakase.omakase.worker.tool.manifest.dash;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import org.projectomakase.omakase.commons.functions.Throwables;
import org.projectomakase.omakase.worker.tool.ToolException;
import org.projectomakase.omakase.worker.tool.manifest.ManifestFileBuilder;
import org.projectomakase.omakase.worker.tool.manifest.ManifestParser;
import org.projectomakase.omakase.worker.tool.manifest.ManifestParserResult;
import org.projectomakase.omakase.worker.tool.manifest.dash.model.AdaptationSetType;
import org.projectomakase.omakase.worker.tool.manifest.dash.model.BaseURLType;
import org.projectomakase.omakase.worker.tool.manifest.dash.model.MPD;
import org.projectomakase.omakase.worker.tool.manifest.dash.model.PeriodType;
import org.projectomakase.omakase.worker.tool.manifest.dash.model.RepresentationType;
import org.projectomakase.omakase.worker.tool.manifest.dash.model.SegmentBaseType;
import org.projectomakase.omakase.worker.tool.manifest.dash.model.SegmentListType;
import org.projectomakase.omakase.worker.tool.manifest.dash.model.SegmentTemplateType;
import org.jboss.logging.Logger;
import org.projectomakase.omakase.commons.collectors.ImmutableListCollector;
import org.xml.sax.SAXException;

import javax.inject.Inject;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.ValidationEvent;
import javax.xml.bind.ValidationEventLocator;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.SchemaFactory;
import java.io.InputStream;
import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;

/**
 * DASH MPD Parser.
 * <p>
 * The following assumptions are currently made about the MPD being parsed:
 * </p>
 * <ul>
 * <li>Each element will contain 0 or 1 relative base URLs. If there are more than one or the URLs are absolute they will be ignored.</li>
 * <li>All files in declared in the manifest are relative to the manifest URI.</li>
 * <li>Manifest will not contain segment lists or segment types</li>
 * <li>Xlink URLs are not used</li>
 * <li>No custom XSDs are declared/referenced</li>
 * </ul>
 *
 * @author Richard Lucas
 */
public class DashManifestParser implements ManifestParser {

    private static final Logger LOGGER = Logger.getLogger(DashManifestParser.class);

    @Inject
    ManifestFileBuilder manifestFileBuilder;

    @Override
    public ManifestParserResult parse(URI manifestUri, InputStream inputStream) {
        StreamSource[] streamSources = new StreamSource[]{new StreamSource(DashManifestParser.class.getResourceAsStream("/xlink.xsd")),
                new StreamSource(DashManifestParser.class.getResourceAsStream("/DASH-MPD.xsd"))};

        try {
            JAXBContext context = JAXBContext.newInstance(MPD.class);
            Unmarshaller unmarshaller = context.createUnmarshaller();
            SchemaFactory schemaFactory = SchemaFactory.newInstance(javax.xml.XMLConstants.W3C_XML_SCHEMA_NS_URI);
            unmarshaller.setSchema(schemaFactory.newSchema(streamSources));
            ImmutableList.Builder<String> errorListBuilder = ImmutableList.builder();
            unmarshaller.setEventHandler(validationEvent -> handleValidationEvent(errorListBuilder, validationEvent));
            JAXBElement<MPD> element = unmarshaller.unmarshal(new StreamSource(inputStream), MPD.class);

            List<String> errors = errorListBuilder.build();
            if (!errors.isEmpty()) {
                throw new ToolException("Failed to parse DASH MPD due to validation errors. " + errors.stream().collect(Collectors.joining(", ")));
            }

            MPD mpd = element.getValue();

            ImmutableList.Builder<URI> uriListBuilder = ImmutableList.builder();
            String rootPath = getRelativePathFromBaseURLsWithPathSeparator(mpd.getBaseURLs());
            mpd.getPeriods().forEach(period -> parsePeriod(uriListBuilder, rootPath, period));

            manifestFileBuilder.init(manifestUri);
            return new ManifestParserResult(ImmutableList.of(), uriListBuilder.build().stream().map(uri -> manifestFileBuilder.build(uri)).collect(ImmutableListCollector.toImmutableList()));

        } catch (JAXBException e) {
            LOGGER.error("Failed to parse DASH MPD", e);
            throw new ToolException("Failed to parse DASH MPD", e);
        } catch (SAXException e) {
            LOGGER.error("Failed to get DASH MPD XSD", e);
            throw new ToolException("Failed to get DASH MPD XSD", e);
        }
    }


    private static boolean handleValidationEvent(ImmutableList.Builder<String> errorList, ValidationEvent validationEvent) {
        if (validationEvent.getSeverity() != ValidationEvent.WARNING) {
            ValidationEventLocator locator = validationEvent.getLocator();
            errorList.add("Line:Col[" + locator.getLineNumber() +
                                  ":" + locator.getColumnNumber() +
                                  "]:" + validationEvent.getMessage());
        }
        return true;
    }

    private static void parsePeriod(ImmutableList.Builder<URI> uriListBuilder, String rootPath, PeriodType period) {
        validateSegmentList(period.getSegmentList());
        validateSegmentTemplate(period.getSegmentTemplate());
        String periodPath = getPathWithPrefix(rootPath, getRelativePathFromBaseURLsWithPathSeparator(period.getBaseURLs()));
        uriListBuilder.addAll(getFilesFromSegmentBase(periodPath, period.getSegmentBase()));
        period.getAdaptationSets().forEach(adaptationSet -> parseAdaptationSet(uriListBuilder, periodPath, adaptationSet));
    }

    private static void parseAdaptationSet(ImmutableList.Builder<URI> uriListBuilder, String periodPath, AdaptationSetType adaptationSet) {
        validateSegmentList(adaptationSet.getSegmentList());
        validateSegmentTemplate(adaptationSet.getSegmentTemplate());
        String adaptationSetPath = getPathWithPrefix(periodPath, getRelativePathFromBaseURLsWithPathSeparator(adaptationSet.getBaseURLs()));
        uriListBuilder.addAll(getFilesFromSegmentBase(adaptationSetPath, adaptationSet.getSegmentBase()));
        adaptationSet.getRepresentations().forEach(representation -> parseRepresentation(uriListBuilder, adaptationSetPath, representation));
    }

    private static void parseRepresentation(ImmutableList.Builder<URI> uriListBuilder, String adaptationSetPath, RepresentationType representation) {
        validateSegmentList(representation.getSegmentList());
        validateSegmentTemplate(representation.getSegmentTemplate());
        uriListBuilder.add(Throwables.returnableInstance(() -> new URI(getPathWithPrefix(adaptationSetPath, getRelativePathFromBaseURLs(representation.getBaseURLs())))));
        uriListBuilder.addAll(getFilesFromSegmentBase(adaptationSetPath, representation.getSegmentBase()));
    }

    private static void validateSegmentList(SegmentListType segmentListType) {
        if (segmentListType != null) {
            LOGGER.error("DASH MPD files with Segment lists are not supported");
            throw new ToolException("DASH MPD files with Segment lists are not supported");
        }
    }

    private static void validateSegmentTemplate(SegmentTemplateType segmentTemplateType) {
        if (segmentTemplateType != null) {
            LOGGER.error("DASH MPD files with Segment templates are not supported");
            throw new ToolException("DASH MPD files with Segment templates are not supported");
        }
    }

    private static List<URI> getFilesFromSegmentBase(String pathPrefix, SegmentBaseType segmentBaseType) {
        ImmutableList.Builder<URI> uriListBuilder = ImmutableList.builder();
        if (segmentBaseType != null) {
            if (segmentBaseType.getRepresentationIndex() != null && segmentBaseType.getRepresentationIndex().getSourceURL() != null) {
                uriListBuilder.add(Throwables.returnableInstance(() -> new URI(getPathWithPrefix(pathPrefix, segmentBaseType.getRepresentationIndex().getSourceURL()))));
            }
            if (segmentBaseType.getInitialization() != null && segmentBaseType.getInitialization().getSourceURL() != null) {
                uriListBuilder.add(Throwables.returnableInstance(() -> new URI(getPathWithPrefix(pathPrefix, segmentBaseType.getInitialization().getSourceURL()))));
            }
        }
        return uriListBuilder.build();
    }


    private static String getPathWithPrefix(String prefix, String path) {
        if (Strings.isNullOrEmpty(prefix)) {
            return path;
        } else {
            return prefix + path;
        }
    }

    private static String getRelativePathFromBaseURLsWithPathSeparator(List<BaseURLType> baseURLTypes) {
        return baseURLTypes.stream()
                .map(baseURL -> Throwables.returnableInstance(() -> new URI(baseURL.getValue()))).filter(uri -> !uri.isAbsolute())
                .findFirst()
                .map(uri -> uri.toString() + "/").orElse("");
    }

    private static String getRelativePathFromBaseURLs(List<BaseURLType> baseURLTypes) {
        return baseURLTypes.stream()
                .map(baseURL -> Throwables.returnableInstance(() -> new URI(baseURL.getValue()))).filter(uri -> !uri.isAbsolute())
                .findFirst()
                .map(URI::toString).orElse("");
    }
}
