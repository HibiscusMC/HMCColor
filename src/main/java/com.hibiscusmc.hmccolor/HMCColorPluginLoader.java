package com.hibiscusmc.hmccolor;

import io.papermc.paper.ServerBuildInfo;
import io.papermc.paper.plugin.loader.PluginClasspathBuilder;
import io.papermc.paper.plugin.loader.PluginLoader;
import io.papermc.paper.plugin.loader.library.impl.MavenLibraryResolver;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.repository.RepositoryPolicy;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

import static org.eclipse.aether.repository.RepositoryPolicy.CHECKSUM_POLICY_IGNORE;
import static org.eclipse.aether.repository.RepositoryPolicy.UPDATE_POLICY_ALWAYS;

public class HMCColorPluginLoader implements PluginLoader {
    private static final List<String> libraries = new ArrayList<>();

    private static String IDOFRONT_VERSION;

    private static final List<Repo> repositories = List.of(
            new Repo("central", getDefaultMavenCentralMirror()),
            new Repo("madeinabyss-release", "https://repo.mineinabyss.com/releases"),
            new Repo("madeinabyss-snapshot", "https://repo.mineinabyss.com/snapshots"),
            new Repo("paper", "https://repo.papermc.io/repository/maven-public/")
    );

    static {

        if (Version.atleast("1.21.6"))
            IDOFRONT_VERSION = "0.27.0-dev.4";
        else if (Version.atleast("1.21.4"))
            IDOFRONT_VERSION = "0.26.6";
        else IDOFRONT_VERSION = "0.25.24";

        libraries.add("org.jetbrains.kotlin:kotlin-stdlib:2.2.0");
        libraries.add("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0");
        libraries.add("com.charleskorn.kaml:kaml:0.85.0");
        libraries.add("com.charleskorn.kaml:kaml-jvm:0.85.0");

        libraries.add("com.mineinabyss:idofront-di:" + IDOFRONT_VERSION);
        libraries.add("com.mineinabyss:idofront-config:" + IDOFRONT_VERSION);
        libraries.add("com.mineinabyss:idofront-util:" + IDOFRONT_VERSION);
        libraries.add("com.mineinabyss:idofront-serializers:" + IDOFRONT_VERSION);
    }

    @Override
    public void classloader(@NotNull PluginClasspathBuilder classpathBuilder) {
        MavenLibraryResolver resolver = new MavenLibraryResolver();
        RepositoryPolicy snapshot = new RepositoryPolicy(true, UPDATE_POLICY_ALWAYS, CHECKSUM_POLICY_IGNORE);

        for (Repo repo : repositories) resolver.addRepository(new RemoteRepository.Builder(repo.name(), "default", repo.link()).setSnapshotPolicy(snapshot).build());
        for (String lib : libraries) resolver.addDependency(new Dependency(new DefaultArtifact(lib), null));

        classpathBuilder.addLibrary(resolver);
    }

    private record Repo(String name, String link) {
    }

    private static String getDefaultMavenCentralMirror() {
        String central = System.getenv("PAPER_DEFAULT_CENTRAL_REPOSITORY");
        if (central == null) {
            central = System.getProperty("org.bukkit.plugin.java.LibraryLoader.centralURL");
        }

        if (central == null) {
            central = "https://maven-central.storage-download.googleapis.com/maven2";
        }

        return central;
    }

    private record Version(int major, int minor, int build) {
        // Constructor to parse a version string like "1.20.1"
        private Version(String version) {
            this(fromString(version)[0], fromString(version)[1], fromString(version)[2]);
        }

        // Helper method to parse version string into components
        private static int[] fromString(String version) {
            String[] parts = version.split("\\.");
            if (parts.length != 3) {
                throw new IllegalArgumentException("Version string must be in format 'major.minor.build'");
            }
            try {
                int major = Integer.parseInt(parts[0]);
                int minor = Integer.parseInt(parts[1]);
                int build = Integer.parseInt(parts[2]);
                return new int[]{major, minor, build};
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid version number format: " + version, e);
            }
        }

        // Static field for current version
        private static final Version currentVersion = new Version(ServerBuildInfo.buildInfo().minecraftVersionId());

        // Method to check if currentVersion is at least the given version
        public static boolean atleast(String version) {
            Version other = new Version(version);
            if (currentVersion.major() > other.major()) {
                return true;
            } else if (currentVersion.major() == other.major()) {
                if (currentVersion.minor() > other.minor()) {
                    return true;
                } else if (currentVersion.minor() == other.minor()) {
                    return currentVersion.build() >= other.build();
                }
            }
            return false;
        }
    }
}
