/**
 * Copyright 2014 Fernando Rincon Martin <frm.rincon@gmail.com>
 *
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
 */
package org.openeos.tools.maven.plugins;

import java.io.File;

import org.apache.maven.plugin.testing.AbstractMojoTestCase;
import org.codehaus.plexus.configuration.PlexusConfiguration;

public class GenerateEntitiesMojoTestCase extends AbstractMojoTestCase {

	private static final String CONFIG_FILE = "generate-entities-plugin.xml";

	@Override
	protected void setUp() throws Exception {
		super.setUp();
	}

	public void testMojoExecution() throws Exception {
		GenerateEntitiesMojo mojo = new GenerateEntitiesMojo();
		PlexusConfiguration config = loadConfiguration(CONFIG_FILE);
		configureMojo(mojo, config);
	}

	protected PlexusConfiguration loadConfiguration(String configFile) throws Exception {
		File testPom = new File(getBasedir(), "target/test-classes/" + configFile);
		assertTrue("The configuration pom could not be found, " + testPom.getAbsolutePath(), testPom.exists());

		PlexusConfiguration config = extractPluginConfiguration("generate-entities-maven-plugin", testPom);
		assertNotNull("There should be a configuration for the plugin in the pom", config);
		return config;
	}

}
