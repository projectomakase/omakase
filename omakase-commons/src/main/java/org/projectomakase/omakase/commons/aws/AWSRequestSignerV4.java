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
import com.google.common.collect.Multimap;
import com.google.common.collect.SortedSetMultimap;
import com.google.common.collect.TreeMultimap;
import com.google.common.hash.HashCode;
import com.google.common.hash.Hashing;
import com.google.common.io.BaseEncoding;
import org.apache.http.Header;
import org.apache.http.HttpMessage;
import org.apache.http.client.methods.HttpUriRequest;
import org.jboss.logging.Logger;
import org.projectomakase.omakase.commons.exceptions.OmakaseRuntimeException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.google.common.base.Charsets.UTF_8;

/**
 * AWS Request Signer V4 implementation.
 *
 * @author Richard Lucas
 */
public class AWSRequestSignerV4 {

    private static final Logger LOGGER = Logger.getLogger(AWSRequestSignerV4.class);

    private static final String AUTH_TAG = "AWS4";
    private static final String ALGORITHM = AUTH_TAG + "-HMAC-SHA256";
    private static final String TERMINATION_STRING = "aws4_request";

    /**
     * Creates a AWS V4 Signature for the given http request.
     *
     * @param request
     *         the HTTP request
     * @param service
     *         the AWS service e.g. S3
     * @param region
     *         the AWS region
     * @param endpoint
     *         the AWS endpoint the request is being sent to
     * @param awsCredentials
     *         the AWS credentials used to sign the request
     * @param contentSha256Hash
     *         the sha256 hash of the requests payload.
     * @return the AWS V4 signature for the given request.
     */
    public String createV4Signature(HttpUriRequest request, String service, String region, String endpoint, AWSCredentials awsCredentials, String contentSha256Hash) {

        Multimap<String, String> canonicalizedHeadersMap = buildCanonicalizedHeadersMap(request);
        String canonicalizedQueryParams = Optional.ofNullable(request.getURI().getQuery()).orElse("");
        String canonicalizedHeadersString = buildCanonicalizedHeadersString(canonicalizedHeadersMap);
        String signedHeaders = buildSignedHeaders(canonicalizedHeadersMap);

        String date = request.getFirstHeader("X-Amz-Date").getValue();
        String dateWithoutTimestamp = formatDateWithoutTimestamp(date);
        String credentialScope = buildCredentialScope(dateWithoutTimestamp, service, region);

        // Task 1: Create a Canonical Request For Signature Version 4.
        HashCode hashedCanonicalRequest = buildHashedCanonicalRequest(request.getMethod(), endpoint, canonicalizedQueryParams, contentSha256Hash, canonicalizedHeadersString, signedHeaders);

        // Task 2: Create a String to Sign for Signature Version 4.
        String stringToSign = createStringToSign(date, credentialScope, hashedCanonicalRequest);

        // Task 3: Calculate the AWS Signature Version 4.
        String signature = buildSignature(awsCredentials.getAWSSecretKey(), dateWithoutTimestamp, stringToSign, service, region);

        // Sign the request
        return buildAuthHeader(awsCredentials.getAWSAccessKeyId(), credentialScope, signedHeaders, signature);
    }

    private static Mac hmacSHA256(byte[] key) throws NoSuchAlgorithmException, InvalidKeyException {
        String algorithm = "HmacSHA256";
        Mac mac = Mac.getInstance(algorithm);
        SecretKeySpec signingKey = new SecretKeySpec(key, algorithm);
        mac.init(signingKey);
        return mac;
    }

    private static HashCode buildHashedCanonicalRequest(String method, String endpoint, String query, String linearHash, String canonicalizedHeadersString, String signedHeaders) {
        String canonicalRequest = method + "\n" + endpoint + "\n" + query + "\n" + canonicalizedHeadersString + "\n" + signedHeaders + "\n" + linearHash;
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Canonical request:\n" + canonicalRequest);
        }
        return Hashing.sha256().newHasher().putString(canonicalRequest, UTF_8).hash();
    }

    private static String createStringToSign(String date, String credentialScope, HashCode hashedCanonicalRequest) {
        return ALGORITHM + "\n" + date + "\n" + credentialScope + "\n" + hashedCanonicalRequest.toString();
    }

    private static String formatDateWithoutTimestamp(String date) {
        return date.substring(0, 8);
    }

    private static String buildCredentialScope(String dateWithoutTimeStamp, String service, String region) {
        return dateWithoutTimeStamp + "/" + region + "/" + service + "/" + TERMINATION_STRING;
    }

    private static Multimap<String, String> buildCanonicalizedHeadersMap(HttpMessage request) {
        Header[] headers = request.getAllHeaders();
        SortedSetMultimap<String, String> canonicalizedHeaders = TreeMultimap.create();
        for (Header header : headers) {
            if (header.getName() == null) {
                continue;
            }
            String key = header.getName().toLowerCase();
            canonicalizedHeaders.put(key, header.getValue());
        }
        return canonicalizedHeaders;
    }

    private static String buildCanonicalizedHeadersString(Multimap<String, String> canonicalizedHeadersMap) {
        StringBuilder canonicalizedHeadersBuffer = new StringBuilder();
        for (Map.Entry<String, String> header : canonicalizedHeadersMap.entries()) {
            String key = header.getKey();
            canonicalizedHeadersBuffer.append(key).append(':').append(header.getValue()).append('\n');
        }
        return canonicalizedHeadersBuffer.toString();
    }

    private static String buildSignedHeaders(Multimap<String, String> canonicalizedHeadersMap) {
        return canonicalizedHeadersMap.keySet().stream().map(String::toLowerCase).collect(Collectors.joining(";"));
    }

    private static String buildAuthHeader(String accessKey, String credentialScope, String signedHeaders, String signature) {
        return ALGORITHM + " " + "Credential=" + accessKey + "/" + credentialScope + ", " + "SignedHeaders=" + signedHeaders + ", " + "Signature=" + signature;
    }

    private static byte[] hmacSha256(byte[] key, String s) {
        try {
            Mac hmacSHA256 = hmacSHA256(key);
            return hmacSHA256.doFinal(s.getBytes(UTF_8));
        } catch (Exception e) {
            throw new OmakaseRuntimeException("Error signing request", e);
        }
    }

    private static String buildSignature(String secretKey, String dateWithoutTimestamp, String stringToSign, String service, String region) {

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("String to sign:\n" + stringToSign);
        }

        byte[] kSecret = (AUTH_TAG + secretKey).getBytes(UTF_8);
        byte[] kDate = hmacSha256(kSecret, dateWithoutTimestamp);
        byte[] kRegion = hmacSha256(kDate, region);
        byte[] kService = hmacSha256(kRegion, service);
        byte[] kSigning = hmacSha256(kService, TERMINATION_STRING);
        return BaseEncoding.base16().encode(hmacSha256(kSigning, stringToSign)).toLowerCase();
    }
}
