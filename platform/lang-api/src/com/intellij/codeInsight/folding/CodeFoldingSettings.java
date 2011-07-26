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

package com.intellij.codeInsight.folding;

import com.intellij.ide.IdeBundle;
import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.components.*;
import com.intellij.util.xmlb.XmlSerializerUtil;
import org.jetbrains.annotations.NotNull;

import java.io.File;

/**
 * @author yole
 */
@State(
  name="CodeFoldingSettings",
  storages= {
    @Storage(
      file = "$APP_CONFIG$/editor.codeinsight.xml"
    )}
)
public class CodeFoldingSettings implements PersistentStateComponent<CodeFoldingSettings>, ExportableComponent {
  @SuppressWarnings({"WeakerAccess"}) public boolean COLLAPSE_IMPORTS = true;
  @SuppressWarnings({"WeakerAccess"}) public boolean COLLAPSE_METHODS = false;
  @SuppressWarnings({"WeakerAccess"}) public boolean COLLAPSE_FILE_HEADER = true;
  @SuppressWarnings({"WeakerAccess"}) public boolean COLLAPSE_DOC_COMMENTS = false;

  public static CodeFoldingSettings getInstance() {
    return ServiceManager.getService(CodeFoldingSettings.class);
  }

  public CodeFoldingSettings getState() {
    return this;
  }

  public void loadState(final CodeFoldingSettings state) {
    XmlSerializerUtil.copyBean(state, this);
  }

  @NotNull
  public File[] getExportFiles() {
    return new File[] { PathManager.getOptionsFile("editor.codeinsight") };
  }

  @NotNull
  public String getPresentableName() {
    return IdeBundle.message("code.folding.settings");
  }
}
