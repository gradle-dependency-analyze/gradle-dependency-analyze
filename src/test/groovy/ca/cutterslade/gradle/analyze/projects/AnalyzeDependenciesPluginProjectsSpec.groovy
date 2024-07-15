package ca.cutterslade.gradle.analyze.projects

import ca.cutterslade.gradle.analyze.AnalyzeDependenciesPluginBaseSpec
import org.apache.commons.io.FileUtils

class AnalyzeDependenciesPluginProjectsSpec extends AnalyzeDependenciesPluginBaseSpec {
    def 'issue_527'() {
        setup:
        copyProjectToTestFolder('projects/issue_527', projectDir)
        when:
        def result = buildGradleProject(SUCCESS)

        then:
        assertBuildResult(result, SUCCESS)
    }

    void copyProjectToTestFolder(String sourcePath, File destFolder) {
        URL resourceUrl = this.class.getResource("/$sourcePath")
        if (resourceUrl == null) {
            throw new IllegalStateException("Resource folder '$sourcePath' not found in classpath")
        }
        File sourceFolder = new File(resourceUrl.toURI())

        if (!sourceFolder.exists()) {
            throw new IllegalStateException("Source folder does not exist at $sourceFolder")
        }

        // Make sure destination is a directory and not a file
        if (!destFolder.exists()) {
            destFolder.mkdirs()
        } else if (!destFolder.isDirectory()) {
            throw new IllegalArgumentException("Destination must be a directory")
        }

        FileUtils.copyDirectory(sourceFolder, destFolder)
    }
}
