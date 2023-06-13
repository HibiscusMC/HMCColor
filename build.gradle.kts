plugins {
    java
    id("com.github.johnrengelman.shadow") version "8.1.1"
    id ("org.jetbrains.kotlin.jvm") version "1.8.22"
    alias(libs.plugins.kotlinx.serialization)
    alias(libs.plugins.mia.kotlin.jvm)
}

val pluginVersion: String by project
group = "com.hibiscusmc"
version = pluginVersion

repositories {
    mavenCentral()
    gradlePluginPortal()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.mineinabyss.com/releases")
    maven("https://mvn.lumine.io/repository/maven-public/")
    maven("https://jitpack.io")
    maven("https://repo.mineinabyss.com/releases")
    maven("https://repo.mineinabyss.com/snapshots")
    mavenLocal()
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.20-R0.1-SNAPSHOT")
    compileOnly("com.github.oraxen:oraxen:1.157.0")
    compileOnly("com.github.LoneDev6:api-itemsadder:3.4.1-r4")
    compileOnly("io.lumine:Mythic-Dist:5.2.0-SNAPSHOT")
    compileOnly("io.lumine:MythicCrucible:1.6.0-SNAPSHOT")
    compileOnly("com.mineinabyss:geary-papermc:0.24-SNAPSHOT")

    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("net.kyori:adventure-api:4.14.0")
    implementation("net.kyori:adventure-text-minimessage:4.14.0")
    implementation("net.kyori:adventure-text-serializer-plain:4.14.0")
    implementation("net.kyori:adventure-platform-bukkit:4.3.0")
    implementation("dev.triumphteam:triumph-gui:3.1.5")
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.serialization.kaml)
    implementation(libs.bundles.idofront.core)
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
        relocate("dev.triumphteam.gui", "com.hibiscusmc.hmccolor.shaded.gui")
        relocate("net.kyori:adventure-api", "com.hibiscusmc.hmccolor.shaded.kyori:adventure-api")
        relocate("org.spongepowered.configurate", "com.hibiscusmc.hmccolor.shaded.configurate")
        relocate("org.bstats", "com.hibiscusmc.hmccolor.shaded.bstats")
        relocate("kotlin", "com.hibiscusmc.hmccolor.shaded.kotlin")
        relocate("org.jetbrains", "com.hibiscusmc.hmccolor.shaded.jetbrains")
        relocate("com.mineinabyss.idofront", "com.hibiscusmc.hmccolor.shaded.idofront")
        archiveFileName.set("HMCColor-${pluginVersion}.jar")
    }

    if(copyJar != "false" && pluginPath != null) {
        register<Copy>("copyJar") {
            this.doNotTrackState("Overwrites the plugin jar to allow for easier reloading")
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
