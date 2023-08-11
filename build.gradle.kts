plugins {
    id("com.github.johnrengelman.shadow") version "8.1.1"
    id ("org.jetbrains.kotlin.jvm") version "1.9.0"
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
    maven("https://repo.mineinabyss.com/releases")
    maven("https://mvn.lumine.io/repository/maven-public/")
    maven("https://jitpack.io")
    maven("https://repo.mineinabyss.com/releases")
    maven("https://repo.mineinabyss.com/snapshots")
    mavenLocal()
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.20.1-R0.1-SNAPSHOT")
    compileOnly("com.github.oraxen:oraxen:1.159.0")
    compileOnly("com.github.LoneDev6:api-itemsadder:3.4.1e")
    compileOnly("io.lumine:Mythic-Dist:5.2.0-SNAPSHOT")
    compileOnly("io.lumine:MythicCrucible:1.6.0-SNAPSHOT")
    compileOnly("com.mineinabyss:geary-papermc:0.24.1")

    implementation("dev.triumphteam:triumph-gui:3.1.5")
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

copyJar {
    destPath.set(project.findProperty("hibiscusmc_plugin_path") as String)
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
    build.get().dependsOn(shadowJar)
    if (project.findProperty("copyJar") as? Boolean? == true) {
        copyJar.get().dependsOn(shadowJar)
        build.get().dependsOn(copyJar)
    }
}
