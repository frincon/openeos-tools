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

/*
 * Copyright 2001-2005 The Apache Software Foundation.
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

import static org.twdata.maven.mojoexecutor.MojoExecutor.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.apache.commons.io.IOUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.dependency.graph.DependencyGraphBuilder;
import org.apache.maven.shared.dependency.graph.DependencyGraphBuilderException;
import org.apache.maven.shared.dependency.graph.DependencyNode;
import org.apache.maven.shared.dependency.tree.DependencyTreeBuilder;
import org.twdata.maven.mojoexecutor.MojoExecutor.Element;

/**
 * Goal which touches a timestamp file.
 * 
 */
@Mojo(name = "generate-entities", defaultPhase = LifecyclePhase.GENERATE_SOURCES)
public class GenerateEntitiesMojo extends AbstractMojo {

	private static final String ARTIFACT_ID = "generate-entities-maven-plugin";
	private static final String GROUP_ID = "org.openeos.tools";

	/**
	 * The project currently being build.
	 * 
	 */
	@Parameter(defaultValue = "${project}", readonly = true, required = true)
	private MavenProject mavenProject;

	/**
	 * The current Maven session.
	 * 
	 */
	@Parameter(defaultValue = "${session}", readonly = true, required = true)
	private MavenSession mavenSession;

	/**
	 * The Maven BuildPluginManager component.
	 * 
	 */
	@Component
	private BuildPluginManager pluginManager;

	/**
	 * The fully qualified name of the driver class to use to connect to the
	 * database.
	 * 
	 */
	@Parameter(defaultValue = "${liquibase.driver}")
	protected String driver;

	/**
	 * The Database URL to connect to for executing Liquibase.
	 * 
	 */
	@Parameter(defaultValue = "${liquibase.url}")
	protected String url;

	/**
	 * The database username to use to connect to the specified database.
	 * 
	 */
	@Parameter(defaultValue = "${liquibase.username}")
	protected String username;

	/**
	 * The database password to use to connect to the specified database.
	 * 
	 */
	@Parameter(defaultValue = "${liquibase.password}")
	protected String password;

	/**
	 * The search path where find liquibase files
	 */
	@Parameter(required = true)
	protected String searchpath;

	/**
	 * The resources to search
	 */
	@Parameter(required = true, defaultValue = "${project.resources}")
	protected List<Resource> resources;

	@Parameter(required = true, defaultValue = "${plugin.dependencies}")
	protected List<Dependency> pluginDependencies;

	@Parameter(required = true)
	protected String jdbcConfigurationConfigurationFile;

	@Parameter(required = true)
	protected String jdbcConfigurationRevengFile;

	@Parameter(required = true)
	protected String jdbcConfigurationReverseStrategy;

	@Parameter(required = true)
	protected String jdbcConfigurationPackageName;

	@Parameter(required = true, defaultValue = "${project.build.directory}/generated-sources/entities")
	protected String generatedSourcesDirectory;

	@Component(hint = "default")
	private DependencyGraphBuilder dependencyGraphBuilder;

	/**
	 * The dependency tree builder to use for verbose output.
	 */
	@Component
	private DependencyTreeBuilder dependencyTreeBuilder;

