package org.github.rzymek.maven

import java.io.File
import java.nio.file.FileSystems
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardWatchEventKinds
import java.nio.file.WatchEvent
import java.util.List
import java.util.Map
import org.apache.maven.Maven
import org.apache.maven.execution.DefaultMavenExecutionRequest
import org.apache.maven.execution.MavenSession
import org.apache.maven.model.Plugin
import org.apache.maven.plugin.AbstractMojo
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
	@Parameter(defaultValue='${session}', required=true, readonly=true)
	MavenSession session
	@Parameter(required=true)
	List<Watch> watch
	@Component
	PluginPrefixResolver pluginPrefixResolver
	@Component
	PluginVersionResolver pluginVersionResolver;
	@Component 
	Maven maven

	override execute() throws MojoExecutionException, MojoFailureException {
		for(w : watch) {
			register(w.on)			
		}
		val Map<String, List<String>> watchMap = newHashMap()
		for(w : watch) {
			val goal = w.run.split(' ').map[trim]
			watchMap.put(w.on.absolutePath, goal)
		}
		log.info("Waiting: "+watch.map[on]);
		watchLoop[
				val goals = watchMap.get(it.absolutePath)
				log.debug(it + " -> " + goals)
				if(goals != null) {
					log.info(it +" changed -> ["+goals.join(' ')+"]");
					val request=DefaultMavenExecutionRequest.copy(session.request);
					request.setGoals(goals);				
					maven.execute(request)
				}  
			]
	}

	def resolve(String prefix) {
		var pluginResult = pluginPrefixResolver.resolve(new DefaultPluginPrefixRequest(prefix, session))
		val plugin = new Plugin()
		plugin.groupId = pluginResult.groupId
		plugin.artifactId = pluginResult.artifactId
		
		var versionRequest = new DefaultPluginVersionRequest(
			plugin,
			session) 
		plugin.version = pluginVersionResolver.resolve(versionRequest).getVersion();
		return plugin
	}
	
	val watchService = FileSystems.^default.newWatchService
	def watch(File file, (File)=>void run) {
		register(file)
		watchLoop(run)		
	}

	def watchLoop((File)=>void run) {
		var valid = true;
		while(valid) {
			log.info("Waiting for modifications...");
			val key = watchService.take
			key.pollEvents
				.map[log.debug("WatchService event: "+it.kind+":"+it.context); it]
				.map[it as WatchEvent<Path>]
				.filter[kind != StandardWatchEventKinds.OVERFLOW]
				.map[(key.watchable as Path).resolve(it.context)]				
				.map[toFile]
				.forEach[run.apply(it)]
			valid = key.reset
		} 
	}

	def register(File file) {
		val path = Paths.get(file.absoluteFile.parent)
		log.debug("register: "+path + "\t"+file+"\n"+path.absolute)
		path.register(watchService, StandardWatchEventKinds.ENTRY_MODIFY)
	}
}
