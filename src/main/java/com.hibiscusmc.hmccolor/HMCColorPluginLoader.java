package com.hibiscusmc.hmccolor;

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

    private static final String IDOFRONT_VERSION = "0.27.0-dev.3";

    private static final List<Repo> repositories = List.of(
            new Repo("central", MavenLibraryResolver.MAVEN_CENTRAL_DEFAULT_MIRROR),
            new Repo("madeinabyss-release", "https://repo.mineinabyss.com/releases"),
            new Repo("madeinabyss-snapshot", "https://repo.mineinabyss.com/snapshots"),
            new Repo("paper", "https://repo.papermc.io/repository/maven-public/")
    );

    static {
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
}
