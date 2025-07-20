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
import java.util.logging.Logger;

import static org.eclipse.aether.repository.RepositoryPolicy.CHECKSUM_POLICY_IGNORE;
import static org.eclipse.aether.repository.RepositoryPolicy.UPDATE_POLICY_ALWAYS;

public class HMCColorPluginLoader implements PluginLoader {
    private static final Logger LOGGER = Logger.getLogger("HMCColorPluginLoader");
    public static boolean usedPaperPluginLoader = false;
    private static final List<String> libraries = new ArrayList<>();

    private static final List<Repo> repositories = List.of(
            new Repo("central", "https://repo1.maven.org/maven2/"),
            new Repo("madeinabyss-release", "https://repo.mineinabyss.com/releases"),
            new Repo("madeinabyss-snapshot", "https://repo.mineinabyss.com/snapshots"),
            new Repo("paper", "https://repo.papermc.io/repository/maven-public/")
    );

    static {
        String idofront = "0.27.0-dev.2";

        libraries.add("org.jetbrains.kotlin:kotlin-stdlib:2.1.10");
        libraries.add("org.jetbrains.kotlinx:kotlinx-serialization-json:2.1.10");
        libraries.add("com.charleskorn.kaml:kaml:0.85.0");
        libraries.add("com.mineinabyss:idofront-serializers:" + idofront);

        libraries.add("com.mineinabyss:idofront-di:" + idofront);
        libraries.add("com.mineinabyss:idofront-commands:" + idofront);
        libraries.add("com.mineinabyss:idofront-config:" + idofront);
        libraries.add("com.mineinabyss:idofront-text-components:" + idofront);
        libraries.add("com.mineinabyss:idofront-logging:" + idofront);
        libraries.add("com.mineinabyss:idofront-util:" + idofront);
    }

    @Override
    public void classloader(@NotNull PluginClasspathBuilder classpathBuilder) {
        MavenLibraryResolver resolver = new MavenLibraryResolver();
        RepositoryPolicy snapshot = new RepositoryPolicy(true, UPDATE_POLICY_ALWAYS, CHECKSUM_POLICY_IGNORE);
        for (Repo repo : repositories) {
            LOGGER.info("Adding repository: " + repo.name() + " (" + repo.link() + ")");
            resolver.addRepository(
                    new RemoteRepository.Builder(repo.name(), "default", repo.link())
                            .setSnapshotPolicy(snapshot).build()
            );
        }

        for (String lib : libraries) {
            LOGGER.info("Adding dependency: " + lib);
            resolver.addDependency(new Dependency(new DefaultArtifact(lib), null));
        }

        try {
            classpathBuilder.addLibrary(resolver);
            LOGGER.info("MavenLibraryResolver added successfully");
        } catch (Exception e) {
            LOGGER.severe("Failed to add MavenLibraryResolver: " + e.getMessage());
            e.printStackTrace();
        }

        usedPaperPluginLoader = true;
    }

    private record Repo(String name, String link) {
    }

    public record Version(int major, int minor, int build) {
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