	private Set<Artifact> visitedArtifacts = new TreeSet<Artifact>();

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		try {
			DependencyNode rootNode = dependencyGraphBuilder.buildDependencyGraph(mavenProject, null);
			recursiveCheckNode(rootNode);
			List<File> files = checkForLocalFiles();
			callLiquibase(files);
			callHbm2Java();
			addSourceDirectory();
		} catch (IOException e) {
			throw new MojoExecutionException("Exception when trying to run liquibase.", e);
		} catch (URISyntaxException e) {
			throw new MojoExecutionException("Exception when trying to run liquibase.", e);
		} catch (DependencyGraphBuilderException e) {
			throw new MojoExecutionException("Exception when trying to run liquibase.", e);
		}
	}

	private void recursiveCheckNode(DependencyNode node) throws IOException, MojoExecutionException {
		if (visitedArtifacts.contains(node.getArtifact())) {
			return;
		}
		// First the childs
		for (DependencyNode child : node.getChildren()) {
			recursiveCheckNode(child);
		}
		getLog().info("Checking artifact: " + node.getArtifact().toString());
		List<File> files = new ArrayList<File>(findLiquibaseFiles(findArtifact(node.getArtifact())));
		callLiquibase(files);
		visitedArtifacts.add(node.getArtifact());
	}

	private Artifact findArtifact(Artifact artifact) {
		for (Artifact artifact2 : mavenProject.getArtifacts()) {
			if (artifact2.getGroupId().equals(artifact.getGroupId()) && artifact2.getArtifactId().equals(artifact.getArtifactId())
					&& artifact2.getVersion().equals(artifact.getVersion()) && artifact2.getType().equals(artifact.getType())) {
				return artifact2;
			}
		}
		return null;
	}

	private void addSourceDirectory() {
		mavenProject.addCompileSourceRoot(generatedSourcesDirectory);
	}

	private List<File> checkForLocalFiles() throws IOException, URISyntaxException {
		List<File> result = new ArrayList<File>();
		for (Resource resource : resources) {
			getLog().info("Checking resource directory: " + resource.getDirectory());
			File file = new File(resource.getDirectory());
			if (file.exists() && file.isDirectory()) {
				result.addAll(checkForLocalFiles(file));
			}
		}
		return result;
	}

	private Set<File> checkForLocalFiles(File file) throws IOException, URISyntaxException {
		final Set<File> result = new TreeSet<File>();
		final PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:" + file.getAbsolutePath() + "/" + searchpath);
		SimpleFileVisitor<Path> fileVisitor = new SimpleFileVisitor<Path>() {

			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
				if (matcher.matches(file)) {
					result.add(file.toFile());
				}
				return FileVisitResult.CONTINUE;
			}

		};
		Files.walkFileTree(file.toPath(), fileVisitor);
		return result;
	}

	private void callHbm2Java() throws MojoExecutionException {
		getLog().info("Calling hbm2java...");
		List<Dependency> dependencies = getPlugin().getDependencies();
		Plugin plugin = plugin("org.codehaus.mojo", "hibernate3-maven-plugin", "3.0.UNO", dependencies);
		Element jdbcConfiguration = element(
				name("jdbcconfiguration"),
				null,
				attributes(attribute(name("configurationfile"), jdbcConfigurationConfigurationFile),
						attribute(name("revengfile"), jdbcConfigurationRevengFile),
						attribute(name("reversestrategy"), jdbcConfigurationReverseStrategy),
						attribute(name("packagename"), jdbcConfigurationPackageName)));
		Element hbm2java = element(name("hbm2java"), null,
				attributes(attribute(name("jdk5"), "true"), attribute(name("ejb3"), "true")));
		Element hibernateTool = element(name("hibernatetool"), attribute(name("destdir"), generatedSourcesDirectory),
				jdbcConfiguration, hbm2java);
		executeMojo(plugin, goal("hbm2java"), configuration(hibernateTool),
				executionEnvironment(mavenProject, mavenSession, pluginManager));
	}

	private void callLiquibase(List<File> files) throws MojoExecutionException {
		for (File file : files) {
			List<Dependency> dependencies = getPlugin().getDependencies();
			getLog().info("Running liquibase with file: " + file.toString());
			executeMojo(
					plugin("org.liquibase", "liquibase-maven-plugin", "2.0.1", dependencies),
					goal("update"),
					configuration(element(name("promptOnNonLocalDatabase"), "false"), element(name("driver"), driver),
							element(name("url"), url), element(name("username"), username), element(name("password"), password),
							element(name("changeLogFile"), file.getAbsolutePath())),
					executionEnvironment(mavenProject, mavenSession, pluginManager));
		}
	}

	private Plugin getPlugin() {
		for (Plugin plugin : mavenProject.getBuild().getPlugins()) {
			if (plugin.getArtifactId().equals(ARTIFACT_ID) && plugin.getGroupId().equals(GROUP_ID)) {
				return plugin;
			}
		}
		throw new RuntimeException("Plugin configuration of generate entities not found in project");
	}

	private List<File> findLiquibaseFiles(Artifact artifact) throws IOException {

		if (artifact == null) {
			return Collections.emptyList();
		}
		List<File> result = new ArrayList<File>();

		if (artifact.getType().equals("jar")) {
			File file = artifact.getFile();
			FileSystem fs = FileSystems.newFileSystem(Paths.get(file.getAbsolutePath()), this.getClass().getClassLoader());
			PathMatcher matcher = fs.getPathMatcher("glob:" + searchpath);
			JarFile jarFile = new JarFile(file);
			Enumeration<JarEntry> entries = jarFile.entries();
			TreeSet<JarEntry> setEntries = new TreeSet<JarEntry>(new Comparator<JarEntry>() {

				@Override
				public int compare(JarEntry o1, JarEntry o2) {
					return o1.getName().compareTo(o2.getName());
				}

			});
			while (entries.hasMoreElements()) {
				JarEntry entry = entries.nextElement();
				if (matcher.matches(fs.getPath(entry.getName()))) {
					setEntries.add(entry);
				}
			}
			for (JarEntry entry : setEntries) {
				File resultFile = File.createTempFile("generate-entities-maven", ".xml");
				FileOutputStream out = new FileOutputStream(resultFile);
				InputStream in = jarFile.getInputStream(entry);
				IOUtils.copy(in, out);
				in.close();
				out.close();
				result.add(resultFile);
			}
			jarFile.close();
		}
		return result;
	}

}
