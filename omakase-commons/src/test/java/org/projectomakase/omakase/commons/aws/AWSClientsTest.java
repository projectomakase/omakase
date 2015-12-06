/*
 * #%L
 * omakase-commons
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
package org.projectomakase.omakase.commons.aws;

import com.amazonaws.auth.BasicAWSCredentials;
import org.projectomakase.omakase.commons.aws.s3.S3Upload;
import org.junit.Test;

import java.net.URI;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Richard Lucas
 */
public class AWSClientsTest {

    @Test
    public void shouldGetAWSFormattedDateTimeFromGMTDate() {
        ZonedDateTime zonedDateTime = ZonedDateTime.ofStrict(LocalDateTime.of(2012, 05, 25, 00, 24, 53), ZoneOffset.ofHours(0), ZoneId.of("GMT"));
        String expected = "20120525T002453Z";
        assertThat(AWSClients.getAWSFormattedDateTime(zonedDateTime)).isEqualTo(expected);
    }

    @Test
    public void shouldGetAWSFormattedDateTimeFromPSTDate() {
        ZonedDateTime zonedDateTime = ZonedDateTime.of(LocalDateTime.of(2012, 05, 24, 17, 24, 53), ZoneId.of("America/Los_Angeles"));
        String expected = "20120525T002453Z";
        assertThat(AWSClients.getAWSFormattedDateTime(zonedDateTime)).isEqualTo(expected);
    }

    @Test
    public void shouldCreates3UploadFromURI() throws Exception {
        URI uri = new URI("s3://access:secret@dev.s3-us-west-1.amazonaws.com/objectone?partNumber=1&uploadId=abc");
        S3Upload s3Upload = AWSClients.s3UploadFromURI(uri);
        assertThat(s3Upload)
                .isEqualToIgnoringGivenFields(new S3Upload(new BasicAWSCredentials("access", "secret"), "dev.s3-us-west-1.amazonaws.com", "us-west-1", "/objectone", "dev", "objectone", "abc"),
                        "awsCredentials");
        assertThat(s3Upload.getAwsCredentials()).isEqualToComparingFieldByField(new BasicAWSCredentials("access", "secret"));

    }
}