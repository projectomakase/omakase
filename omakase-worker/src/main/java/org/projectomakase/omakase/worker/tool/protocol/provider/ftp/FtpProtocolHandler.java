/*
 * #%L
 * omakase-worker
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
package org.projectomakase.omakase.worker.tool.protocol.provider.ftp;

import com.google.common.base.CharMatcher;
import com.google.common.base.Strings;
import org.projectomakase.omakase.commons.functions.Throwables;
import org.projectomakase.omakase.worker.tool.protocol.HandleProtocol;
import org.projectomakase.omakase.worker.tool.protocol.ProtocolHandler;
import org.projectomakase.omakase.worker.tool.protocol.ProtocolHandlerException;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.StreamSupport;

/**
 * {@link ProtocolHandler} implementation that supports the "ftp" protocol.
 *
 * @author Richard Lucas
 */
@HandleProtocol("ftp")
public class FtpProtocolHandler implements ProtocolHandler {

    private static final org.jboss.logging.Logger LOGGER = org.jboss.logging.Logger.getLogger(FtpProtocolHandler.class);

    private URI uri;
    private FTPClient ftpClient;
    private boolean isOpenStream;

    @Override
    public void init(URI uri) {
        validateUriScheme(uri);
        this.uri = uri;
        connect(getFtpConnectionParametersFromUri(uri));
    }

    @Override
    public InputStream openStream() throws IOException {
        isInitiated();
        InputStream inputStream = ftpClient.retrieveFileStream(getPathFromUri(uri));
        if (inputStream != null) {
            isOpenStream = true;
        } else {
            throw new ProtocolHandlerException("Failed to open stream for " + uri.toString() + ". " + CharMatcher.BREAKING_WHITESPACE.trimTrailingFrom(ftpClient.getReplyString()));
        }
        return inputStream;
    }

    /**
     * This method can <b>NOT</b> be called after {#link FtpProtocolHandler#openStream}
     *
     * @return the length of the content identified by the protocol handler URI.
     */
    @Override
    public long getContentLength() {
        isInitiated();
        if (isOpenStream) {
            throw new ProtocolHandlerException("Failed to get length of " + uri.toString() + ". The file current has an open stream.");
        }
        try {
            FTPFile[] ftpFiles = ftpClient.listFiles(getPathFromUri(uri));

            if (ftpFiles.length == 0) {
                throw new ProtocolHandlerException("Failed to get length of " + uri.toString() + ". It does not exist.");
            }
            if (ftpFiles.length > 1) {
                throw new ProtocolHandlerException("Failed to get length of " + uri.toString() + ". Ambiguous file path.");
            }
            return ftpFiles[0].getSize();
        } catch (IOException e) {
            throw new ProtocolHandlerException("Failed to get length of " + uri.toString(), e);
        }
    }

    @Override
    public void copyTo(InputStream from, long contentLength) {
        isInitiated();
        try {
            String currentDir = ftpClient.printWorkingDirectory();
            Path filePath = Paths.get(getPathFromUri(uri));
            StreamSupport.stream(filePath.spliterator(), false).limit(filePath.getNameCount() - 1L).forEach(path -> {
                Throwables.returnableInstance(() -> ftpClient.makeDirectory(path.toString()));
                Throwables.returnableInstance(() -> ftpClient.changeWorkingDirectory(path.toString()));
            });

            ftpClient.changeWorkingDirectory(currentDir);
            if (!ftpClient.storeFile(filePath.toString(), from)) {
                throw new ProtocolHandlerException("Unable to transfer file to ftp location.  Status code:" + ftpClient.getReplyCode() + "  Reason:" + ftpClient.getReplyString());
            }
        } catch(ProtocolHandlerException phe) {
            throw phe;
        } catch (Exception e) {
            throw new ProtocolHandlerException("Failed to copy to " + uri.toString(), e);
        }
    }

    @Override
    public void delete() {
        isInitiated();
        try {
            Path filePath = Paths.get(getPathFromUri(uri));
            if (!ftpClient.deleteFile(filePath.toString())) {
                throw new ProtocolHandlerException("Unable to delete file from ftp location.  Status code:" + ftpClient.getReplyCode() + "  Reason:" + ftpClient.getReplyString());
            }
        } catch(ProtocolHandlerException phe) {
            throw phe;
        } catch (Exception e) {
            throw new ProtocolHandlerException("Failed to delete " + uri.toString(), e);
        }
    }

    @Override
    public void close() {
        try {
            if (ftpClient != null && ftpClient.isConnected()) {
                if (isOpenStream) {
                    ftpClient.completePendingCommand();
                }
                ftpClient.logout();
                ftpClient.disconnect();
            }
            uri = null;
            isOpenStream = false;
        } catch (IOException e) {
            LOGGER.error("Failed to close connection", e);
        }
    }

    private void isInitiated() {
        if (uri == null) {
            throw new ProtocolHandlerException("Protocol Handler is not initiated.");
        }
    }

    private FtpConnectionParameters getFtpConnectionParametersFromUri(URI uri) {
        String userInfo = uri.getUserInfo();
        if (Strings.isNullOrEmpty(userInfo)) {
            throw new ProtocolHandlerException("URI is missing user info");
        }

        String[] credentials = uri.getUserInfo().split(":");
        if (credentials.length != 2) {
            throw new ProtocolHandlerException("URI credentials are invalid");
        }

        String server = uri.getHost();
        if (Strings.isNullOrEmpty(server)) {
            throw new ProtocolHandlerException("URI is missing required host");
        }

        int port = uri.getPort();
        if (port == -1) {
            port = 21;
        }

        boolean passive = true;
        String query = uri.getQuery();
        if (!Strings.isNullOrEmpty(query) && query.contains("passive=false")) {
            passive = false;
            LOGGER.debug("passive mode disabled, using active mode");
        }

        return new FtpConnectionParameters(server, port, credentials[0], credentials[1], passive);
    }

    private void connect(FtpConnectionParameters param) {
        ftpClient = new FTPClient();
        try {
            ftpClient.setBufferSize(0);
            ftpClient.connect(param.getServer(), param.getPort());
            LOGGER.debug(ftpClient.getReplyString());
            ftpClient.login(param.getUser(), param.getPassword());
            ftpClient.setFileType(FTP.BINARY_FILE_TYPE);

            if (param.isPassive()) {
                ftpClient.enterLocalPassiveMode();
            }

        } catch (IOException e) {
            LOGGER.error(e.getMessage(), e);
            throw new ProtocolHandlerException("FTPIllegalReplyException: " + e.getMessage(), e.getCause());
        }
    }

    private static String getPathFromUri(URI uri) {
        String path = uri.getPath();
        if (Strings.isNullOrEmpty(path)) {
            throw new ProtocolHandlerException("URI is missing required path");
        }
        return path.replaceFirst("/", "./");
    }

    private static void validateUriScheme(URI uri) {
        if (!"ftp".equalsIgnoreCase(uri.getScheme())) {
            throw new ProtocolHandlerException(uri.getScheme() + " is not supported by this protocol handler");
        }
    }

    private static class FtpConnectionParameters {
        private final String server;
        private final int port;
        private final String user;
        private final String password;
        private final boolean isPassive;

        public FtpConnectionParameters(String server, int port, String user, String password, boolean isPassive) {
            this.server = server;
            this.port = port;
            this.user = user;
            this.password = password;
            this.isPassive = isPassive;
        }

        public String getServer() {
            return server;
        }

        public int getPort() {
            return port;
        }

        public String getUser() {
            return user;
        }

        public String getPassword() {
            return password;
        }

        public boolean isPassive() {
            return isPassive;
        }
    }
}
