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

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import org.projectomakase.omakase.commons.aws.glacier.GlacierUpload;
import org.projectomakase.omakase.commons.aws.s3.S3Upload;

import java.net.URI;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.SignStyle;
import java.util.List;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkArgument;
import static java.time.temporal.ChronoField.DAY_OF_MONTH;
import static java.time.temporal.ChronoField.HOUR_OF_DAY;
import static java.time.temporal.ChronoField.MINUTE_OF_HOUR;
import static java.time.temporal.ChronoField.MONTH_OF_YEAR;
import static java.time.temporal.ChronoField.SECOND_OF_MINUTE;
import static java.time.temporal.ChronoField.YEAR;

/**
 * Utility methods used by the different AWS Client implementations.
 *
 * @author Richard Lucas
 */
public final class AWSClients {

    private AWSClients() {
        // hide default constructor
    }

    /**
     * Returns the specified date/time formatted following the AWS requirements. The date/time is converted to GMT prior to formatting.
     *
     * @param zonedDateTime
     *         the date/time
     * @return the specified date/time formatted following the AWS requirements.
     */
    public static String getAWSFormattedDateTime(ZonedDateTime zonedDateTime) {
        // AWS expects the date and time to be GMT
        ZonedDateTime gmtDateTime = zonedDateTime.withZoneSameInstant(ZoneId.of("GMT"));
        return getAwsDateTimeFormatter().format(gmtDateTime);
    }

    public static GlacierUpload glacierUploadFromURI(URI uploadURI) {

        String host = uploadURI.getHost();
        String region = glacierHostToRegion(host);


        String endpoint = uploadURI.getPath();

        List<String> pathParts = Splitter.on("/").omitEmptyStrings().splitToList(endpoint);

        String vault = pathParts.get(2);
        String uploadId = null;
        if (pathParts.size() == 5) {
            uploadId = pathParts.get(4);
        }

        return new GlacierUpload(credentialsFromUri(uploadURI), host, endpoint, region, vault, uploadId);
    }

    public static String glacierHostToRegion(String host) {
        return host.replace("glacier.", "").replace(".amazonaws.com", "").toLowerCase();
    }

    public static S3Upload s3UploadFromURI(URI uploadURI) {
        String host = uploadURI.getHost();
        String region = s3HostToRegion(host);
        String endpoint = uploadURI.getPath();
        String bucket = host.substring(0, host.indexOf("."));

//        List<String> pathParts = Splitter.on("/").omitEmptyStrings().splitToList(endpoint);
//        checkArgument(pathParts.size() == 1, "Invalid S3 upload part endpoint");

        String key = Optional.of(endpoint).filter(string -> string.startsWith("/")).map(string -> string.replaceFirst("/", "")).orElse(endpoint);
        String uploadId = Optional.ofNullable(uploadURI.getQuery()).map(query -> Splitter.on("&").withKeyValueSeparator("=").split(uploadURI.getQuery()).get("uploadId")).orElse(null);
        return new S3Upload(credentialsFromUri(uploadURI), host, region, endpoint, bucket, key, uploadId);
    }

    public static String s3HostToRegion(String host) {
        return host.substring(host.indexOf(".") + 1).replace("s3-", "").replace(".amazonaws.com", "").toLowerCase();
    }

    public static AWSCredentials credentialsFromUri(URI uri) {
        String userInfo = uri.getUserInfo();
        checkArgument(!Strings.isNullOrEmpty(userInfo), "URI is missing user info");

        String[] credentials = uri.getUserInfo().split(":");
        checkArgument(credentials.length == 2, "URI credentials are invalid");

        return new BasicAWSCredentials(credentials[0], credentials[1]);
    }

    private static DateTimeFormatter getAwsDateTimeFormatter() {
        return new DateTimeFormatterBuilder().appendValue(YEAR, 4, 10, SignStyle.EXCEEDS_PAD).appendValue(MONTH_OF_YEAR, 2).appendValue(DAY_OF_MONTH, 2).appendLiteral('T').appendValue(HOUR_OF_DAY, 2)
                .appendValue(MINUTE_OF_HOUR, 2).appendValue(SECOND_OF_MINUTE, 2).appendLiteral("Z").toFormatter();
    }



}
