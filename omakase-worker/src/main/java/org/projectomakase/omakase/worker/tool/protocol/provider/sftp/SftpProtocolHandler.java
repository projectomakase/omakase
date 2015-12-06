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
package org.projectomakase.omakase.worker.tool.protocol.provider.sftp;

import com.google.common.base.Strings;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;
import org.projectomakase.omakase.commons.functions.Throwables;
import org.projectomakase.omakase.worker.tool.protocol.HandleProtocol;
import org.projectomakase.omakase.worker.tool.protocol.ProtocolHandler;
import org.projectomakase.omakase.worker.tool.protocol.ProtocolHandlerException;

import java.io.InputStream;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.stream.StreamSupport;

/**
 * {@link ProtocolHandler} implementation that supports the "ftp" protocol.
 *
 * @author Richard Lucas
 */
@HandleProtocol("sftp")
public class SftpProtocolHandler implements ProtocolHandler {

    private static final org.jboss.logging.Logger LOGGER = org.jboss.logging.Logger.getLogger(SftpProtocolHandler.class);

    private URI uri;
    private ChannelSftp channel;

    @Override
    public void init(URI uri) {
        validateUriScheme(uri);
        this.uri = uri;
        connect(getFtpConnectionParametersFromUri(uri));
    }

    @Override
    public InputStream openStream() {
        try {
            isInitiated();
            return channel.get(getPathFromUri(uri));
        } catch (SftpException e) {
            LOGGER.error(e.getMessage(), e);
            throw new ProtocolHandlerException(e);
        }
    }

    /**
     * This method can <b>NOT</b> be called after {#link FtpProtocolHandler#openStream}
     *
     * @return the length of the content identified by the protocol handler URI.
     */
    @Override
    public long getContentLength() {
        isInitiated();
        try {
            List files = channel.ls(getPathFromUri(uri));
            if (files.isEmpty()) {
                throw new ProtocolHandlerException("Failed to get length of of " + uri.toString() + ". It does not exist.");
            }
            if (files.size() > 1) {
                throw new ProtocolHandlerException("Failed to get length of of " + uri.toString() + ". Ambiguous file path.");
            }
            return ((ChannelSftp.LsEntry) files.get(0)).getAttrs().getSize();
        } catch (SftpException e) {
            throw new ProtocolHandlerException("Failed to get length of " + uri.toString(), e);
        }
    }

    @Override
    public void copyTo(InputStream from, long contentLength) {
        isInitiated();
        try {
            String currentDir = channel.pwd();
            Path filePath = Paths.get(getPathFromUri(uri)).normalize();
            StreamSupport.stream(filePath.spliterator(), false).limit(filePath.getNameCount() - 1L).forEach(path -> Throwables.voidInstance(() -> createDirectory(path)));
            channel.cd(currentDir);
            channel.put(from, getPathFromUri(uri));
        } catch (Exception e) {
            throw new ProtocolHandlerException("Failed to copy to " + uri.toString(), e);
        }
    }

    @Override
    public void delete() {
        isInitiated();
        try {
            String currentDir = channel.pwd();
            channel.cd(currentDir);
            channel.rm(getPathFromUri(uri));
        } catch (Exception e) {
            throw new ProtocolHandlerException("Failed to delete from " + uri.toString(), e);
        }
    }

    @Override
    public void close() {
        disconnect();
        uri = null;
    }

    private static void validateUriScheme(URI uri) {
        if (!"sftp".equalsIgnoreCase(uri.getScheme())) {
            throw new ProtocolHandlerException(uri.getScheme() + " is not supported by this protocol handler");
        }
    }

    private void isInitiated() {
        if (uri == null) {
            throw new ProtocolHandlerException("Protocol Handler is not initiated.");
        }
    }

    private SftpConnectionParameters getFtpConnectionParametersFromUri(URI uri) {
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
            port = 22;
        }

        return new SftpConnectionParameters(server, port, credentials[0], credentials[1]);
    }

    private void connect(SftpConnectionParameters param) {
        try {
            JSch jsch = new JSch();
            Session session = jsch.getSession(param.getUser(), param.getServer(), param.getPort());
            session.setConfig("StrictHostKeyChecking", "no");
            session.setPassword(param.getPassword());
            session.connect();
            channel = null;
            channel = (ChannelSftp) session.openChannel("sftp");
            channel.connect();
        } catch (JSchException jse) {
            LOGGER.error(jse.getMessage(), jse);
            throw new ProtocolHandlerException(jse);
        }
    }

    private void disconnect() {
        try {
            if (channel != null && channel.isConnected()) {
                channel.exit();
                Session session = channel.getSession();
                if (session != null && session.isConnected()) {
                    channel.getSession().disconnect();
                }
            }
        } catch (JSchException jse) {
            LOGGER.error(jse.getMessage(), jse);
            throw new ProtocolHandlerException(jse);
        }
    }

    @SuppressWarnings("unchecked")
    private void createDirectory(Path path) throws SftpException {
        List files = channel.ls(channel.pwd());
        Optional entry = files.stream().filter(file -> ((ChannelSftp.LsEntry) file).getFilename().equals(path.toString())).findFirst();
        if (entry.isPresent()) {
            channel.cd(path.toString());
        } else {
            channel.mkdir(path.toString());
            channel.cd(path.toString());
        }
    }

    private static String getPathFromUri(URI uri) {
        String path = uri.getPath();
        if (Strings.isNullOrEmpty(path)) {
            throw new ProtocolHandlerException("URI is missing required path");
        }
        return "./" + path;
    }

    private static class SftpConnectionParameters {
        private final String server;
        private final int port;
        private final String user;
        private final String password;

        public SftpConnectionParameters(String server, int port, String user, String password) {
            this.server = server;
            this.port = port;
            this.user = user;
            this.password = password;
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
    }
}
