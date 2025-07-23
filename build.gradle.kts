plugins {
    id("com.gradleup.shadow") version "8.3.5"
    id ("org.jetbrains.kotlin.jvm") version "2.2.0"
    id("org.jetbrains.kotlin.plugin.serialization") version "2.2.0"
}

val pluginVersion: String by project
val idofrontVersion: String by project
group = "com.hibiscusmc"
version = pluginVersion

repositories {
    mavenCentral()
    gradlePluginPortal()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://mvn.lumine.io/repository/maven-public/") { metadataSources { artifact() } }// MythicMobs
    maven("https://jitpack.io")
    maven("https://repo.nexomc.com/releases")
    maven("https://repo.mineinabyss.com/releases")
    maven("https://repo.mineinabyss.com/snapshots")
    maven("https://repo.triumphteam.dev/snapshots")
    mavenLocal()
}

dependencies {
    compileOnly("org.jetbrains.kotlin:kotlin-stdlib:2.2.0")
    compileOnly("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")
    compileOnly("com.charleskorn.kaml:kaml:0.85.0")

    compileOnly("io.papermc.paper:paper-api:1.21.8-R0.1-SNAPSHOT")
    compileOnly("com.nexomc:nexo:1.8.0")
    compileOnly("com.github.LoneDev6:api-itemsadder:3.4.1e")
    compileOnly("io.lumine:Mythic-Dist:5.9.5")
    compileOnly("io.lumine:MythicCrucible:2.2.0-SNAPSHOT")
    compileOnly("com.mineinabyss:geary-papermc:0.32.6")

    compileOnly("com.mineinabyss:idofront-di:$idofrontVersion")
    compileOnly("com.mineinabyss:idofront-config:$idofrontVersion")
    compileOnly("com.mineinabyss:idofront-util:$idofrontVersion")
    compileOnly("com.mineinabyss:idofront-serializers:$idofrontVersion")

    implementation("dev.triumphteam:triumph-gui:3.2.0-SNAPSHOT") { exclude("net.kyori") }
}

kotlin {
    jvmToolchain(21)
    compilerOptions {
        freeCompilerArgs.addAll(
            "-opt-in=kotlinx.serialization.ExperimentalSerializationApi",
            "-opt-in=kotlin.ExperimentalUnsignedTypes",
            "-Xcontext-receivers"
        )
    }
}

tasks {

    shadowJar {
        filesMatching(arrayOf("paper-plugin.yml").asIterable()) {
            expand(mapOf("version" to pluginVersion))
        }
        relocate("dev.triumphteam.gui", "com.hibiscusmc.hmccolor.shaded.gui")
        relocate("org.spongepowered.configurate", "com.hibiscusmc.hmccolor.shaded.configurate")
        relocate("org.bstats", "com.hibiscusmc.hmccolor.shaded.bstats")
    }

    build.get().dependsOn(shadowJar)
}
