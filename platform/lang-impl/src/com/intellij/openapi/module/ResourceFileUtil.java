/*
 * Copyright 2000-2009 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.intellij.openapi.module;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.roots.impl.DirectoryIndex;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.FilteredQuery;
import org.jetbrains.annotations.Nullable;

public class ResourceFileUtil {
  private ResourceFileUtil() {
  }

  @Nullable
  public static VirtualFile findResourceFile(final String name, final Module inModule) {
    final VirtualFile[] sourceRoots = ModuleRootManager.getInstance(inModule).getSourceRoots();
    final ProjectFileIndex fileIndex = ProjectRootManager.getInstance(inModule.getProject()).getFileIndex();
    for (final VirtualFile sourceRoot : sourceRoots) {
      final String packagePrefix = fileIndex.getPackageNameByDirectory(sourceRoot);
      final String prefix = packagePrefix == null || packagePrefix.isEmpty() ? null : packagePrefix.replace('.', '/') + "/";
      final String relPath = prefix != null && name.startsWith(prefix) && name.length() > prefix.length() ? name.substring(prefix.length()) : name;
      final String fullPath = sourceRoot.getPath() + "/" + relPath;
      final VirtualFile fileByPath = LocalFileSystem.getInstance().findFileByPath(fullPath);
      if (fileByPath != null) {
        return fileByPath;
      }
    }
    return null;
  }

  @Nullable
  public static VirtualFile findResourceFileInDependents(final Module searchFromModule, final String fileName) {
    return findResourceFileInScope(fileName, searchFromModule.getProject(), searchFromModule.getModuleWithDependenciesScope());
  }

  @Nullable
  public static VirtualFile findResourceFileInProject(final Project project, final String resourceName) {
    return findResourceFileInScope(resourceName, project, GlobalSearchScope.projectScope(project));
  }

  @Nullable
  public static VirtualFile findResourceFileInScope(final String resourceName,
                                                    final Project project,
                                                    final GlobalSearchScope scope) {
    int index = resourceName.lastIndexOf('/');
    String packageName = index >= 0 ? resourceName.substring(0, index).replace('/', '.') : "";
    final String fileName = index >= 0 ? resourceName.substring(index+1) : resourceName;

    final VirtualFile dir = new FilteredQuery<VirtualFile>(
      DirectoryIndex.getInstance(project).getDirectoriesByPackageName(packageName, false), new Condition<VirtualFile>() {
      @Override
      public boolean value(final VirtualFile file) {
        final VirtualFile child = file.findChild(fileName);
        return child != null && scope.contains(child);
      }
    }).findFirst();
    return dir != null ? dir.findChild(fileName) : null;
  }
}
