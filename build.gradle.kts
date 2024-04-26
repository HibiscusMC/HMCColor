import io.papermc.paperweight.util.path
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.util.libsDirectory
import kotlin.io.path.absolutePathString

plugins {
    id("com.github.johnrengelman.shadow") version "8.1.1"
    id ("org.jetbrains.kotlin.jvm") version "1.9.20"
    alias(libs.plugins.kotlinx.serialization)
    alias(libs.plugins.mia.copyjar)
}

val pluginVersion: String by project
group = "com.hibiscusmc"
version = pluginVersion

repositories {
    mavenCentral()
    gradlePluginPortal()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://mvn.lumine.io/repository/maven-public/")
    maven("https://jitpack.io")
    maven("https://repo.oraxen.com/releases")
    maven("https://repo.mineinabyss.com/releases")
    maven("https://repo.mineinabyss.com/snapshots")
    mavenLocal()
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.20.4-R0.1-SNAPSHOT")
    compileOnly("io.th0rgal:oraxen:1.167.0")
    compileOnly("com.github.LoneDev6:api-itemsadder:3.4.1e")
    compileOnly("io.lumine:Mythic-Dist:5.2.0-SNAPSHOT")
    compileOnly("io.lumine:MythicCrucible:1.6.0-SNAPSHOT")
    compileOnly("com.mineinabyss:geary-papermc:0.29.11")

    implementation("dev.triumphteam:triumph-gui:3.1.7") { exclude("net.kyori") }
    implementation(libs.kotlin.stdlib)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.serialization.kaml)

    implementation(libs.idofront.di)
    implementation(libs.idofront.commands)
    implementation(libs.idofront.config)
    implementation(libs.idofront.text.components)
    implementation(libs.idofront.logging)
    implementation(libs.idofront.serializers)
    implementation(libs.idofront.util)
}

kotlin {
    jvmToolchain(17)
}

val buildPath = project.findProperty("oraxen_plugin_path") as? String?
copyJar {
    this.destPath.set(buildPath ?: project.libsDirectory.path.absolutePathString())
    this.jarName.set("HMCColor-${pluginVersion}.jar")
    this.excludePlatformDependencies.set(false)
}

tasks {

    shadowJar {
        filesMatching(arrayOf("plugin.yml").asIterable()) {
            expand(mapOf("version" to pluginVersion))
        }
        relocate("dev.triumphteam.gui", "com.hibiscusmc.hmccolor.shaded.gui")
        relocate("org.spongepowered.configurate", "com.hibiscusmc.hmccolor.shaded.configurate")
        relocate("org.bstats", "com.hibiscusmc.hmccolor.shaded.bstats")
        relocate("kotlin", "com.hibiscusmc.hmccolor.shaded.kotlin")
        relocate("org.jetbrains", "com.hibiscusmc.hmccolor.shaded.jetbrains")
        relocate("com.mineinabyss.idofront", "com.hibiscusmc.hmccolor.shaded.idofront")
    }
}
