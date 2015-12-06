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
package org.projectomakase.omakase.repository.spi;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * RepositoryConfiguration interface. RepositoryProvider implementations are required to provide an implementation of this interface that defines the configuration properties available for that
 * provider.
 *
 * @author Richard Lucas
 */
@JsonIgnoreProperties({"id", "node_path"})
public interface RepositoryConfiguration {
}
