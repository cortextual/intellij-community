/*
 * Copyright 2000-2011 JetBrains s.r.o.
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
package com.intellij.ui;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileTypes.StdFileTypes;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComponentWithBrowseButton;
import com.intellij.psi.*;

/**
 * User: anna
 */
public class EditorTextFieldWithBrowseButton extends ComponentWithBrowseButton<EditorTextField> implements TextAccessor {
  public EditorTextFieldWithBrowseButton(Project project, boolean isClassAccepted) {
    super(new EditorTextField(createDocument("", PsiManager.getInstance(project), isClassAccepted), project, StdFileTypes.JAVA), null);
  }

  private static Document createDocument(final String text, PsiManager manager, boolean isClassesAccepted) {
    PsiElement defaultPackage = JavaPsiFacade.getInstance(manager.getProject()).findPackage("");
    final JavaCodeFragment fragment = JavaPsiFacade.getInstance(manager.getProject()).getElementFactory()
      .createReferenceCodeFragment(text, defaultPackage, true, isClassesAccepted);
    fragment.setVisibilityChecker(JavaCodeFragment.VisibilityChecker.EVERYTHING_VISIBLE);
    return PsiDocumentManager.getInstance(manager.getProject()).getDocument(fragment);
  }

  @Override
  public void setText(String text) {
    if (text == null) text = "";
    getChildComponent().setText(text);
  }

  @Override
  public String getText() {
    return getChildComponent().getText();
  }
}
