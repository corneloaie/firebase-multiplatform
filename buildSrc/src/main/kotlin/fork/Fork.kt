package fork

import org.gradle.api.Project
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.withType

/**
 * The project's Maven group, name and POM.
 *
 * `project.group` is [GROUP_ID]. Besides the Maven coordinates it names each module's SwiftPM cinterop package after
 * the group and the module (`swiftPMImport.io.github.corneloaie.firebase.<module>`), which every Apple source imports.
 */
object Fork {
    const val GROUP_ID = "io.github.corneloaie"
    const val NAME = "firebase-multiplatform"
    const val URL = "https://github.com/corneloaie/firebase-multiplatform"
    const val DEVELOPER_ID = "corneloaie"
    const val DESCRIPTION = "Kotlin Multiplatform Firebase SDK: the Firebase Android SDK API (com.google.firebase) and a " +
        "Kotlin-first API (io.github.corneloaie.firebase) from common code on Android, iOS, macOS, tvOS, the JVM, JS " +
        "and Wasm. Based on GitLive's firebase-kotlin-sdk."
}

/**
 * Applies the project's group, version (the `forkVersion` Gradle property) and POM to a module. Called for every
 * subproject from the root build, before the module's own build file runs.
 */
fun Project.applyFork() {
    group = Fork.GROUP_ID
    providers.gradleProperty("forkVersion").orNull?.let { version = it }
    plugins.withId("maven-publish") {
        extensions.configure<PublishingExtension> {
            publications.withType<MavenPublication>().configureEach {
                pom {
                    name.set(Fork.NAME)
                    description.set(Fork.DESCRIPTION)
                    url.set(Fork.URL)
                    inceptionYear.set("2019")
                    scm {
                        url.set(Fork.URL)
                        connection.set("scm:git:${Fork.URL}.git")
                        developerConnection.set("scm:git:ssh://git@github.com/corneloaie/firebase-multiplatform.git")
                        tag.set("HEAD")
                    }
                    issueManagement {
                        system.set("GitHub Issues")
                        url.set("${Fork.URL}/issues")
                    }
                    developers {
                        developer {
                            id.set(Fork.DEVELOPER_ID)
                            name.set("Cornel Oaie")
                            url.set("https://github.com/corneloaie")
                        }
                    }
                    licenses {
                        license {
                            name.set("The Apache Software License, Version 2.0")
                            url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                            distribution.set("repo")
                            comments.set("A business-friendly OSS license")
                        }
                    }
                }
            }
        }
    }
}
