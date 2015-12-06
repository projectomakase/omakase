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
package org.projectomakase.omakase.worker.tool.manifest.smooth;

import com.google.common.collect.ImmutableList;
import org.projectomakase.omakase.commons.collectors.ImmutableListCollector;
import org.projectomakase.omakase.commons.functions.Throwables;
import org.projectomakase.omakase.worker.tool.ToolException;
import org.projectomakase.omakase.worker.tool.manifest.ManifestFileBuilder;
import org.projectomakase.omakase.worker.tool.manifest.ManifestParser;
import org.projectomakase.omakase.worker.tool.manifest.ManifestParserResult;
import org.jboss.logging.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.inject.Inject;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.List;
import java.util.stream.IntStream;

/**
 * @author Richard Lucas
 */
public class SmoothManifestParser implements ManifestParser {

    private static final Logger LOGGER = Logger.getLogger(SmoothManifestParser.class);

    @Inject
    ManifestFileBuilder manifestFileBuilder;

    @Override
    public ManifestParserResult parse(URI manifestUri, InputStream inputStream) {
        try {
            DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
            Document document = documentBuilder.parse(inputStream);

            XPathFactory xPathFactory = XPathFactory.newInstance();
            XPath xPath = xPathFactory.newXPath();

            ImmutableList.Builder<String> files = ImmutableList.builder();
            files.add(getClientManifest(document, xPath));
            files.addAll(getVideoSrcFiles(document, xPath));
            files.addAll(getAudioSrcFiles(document, xPath));

            manifestFileBuilder.init(manifestUri);
            return new ManifestParserResult(ImmutableList.of(), files.build().stream()
                    .map(file -> manifestFileBuilder.build(Throwables.returnableInstance(() -> new URI(file))))
                    .collect(ImmutableListCollector.toImmutableList()));

        } catch (ParserConfigurationException | SAXException | IOException | XPathExpressionException e) {
            LOGGER.error("Failed to parse DASH MPD", e);
            throw new ToolException("Failed to parse SmoothStreaming ISM", e);
        }
    }

    private static String getClientManifest(Document document, XPath xPath) throws XPathExpressionException {
        XPathExpression ismcExpression = xPath.compile("/smil/head/meta/@content");
        return  (String) ismcExpression.evaluate(document, XPathConstants.STRING);
    }

    private static List<String> getVideoSrcFiles(Document document, XPath xPath) throws XPathExpressionException {
        XPathExpression ismcExpression = xPath.compile("/smil/body/switch/video/@src");
        return getSrcFiles(document, ismcExpression);
    }

    private static List<String> getAudioSrcFiles(Document document, XPath xPath) throws XPathExpressionException {
        XPathExpression ismcExpression = xPath.compile("/smil/body/switch/audio/@src");
        return getSrcFiles(document, ismcExpression);
    }


    private static List<String> getSrcFiles(Document document, XPathExpression ismcExpression) throws XPathExpressionException {
        NodeList nodeList = (NodeList) ismcExpression.evaluate(document, XPathConstants.NODESET);
        return IntStream
                .range(0, nodeList.getLength())
                .mapToObj(nodeList::item)
                .map(Node::getNodeValue)
                .collect(ImmutableListCollector.toImmutableList());
    }
}
