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
/**
 * Apache Camel integration.
 * <p>
 * Camel is used to abstract the different queuing technologies supported by Omakase. This enables robust queue integrations using Camels powerful DSL.
 * </p>
 * <p>
 * Camel is integrated into the Java EE framework allowing the use of CDI to inject camel components. Camel uses the Java EE Managed Executor Service when creating threads to avoid
 * the creation of un-managed threads.
 * </p>
 *
 * @author Richard Lucas
 */
package org.projectomakase.omakase.camel;