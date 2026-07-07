package de.zorro909.codecheck.java;

import de.zorro909.codecheck.RepositoryPathProvider;
import de.zorro909.codecheck.config.CodeCheckConfigLoader;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Singleton
public class MavenProjectModelService implements ProjectModelService {

    private final Path repositoryRoot;

    private final CodeCheckConfigLoader configLoader;

    private volatile ProjectModel currentModel;

    @Inject
    public MavenProjectModelService(@Named(RepositoryPathProvider.REPOSITORY_DIRECTORY) Path repositoryRoot,
            CodeCheckConfigLoader configLoader) {
        this.repositoryRoot = repositoryRoot.toAbsolutePath().normalize();
        this.configLoader = configLoader;
    }

    public MavenProjectModelService(Path repositoryRoot) {
        this(repositoryRoot, CodeCheckConfigLoader.defaultsOnly());
    }

    @Override
    public ProjectModel currentModel() {
        ProjectModel model = currentModel;
        if (model == null) {
            model = refresh();
        }
        return model;
    }

    @Override
    public ProjectModel refresh() {
        int languageLevel = configLoader.load().javaProject().languageLevel();
        ProjectModel model = new ProjectModel(repositoryRoot, repositoryRoot, discoverModules(), languageLevel);
        currentModel = model;
        return model;
    }

    private List<MavenModule> discoverModules() {
        Set<Path> moduleRoots = new LinkedHashSet<>();
        moduleRoots.add(repositoryRoot);
        moduleRoots.addAll(readConfiguredModules(repositoryRoot.resolve("pom.xml")));
        return moduleRoots.stream().map(this::module).toList();
    }

    private List<Path> readConfiguredModules(Path pom) {
        if (!Files.exists(pom)) {
            return List.of();
        }
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            Element document = factory.newDocumentBuilder().parse(pom.toFile()).getDocumentElement();
            NodeList moduleNodes = document.getElementsByTagName("module");
            List<Path> modules = new ArrayList<>();
            for (int i = 0; i < moduleNodes.getLength(); i++) {
                String modulePath = moduleNodes.item(i).getTextContent().trim();
                if (!modulePath.isEmpty()) {
                    modules.add(repositoryRoot.resolve(modulePath).normalize());
                }
            }
            return modules;
        }
        catch (Exception e) {
            throw new IllegalStateException("Unable to read Maven modules from " + pom, e);
        }
    }

    private MavenModule module(Path moduleRoot) {
        String relative = repositoryRoot.equals(moduleRoot) ? "." : repositoryRoot.relativize(moduleRoot).toString();
        return new MavenModule(new ModuleId(relative), moduleRoot, List.of(moduleRoot.resolve("src/main/java")),
                List.of(moduleRoot.resolve("src/test/java")),
                List.of(moduleRoot.resolve("target/generated-sources/annotations")),
                List.of(moduleRoot.resolve("target/generated-test-sources/test-annotations")));
    }

}
