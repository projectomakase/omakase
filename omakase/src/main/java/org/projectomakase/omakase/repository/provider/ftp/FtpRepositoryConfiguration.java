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
package org.projectomakase.omakase.repository.provider.ftp;


import org.projectomakase.omakase.jcr.JcrEntity;
import org.projectomakase.omakase.repository.spi.RepositoryConfiguration;
import org.jcrom.annotations.JcrNode;
import org.jcrom.annotations.JcrProperty;

/**
 * FTP Repository Configuration.
 *
 * @author Scott Sharp
 */
@JcrNode(mixinTypes = {FtpRepositoryConfiguration.MIXIN}, classNameProperty = "className")
public class FtpRepositoryConfiguration extends JcrEntity implements RepositoryConfiguration {

    public static final String MIXIN = "omakase:repositoryFtpConfiguration";

    @JcrProperty(name = "omakase:address")
    private String address;

    @JcrProperty(name = "omakase:port")
    private long port;

    @JcrProperty(name = "omakase:username")
    private String username;

    @JcrProperty(name = "omakase:password")
    private String password;

    @JcrProperty(name = "omakase:root")
    private String root;

    @JcrProperty(name = "omakase:passive")
    private boolean passive;

    /**
     * Empty constructor
     */
    public FtpRepositoryConfiguration() {
        // required by Jcrom
    }

    public FtpRepositoryConfiguration(String address, long port, String username, String password, String root) {
        this.address = address;
        this.port = port;
        this.username = username;
        this.password = password;
        this.root = root;
        passive = false;
    }

    public FtpRepositoryConfiguration(String address, long port, String username, String password, String root, boolean passive) {
        this.address = address;
        this.port = port;
        this.username = username;
        this.password = password;
        this.root = root;
        this.passive = passive;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public long getPort() {
        return port;
    }

    public void setPort(long port) {
        this.port = port;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getRoot() {
        return root;
    }

    public void setRoot(String root) {
        this.root = root;
    }

    public boolean getPassive() {
        return passive;
    }

    public void setPassive(boolean passive) {
        this.passive = passive;
    }
}
