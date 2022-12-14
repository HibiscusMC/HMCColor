plugins {
    java
    id("com.github.johnrengelman.shadow") version "7.1.2"
    id ("org.jetbrains.kotlin.jvm") version "1.7.0"
}

group = "com.hibiscusmc"
version = "0.3-SNAPSHOT"
description = "Write something here idk.\n"


repositories {
    mavenCentral()
    gradlePluginPortal()
    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
    maven("https://oss.sonatype.org/content/groups/public/")
    maven("https://repo.mineinabyss.com/releases")
    maven("https://mvn.lumine.io/repository/maven-public/")
    maven("https://jitpack.io")
}

dependencies {
    compileOnly("org.spigotmc:spigot-api:1.19.3-R0.1-SNAPSHOT")
    compileOnly("com.github.oraxen:oraxen:-SNAPSHOT")
    compileOnly("com.github.LoneDev6:api-itemsadder:3.2.5")
    compileOnly("io.lumine:Mythic-Dist:5.2.0-SNAPSHOT")
    compileOnly("io.lumine:MythicCrucible:1.6.0-SNAPSHOT")
    compileOnly("com.mineinabyss:idofront:0.12.111")
    compileOnly("com.mineinabyss:geary-papermc-core:0.19.113")
    compileOnly("com.mineinabyss:looty:0.8.67")

    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("net.kyori:adventure-api:4.12.0")
    implementation("net.kyori:adventure-text-minimessage:4.12.0")
    implementation("dev.triumphteam:triumph-gui:3.1.4")
    implementation("me.mattstudios.utils:matt-framework:1.4.6")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(17))
}

val copyJar = project.findProperty("copyJar")
val pluginPath = project.findProperty("hibiscusmc_plugin_path")
tasks {
    shadowJar {
        relocate("dev.triumphteam.gui", "com.hibiscusmc.hmccolor.gui")
        relocate("me.mattstudios.mf", "com.hibiscusmc.hmccolor.mf")
        relocate("net.kyori.adventure", "com.hibiscusmc.hmccolor.adventure")
        relocate("org.spongepowered.configurate", "com.hibiscusmc.hmccolor.configurate")
        relocate("org.bstats", "com.hibiscusmc.hmccolor.bstats")
        archiveFileName.set("HMCColor.jar")
    }

    if(copyJar != "false" && pluginPath != null) {
        register<Copy>("copyJar") {
            from(findByName("reobfJar") ?: findByName("shadowJar") ?: findByName("jar"))
            into(pluginPath)
            doLast {
                println("Copied to plugin directory $pluginPath")
            }
        }

        build {
            dependsOn("copyJar")
        }
    } else {
        build {
            dependsOn("shadowJar")
        }
    }
}
