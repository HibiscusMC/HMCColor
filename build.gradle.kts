plugins {
    id("io.github.goooler.shadow") version "8.1.7"
    id ("org.jetbrains.kotlin.jvm") version "2.0.0"
    alias(idofrontLibs.plugins.kotlinx.serialization)
    alias(idofrontLibs.plugins.mia.copyjar)
}

val pluginVersion: String by project
group = "com.hibiscusmc"
version = pluginVersion

repositories {
    mavenCentral()
    gradlePluginPortal()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://mvn.lumine.io/repository/maven-public/") { metadataSources { artifact() } }// MythicMobs
    maven("https://jitpack.io")
    maven("https://repo.oraxen.com/releases")
    maven("https://repo.mineinabyss.com/releases")
    maven("https://repo.mineinabyss.com/snapshots")
    mavenLocal()
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21-R0.1-SNAPSHOT")
    compileOnly("io.th0rgal:oraxen:1.180.0")
    compileOnly("com.github.LoneDev6:api-itemsadder:3.4.1e")
    compileOnly("io.lumine:Mythic-Dist:5.7.1")
    compileOnly("io.lumine:MythicCrucible:2.0.0")
    compileOnly("com.mineinabyss:geary-papermc:0.30.12")

    implementation("dev.triumphteam:triumph-gui:3.1.10") { exclude("net.kyori") }
    implementation(idofrontLibs.kotlin.stdlib)
    implementation(idofrontLibs.kotlinx.serialization.json)
    implementation(idofrontLibs.kotlinx.serialization.kaml)

    implementation(idofrontLibs.idofront.di)
    implementation(idofrontLibs.idofront.commands)
    implementation(idofrontLibs.idofront.config)
    implementation(idofrontLibs.idofront.text.components)
    implementation(idofrontLibs.idofront.logging)
    implementation(idofrontLibs.idofront.serializers)
    implementation(idofrontLibs.idofront.util)
}

kotlin {
    jvmToolchain(21)
}

val buildPath = project.findProperty("oraxen_plugin_path") as? String?
copyJar {
    this.destPath.set(buildPath ?: project.path)
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
