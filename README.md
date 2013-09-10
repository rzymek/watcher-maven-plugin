watcher-maven-plugin
====================

Watches files for modifications. On such event executes specified maven goals.

Use case: regenerating exploaded WAR or Eclipse project when dependencies change

Usage:

**1.** Add the plugin repository to your pom:

	<project>
		<pluginRepositories>
			<pluginRepository>
				<id>rzymek-snapshots</id>
				<url>https://github.com/rzymek/repository/raw/master/snapshots</url>		
			</pluginRepository>
		</pluginRepositories>
		...

**2.** Configure the plugin

	<project>
		<build>
			<plugins>
				<plugin> 
					<groupId>watcher</groupId>
					<artifactId>watcher-maven-plugin</artifactId>
					<version>1.0-SNAPSHOT</version>
					<configuration>
						<watch>
							<param>
								<on>pom.xml</on>
								<run>clean war:inplace eclipse:eclipse</run>
							</param>
						</watch>
					</configuration>
				</plugin>
			....
			
**3.** Start watching for changes:

    mvn watcher:run
    
