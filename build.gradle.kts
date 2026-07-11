import xyz.jpenilla.runpaper.task.RunServer

plugins {
    id("com.gradleup.shadow") version "9.3.1"
    id("java-library")
    id("xyz.jpenilla.run-paper") version "3.0.2"
}

group = "eu.jdanek.unwaxsigns"
version = "1.0.0"

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
        minecraftVersion("26.1.2")
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

tasks.shadowJar {
    configurations = project.configurations.runtimeClasspath.map { setOf(it) }

    dependencies {
        // Only merge bStats into the final jar, no other dependencies
        exclude { it.moduleGroup != "org.bstats" }
    }

    // Relocate bStats into the plugin's package to avoid conflicts with other
    // plugins using bStats
    relocate("org.bstats", project.group.toString())
}