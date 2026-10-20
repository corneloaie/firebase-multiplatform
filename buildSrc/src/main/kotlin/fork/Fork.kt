package fork

import org.gradle.api.Project
import org.gradle.api.provider.Property
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.withGroovyBuilder
import org.gradle.kotlin.dsl.withType

/**
 * What makes this build corneloaie/firebase-multiplatform rather than GitLive's firebase-kotlin-sdk.
 *
 * The module build files keep GitLive's publishing setup as it is upstream, so the weekly upstream merges stay free of
 * conflicts there, and [applyFork] overrides it for every module from the root build.
 *
 * `project.group` stays `dev.gitlive`: the Kotlin SwiftPM import names each module's cinterop package after the project
 * group and name (`swiftPMImport.dev.gitlive.firebase.<module>`), and every Apple source imports from it. Only the
 * published Maven coordinates change.
 */
object Fork {
    const val GROUP_ID = "io.github.corneloaie"
    const val NAME = "firebase-multiplatform"
    const val URL = "https://github.com/corneloaie/firebase-multiplatform"
    const val DESCRIPTION = "Kotlin Multiplatform Firebase SDK: the Firebase Android SDK API (com.google.firebase) and a " +
        "Kotlin-first API (dev.gitlive.firebase) from common code on Android, iOS, macOS, tvOS, the JVM, JS and Wasm. " +
        "Based on GitLive's firebase-kotlin-sdk."
}

/**
 * Applies the fork's version (the `forkVersion` Gradle property) and Maven coordinates to a module. Called for every
 * subproject from the root build, before the module's own build file runs.
 */
fun Project.applyFork() {
    providers.gradleProperty("forkVersion").orNull?.let { version = it }
    plugins.withId("maven-publish") {
        // After the module's own mavenPublishing block, whose coordinates() and pom() run first.
        afterEvaluate {
            // The group vanniktech stages and uploads to Maven Central under: its mavenPublishing groupId, which the
            // module's coordinates() call fixed to dev.gitlive (and finalised), so the upload task's copy is replaced.
            // By name and reflectively, as buildSrc does not compile against the plugin, which modules apply by version.
            tasks.matching { it.name == "prepareMavenCentralPublishing" }.configureEach {
                @Suppress("UNCHECKED_CAST")
                (withGroovyBuilder { "getProjectGroup"() } as Property<String>).set(Fork.GROUP_ID)
            }
            extensions.configure<PublishingExtension> {
                publications.withType<MavenPublication>().configureEach {
                    groupId = Fork.GROUP_ID
                    pom {
                        name.set(Fork.NAME)
                        description.set(Fork.DESCRIPTION)
                        url.set(Fork.URL)
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
                                id.set("corneloaie")
                                name.set("Cornel Oaie")
                                url.set("https://github.com/corneloaie")
                            }
                        }
                    }
                }
            }
        }
    }
}
