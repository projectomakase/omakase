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
package org.projectomakase.omakase.worker.tool.manifest.dash.model;

import javax.xml.bind.annotation.XmlRegistry;

@XmlRegistry
public class ObjectFactory {

    public SegmentTimelineType createSegmentTimelineType() {
        return new SegmentTimelineType();
    }


    public MPD createMPD() {
        return new MPD();
    }


    public ProgramInformationType createProgramInformationType() {
        return new ProgramInformationType();
    }


    public BaseURLType createBaseURLType() {
        return new BaseURLType();
    }


    public PeriodType createPeriodType() {
        return new PeriodType();
    }


    public MetricsType createMetricsType() {
        return new MetricsType();
    }


    public ContentComponentType createContentComponentType() {
        return new ContentComponentType();
    }


    public RangeType createRangeType() {
        return new RangeType();
    }


    public URLType createURLType() {
        return new URLType();
    }


    public AdaptationSetType createAdaptationSetType() {
        return new AdaptationSetType();
    }


    public SubRepresentationType createSubRepresentationType() {
        return new SubRepresentationType();
    }


    public DescriptorType createDescriptorType() {
        return new DescriptorType();
    }


    public RepresentationBaseType createRepresentationBaseType() {
        return new RepresentationBaseType();
    }


    public SubsetType createSubsetType() {
        return new SubsetType();
    }


    public RepresentationType createRepresentationType() {
        return new RepresentationType();
    }


    public SegmentTemplateType createSegmentTemplateType() {
        return new SegmentTemplateType();
    }


    public MultipleSegmentBaseType createMultipleSegmentBaseType() {
        return new MultipleSegmentBaseType();
    }


    public SegmentListType createSegmentListType() {
        return new SegmentListType();
    }


    public SegmentBaseType createSegmentBaseType() {
        return new SegmentBaseType();
    }


    public SegmentURLType createSegmentURLType() {
        return new SegmentURLType();
    }


    public SegmentTimelineType.S createSegmentTimelineTypeS() {
        return new SegmentTimelineType.S();
    }

}
