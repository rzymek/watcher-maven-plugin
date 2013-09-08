package org.github.rzymek.maven

import java.io.File
import java.nio.file.FileSystems
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardWatchEventKinds
import java.nio.file.WatchEvent
import java.nio.file.WatchKey
import java.util.List
import java.util.Map
import org.apache.maven.execution.MavenSession
import org.apache.maven.model.Plugin
import org.apache.maven.plugin.AbstractMojo
import org.apache.maven.plugin.BuildPluginManager
import org.apache.maven.plugin.MojoExecutionException
import org.apache.maven.plugin.MojoFailureException
import org.apache.maven.plugin.prefix.DefaultPluginPrefixRequest
import org.apache.maven.plugin.prefix.PluginPrefixResolver
import org.apache.maven.plugin.version.DefaultPluginVersionRequest
import org.apache.maven.plugin.version.PluginVersionResolver
import org.apache.maven.plugins.annotations.Component
import org.apache.maven.plugins.annotations.Mojo
import org.apache.maven.plugins.annotations.Parameter
import org.apache.maven.plugins.annotations.ResolutionScope
import org.apache.maven.project.MavenProject

import static org.twdata.maven.mojoexecutor.MojoExecutor.*

class Watch {
	public File on
	public String run	
}

@Data 
class PluginGoal {
	Plugin plugin;
	String goal;	
}

@Mojo(name='run', requiresDependencyResolution=ResolutionScope::COMPILE_PLUS_RUNTIME)
class Watcher extends AbstractMojo {
	@Parameter(defaultValue='${project}', required=true, readonly=true)
	MavenProject project
	@Parameter(defaultValue='${session}', required=true, readonly=true)
	MavenSession session
	@Component
	BuildPluginManager buildPluginManager
	@Parameter(required=true)
	List<Watch> watch
	@Component
	PluginPrefixResolver pluginPrefixResolver
	@Component
	PluginVersionResolver pluginVersionResolver;

	override execute() throws MojoExecutionException, MojoFailureException {
		for(w : watch) {
			register(w.on)			
		}
		val Map<String, List<PluginGoal>> watchMap = newHashMap()
		for(w : watch) {
			val goal = w.run.split(' ').map[split(':').map[trim]]
				.map[get(0) -> get(1)]
				.map[new PluginGoal(resolve(key), value)]
			watchMap.put(w.on.absolutePath, goal)
		}
		log.info("Waiting: "+watch.map[on]);
		watchLoop[			
			watchMap.get(it.absolutePath)?.forEach [
				executeMojo(it.plugin, it.goal, configuration(),
					executionEnvironment(
						project,
						session,
						buildPluginManager
					))
			]]
	}

	def resolve(String prefix) {
		var pluginResult = pluginPrefixResolver.resolve(new DefaultPluginPrefixRequest(prefix, session))
		val plugin = new Plugin()
		plugin.groupId = pluginResult.groupId
		plugin.artifactId = pluginResult.artifactId
		
		var versionRequest = new DefaultPluginVersionRequest(
			plugin,
			session.getRepositorySession(), 
			project.getRemotePluginRepositories());
		plugin.version = pluginVersionResolver.resolve(versionRequest).getVersion();
		return plugin
	}
	
	val watchService = FileSystems.^default.newWatchService
	def watch(File file, (File)=>void run) {
		register(file)
		watchLoop(run)		
	}

	def watchLoop((File)=>void run) {
		var WatchKey key;
		do {
			log.info("Waiting for modifications...");
			key = watchService.take
			key.pollEvents
				.filter[kind != StandardWatchEventKinds.OVERFLOW]
				.map[it as WatchEvent<Path>]
				.map[context.toFile]
				.forEach[run.apply(it)]
		} while (key.reset)
	}

	def register(File file) {
		val path = Paths.get(file.absoluteFile.parent)
		path.register(watchService, StandardWatchEventKinds.ENTRY_MODIFY)
	}
}
