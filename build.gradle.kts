plugins {
    java
    id("com.github.johnrengelman.shadow") version "7.1.2"
    id ("org.jetbrains.kotlin.jvm") version "1.7.20"
}

val pluginVersion: String by project
group = "com.hibiscusmc"
version = pluginVersion

repositories {
    mavenCentral()
    gradlePluginPortal()
    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
    maven("https://oss.sonatype.org/content/groups/public/")
    maven("https://repo.mineinabyss.com/releases")
    maven("https://mvn.lumine.io/repository/maven-public/")
    maven("https://jitpack.io")
    maven("https://repo.mineinabyss.com/releases")
}

dependencies {
    compileOnly("org.spigotmc:spigot-api:1.19.4-R0.1-SNAPSHOT")
    compileOnly("com.github.oraxen:oraxen:1.155.3")
    compileOnly("com.github.LoneDev6:api-itemsadder:3.4.1-r4")
    compileOnly("io.lumine:Mythic-Dist:5.2.0-SNAPSHOT")
    compileOnly("io.lumine:MythicCrucible:1.6.0-SNAPSHOT")
    compileOnly("com.mineinabyss:idofront:0.12.111")
    compileOnly("com.mineinabyss:geary-papermc-core:0.19.113")
    compileOnly("com.mineinabyss:looty:0.8.67")

    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("net.kyori:adventure-api:4.13.0")
    implementation("net.kyori:adventure-text-minimessage:4.13.0")
    implementation("net.kyori:adventure-platform-bukkit:4.3.0")
    implementation("dev.triumphteam:triumph-gui:3.1.4")
    implementation("me.mattstudios.utils:matt-framework:1.4.6")
    implementation("com.mineinabyss:idofront-commands:0.16.10")
    implementation("com.mineinabyss:idofront-config:0.16.10")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(17))
}

val copyJar = project.findProperty("copyJar")
val pluginPath = project.findProperty("hibiscusmc_plugin_path")
tasks {

    shadowJar {
        filesMatching(arrayOf("plugin.yml").asIterable()) {
            expand(mapOf("version" to pluginVersion))
        }
        relocate("dev.triumphteam.gui", "com.hibiscusmc.hmccolor.gui")
        relocate("me.mattstudios.mf", "com.hibiscusmc.hmccolor.mf")
        relocate("net.kyori", "com.hibiscusmc.hmccolor.kyori")
        relocate("org.spongepowered.configurate", "com.hibiscusmc.hmccolor.configurate")
        relocate("org.bstats", "com.hibiscusmc.hmccolor.bstats")
        relocate("kotlin", "com.hibiscusmc.hmccolor.kotlin")
        relocate("com.mineinabyss.idofront", "com.hibiscusmc.hmccolor.idofront")
        archiveFileName.set("HMCColor-${pluginVersion}.jar")
    }

    if(copyJar != "false" && pluginPath != null) {
        register<Copy>("copyJar") {
            dependsOn(jar, shadowJar)
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
