import io.papermc.hangarpublishplugin.model.Platforms
import java.io.ByteArrayOutputStream
import xyz.jpenilla.runpaper.task.RunServer

plugins {
    id("com.gradleup.shadow") version "9.3.1"
    id("java-library")
    id("xyz.jpenilla.run-paper") version "3.0.2"
    id("io.papermc.hangar-publish-plugin") version "0.1.2"
}

group = "eu.jdanek.unwaxsigns"
version = "1.1.0"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    // Main repository for downloading VaultAPI from GitHub
    maven("https://jitpack.io")
}

dependencies {
    implementation("org.bstats:bstats-bukkit:3.2.1")

    compileOnly("io.papermc.paper:paper-api:26.1.2.build.+")
    compileOnly("com.github.MilkBowl:VaultAPI:1.7.1")
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(25)
}

tasks {
    runServer {
        // Configure the Minecraft version for our task.
        // This is the only required configuration besides applying the plugin.
        // Your plugin's jar (or shadowJar if present) will be used automatically.
        minecraftVersion("26.3")
        jvmArgs("-Xms2G", "-Xmx2G")
    }

//    register<RunServer>("runServerExperimental") {
//        description = "Experimental"
//        group = "paper"
//        pluginJars.from(named("shadowJar"))
//        minecraftVersion("26.2")
//        jvmArgs("-Xms2G", "-Xmx2G")
//        runDirectory.set(file("run/_experimental"))
//    }

    processResources {
        val props = mapOf("version" to version)
        filesMatching("plugin.yml") {
            expand(props)
        }
    }
}

tasks.jar {
    enabled = false
}

tasks.shadowJar {
    archiveClassifier.set("") // remove "-all" suffix
    configurations = project.configurations.runtimeClasspath.map { setOf(it) }

    dependencies {
        // Only merge bStats into the final jar, no other dependencies
        exclude { it.moduleGroup != "org.bstats" }
    }

    // Relocate bStats into the plugin's package to avoid conflicts with other
    // plugins using bStats
    relocate("org.bstats", project.group.toString())
}

// Helper methods
fun executeGitCommand(vararg command: String): String {
    val result = providers.exec {
        commandLine = listOf("git") + command
    }.standardOutput.asText.get()
    return result.trim()
}

fun latestCommitMessage(): String {
    return executeGitCommand("log", "-1", "--pretty=%B")
}

val versionString: String = version as String
val isRelease: Boolean = !versionString.contains('-')

val suffixedVersion: String = if (isRelease) {
    versionString
} else {
    // Give the version a unique name by using the GitHub Actions run number
    versionString + "+" + System.getenv("GITHUB_RUN_NUMBER")
}

// Use the commit description for the changelog
val changelogContent: String = latestCommitMessage()
val pageContent = project.file("README.md").readText()

hangarPublish {
    publications.register("plugin") {
        version.set(suffixedVersion)
        channel.set(if (isRelease) "Release" else "Snapshot")
        changelog.set(changelogContent)
        pages.resourcePage(pageContent)
        id.set("UnwaxSigns")
        //apiKey.set(System.getenv("HANGAR_API_TOKEN"))
        apiKey.set(
            (findProperty("hangarApiToken") as? String)
                ?: System.getenv("HANGAR_API_TOKEN")
                ?: ""
        )
        platforms {
            // TODO: Use the correct platform(s) for your plugin
            register(Platforms.PAPER) {
                // TODO: If you're using ShadowJar, replace the jar lines with the appropriate task:
                   jar.set(tasks.shadowJar.flatMap { it.archiveFile })
                // Set the JAR file to upload
                // jar.set(tasks.jar.flatMap { it.archiveFile })

                // Set platform versions from gradle.properties file
                val versions: List<String> = (property("paperVersion") as String)
                    .split(",")
                    .map { it.trim() }
                platformVersions.set(versions)

                dependencies {
                    // Example for a dependency found on Hangar
                    hangar("Maintenance") {
                        required.set(false)
                    }
                }
            }
        }
    }
}