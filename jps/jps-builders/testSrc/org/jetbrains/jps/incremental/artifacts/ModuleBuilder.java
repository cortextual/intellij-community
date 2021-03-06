package org.jetbrains.jps.incremental.artifacts;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.io.FileUtil;
import gnu.trove.THashMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jps.model.JpsModuleRootModificationUtil;
import org.jetbrains.jps.model.artifact.JpsArtifact;
import org.jetbrains.jps.model.artifact.elements.JpsPackagingElement;
import org.jetbrains.jps.model.java.JpsJavaDependencyScope;
import org.jetbrains.jps.model.module.JpsModule;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.jetbrains.jps.incremental.artifacts.LayoutElementTestUtil.root;

public class ModuleBuilder {
  private static Map<ArtifactBuilderTestCase, Pair<AtomicInteger, AtomicInteger>> testCaseToNameCounter = new THashMap<ArtifactBuilderTestCase, Pair<AtomicInteger, AtomicInteger>>();

  private final ArtifactBuilderTestCase testCase;
  private final String builderName;

  private String name;
  private JpsModule module;
  private String sourceRoot;

  private String fromSourceRoot;

  private JpsArtifact mainArtifact;

  public ModuleBuilder(String builderName, ArtifactBuilderTestCase testCase) {
    this(builderName, testCase, null);
  }

  public ModuleBuilder(String builderName, ArtifactBuilderTestCase testCase, String fromSourceRoot) {
    this.builderName = builderName;
    this.testCase = testCase;
    this.fromSourceRoot = fromSourceRoot;
  }

  public String getName() {
    return get().getName();
  }

  public ModuleBuilder name(@NotNull String name) {
    this.name = name;
    return this;
  }

  public JpsModule get() {
    if (module == null) {
      module = testCase.addModule(getOrCreateModuleName(), getSourceRoot());
      moduleCreated(module);
    }
    return module;
  }

  public JpsArtifact createArtifact() {
    JpsArtifact artifact = testCase.addArtifact(generateName(false), root().element(createPackagingElement(get())));
    if (mainArtifact == null) {
      mainArtifact = artifact;
    }
    return artifact;
  }

  public ModuleBuilder artifact() {
    createArtifact();
    return this;
  }

  public JpsArtifact getArtifact() {
    return mainArtifact;
  }

  protected JpsPackagingElement createPackagingElement(JpsModule module) {
    throw new AbstractMethodError();
  }

  protected void moduleCreated(JpsModule module) {
  }

  private String getSourceRoot() {
    if (sourceRoot == null) {
      sourceRoot = testCase.getAbsolutePath(getRelativeToProjectSourceRoot());
    }
    return sourceRoot;
  }

  private String getRelativeToProjectSourceRoot() {
    return getOrCreateModuleName() + "-src";
  }

  private String getOrCreateModuleName() {
    if (name == null) {
      name = generateName(true);
    }
    return name;
  }

  private String generateName(boolean module) {
    Pair<AtomicInteger, AtomicInteger> counter = testCaseToNameCounter.get(testCase);
    if (counter == null) {
      counter = Pair.create(new AtomicInteger(0), new AtomicInteger(0));
      testCaseToNameCounter.put(testCase, counter);
      Disposer.register(testCase.getTestRootDisposable(), new Disposable() {
        @Override
        public void dispose() {
          testCaseToNameCounter.remove(testCase);
        }
      });
    }
    return (module ? "m" : "a") + (module ? counter.first : counter.second).getAndIncrement();
  }

  public ModuleBuilder copy(String file) throws IOException {
    FileUtil.copy(fromSourceRoot == null ? new File(file) : new File(fromSourceRoot, file), new File(getSourceRoot(), file));
    return this;
  }

  public ModuleBuilder file(String pathRelativeToModuleSourceRoot, String text) throws IOException {
    doCreateFile(pathRelativeToModuleSourceRoot, text);
    return this;
  }

  public String createFile(String pathRelativeToModuleSourceRoot, String text) throws IOException {
    return FileUtil.toSystemIndependentName(doCreateFile(pathRelativeToModuleSourceRoot, text).getAbsolutePath());
  }

  private File doCreateFile(String pathRelativeToModuleSourceRoot, String text) throws IOException {
    File file = new File(getSourceRoot(), pathRelativeToModuleSourceRoot);
    FileUtil.writeToFile(file, text);
    return file;
  }

  public void assertCompiled(String... modulePaths) {
    String[] paths = new String[modulePaths.length];
    String root = getRelativeToProjectSourceRoot();
    for (int i = 0; i < modulePaths.length; i++) {
      paths[i] = root + '/' + modulePaths[i];
    }
    testCase.assertCompiled(builderName, paths);
  }

  public ModuleBuilder dependsOn(ModuleBuilder dependency) {
    JpsModuleRootModificationUtil.addDependency(get(), dependency.get());
    return this;
  }

  public ModuleBuilder dependsOnAndExports(ModuleBuilder dependency) {
    JpsModuleRootModificationUtil.addDependency(get(), dependency.get(), JpsJavaDependencyScope.COMPILE, true);
    return this;
  }
}
