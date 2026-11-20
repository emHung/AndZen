// Top-level build file
plugins {
    alias(libs.plugins.android.application) apply false
    id("org.sonarqube") version "7.6.0.18749"
}

sonar {
    properties {
        property("sonar.projectKey", providers.environmentVariable("SONAR_PROJECT_KEY").getOrElse("AndZen"))
        property("sonar.projectName", "AndZen")
        property("sonar.sources", "backend/app")
        property("sonar.tests", "backend/tests")
        property("sonar.python.version", "3.12")
        property("sonar.python.coverage.reportPaths", "backend/coverage.xml")
        property("sonar.sourceEncoding", "UTF-8")
        property("sonar.exclusions", "**/build/**,**/.venv/**,**/graft/**")
    }
}
