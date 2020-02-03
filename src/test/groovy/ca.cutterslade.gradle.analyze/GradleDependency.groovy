package ca.cutterslade.gradle.analyze

class GradleDependency {
    String configuration
    String id
    String reference
    String project

    private def getRightPart() {
        if (id != null) {
            return "'${id}'"
        }
        if (reference != null) {
            return reference
        }
        if (project != null) {
            return "project(':${project}')"
        }
        return ""
    }

    def get() {
        return "${configuration}(${getRightPart()})"
    }
}
