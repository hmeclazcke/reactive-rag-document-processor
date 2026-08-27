pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)

    repositories {
        mavenCentral()
    }
}

rootProject.name = "reactive-rag-document-processor"

include("file-generator")
include("file-coordinator")
include("file-processor")
include("file-query-api")
include("rag-indexer")
