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
package org.projectomakase.omakase.location.provider.net;

import org.projectomakase.omakase.jcr.JcrEntity;
import org.projectomakase.omakase.location.spi.LocationConfiguration;
import org.jcrom.annotations.JcrNode;
import org.jcrom.annotations.JcrProperty;

/**
 * Net Location Configuration (e.g. FTP, SFTP, HTTP, HTTPS).
 *
 * @author Richard Lucas
 */
@JcrNode(mixinTypes = {NetLocationConfiguration.MIXIN}, classNameProperty = "className")
public class NetLocationConfiguration extends JcrEntity implements LocationConfiguration {

    static final String MIXIN = "omakase:locationNetConfiguration";

    @JcrProperty(name = "omakase:address")
    private String address;
    @JcrProperty(name = "omakase:port")
    private long port;
    @JcrProperty(name = "omakase:root")
    private String root;
    @JcrProperty(name = "omakase:username")
    private String username;
    @JcrProperty(name = "omakase:password")
    private String password;

    /**
     * Empty constructor
     */
    public NetLocationConfiguration() {
        // required by Jcrom
    }

    public NetLocationConfiguration(String address, long port) {
        this.address = address;
        this.port = port;
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

    public String getRoot() {
        return root;
    }

    public void setRoot(String root) {
        this.root = root;
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
}
