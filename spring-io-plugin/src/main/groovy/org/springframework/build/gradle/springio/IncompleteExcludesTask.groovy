package org.springframework.build.gradle.springio

import org.gradle.api.DefaultTask
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ExternalModuleDependency
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

/**
 *
 * @author Rob Winch
 * @author Andy Wilkinson
 */
class IncompleteExcludesTask extends DefaultTask {

	Collection<Configuration> configurations

	File reportFile = project.file("$project.buildDir/spring-io/incomplete-excludes.log")

	@TaskAction
	void check() {
		reportFile.parentFile.mkdirs()

		if (!configurations) {
			configurations = project.configurations.findAll { !it.name.toLowerCase().contains('test') }
		}

		def problemsByConfiguration = [:]
		configurations.each { configuration ->
			def problemsByDependency = [:]
			configuration.dependencies.each { dependency ->
				if (dependency instanceof ExternalModuleDependency) {
					def problems = []
					dependency.excludeRules.each { excludeRule ->
						if (!excludeRule.group) {
							problems << "Exclude for module ${excludeRule.module} does not specify a group. The exclusion will not be included in generated POMs"
						} else if (!excludeRule.module) {
							problems << "Exclude for group ${excludeRule.group} does not specify a module. The exclusion will not be included in generated POMs"
						}
					}
					if (problems) {
						problemsByDependency[dependency] = problems
					}
				}
			}
			if (problemsByDependency) {
				problemsByConfiguration[configuration.name] = problemsByDependency
			}
		}

		if (problemsByConfiguration) {
			reportFile.withWriterAppend { out ->
				out.writeLine(project.name)
				problemsByConfiguration.each { configuration, problemsByDependency ->
					out.writeLine("    Configuration: ${configuration}")
					problemsByDependency.each { dependency, problems ->
						out.writeLine("        ${dependency.group}:${dependency.name}:${dependency.version}")
						problems.each { out.writeLine("            ${it}")}
					}
				}
			}
			throw new IllegalStateException("Found incomplete dependency exclusions. See $reportFile for a detailed report")
		}
	}
}