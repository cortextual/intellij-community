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
package com.intellij.xdebugger.impl.evaluate.quick;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.SelectionModel;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.xdebugger.XDebugSession;
import com.intellij.xdebugger.XDebuggerManager;
import com.intellij.xdebugger.evaluation.XDebuggerEvaluator;
import com.intellij.xdebugger.frame.XStackFrame;
import com.intellij.xdebugger.impl.evaluate.quick.common.AbstractValueHint;
import com.intellij.xdebugger.impl.evaluate.quick.common.QuickEvaluateHandler;
import com.intellij.xdebugger.impl.evaluate.quick.common.ValueHintType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;

/**
 * @author nik
 */
public class XQuickEvaluateHandler extends QuickEvaluateHandler {
  private static final Logger LOG = Logger.getInstance("#com.intellij.xdebugger.impl.evaluate.quick.XQuickEvaluateHandler");

  public boolean isEnabled(@NotNull final Project project) {
    XDebugSession session = XDebuggerManager.getInstance(project).getCurrentSession();
    if (session == null || !session.isSuspended()) {
      return false;
    }
    XStackFrame stackFrame = session.getCurrentStackFrame();
    return stackFrame != null && stackFrame.getEvaluator() != null;
  }

  public AbstractValueHint createValueHint(@NotNull final Project project, @NotNull final Editor editor, @NotNull final Point point, final ValueHintType type) {
    final XDebugSession session = XDebuggerManager.getInstance(project).getCurrentSession();
    if (session == null) return null;

    XStackFrame stackFrame = session.getCurrentStackFrame();
    if (stackFrame == null) return null;
    final XDebuggerEvaluator evaluator = stackFrame.getEvaluator();
    if (evaluator == null) return null;

    return PsiDocumentManager.getInstance(project).commitAndRunReadAction(new Computable<XValueHint>() {
      public XValueHint compute() {
        int offset = AbstractValueHint.calculateOffset(editor, point);
        TextRange range = getExpressionRange(evaluator, project, type, editor, offset);
        if (range == null) return null;
        int textLength = editor.getDocument().getTextLength();
        if (range.getStartOffset() > range.getEndOffset() || range.getStartOffset() < 0 || range.getEndOffset() > textLength) {
          LOG.error("invalid range: " + range + ", text length = " + textLength + ", evaluator: " + evaluator);
          return null;
        }

        return new XValueHint(project, editor, point, type, range, evaluator, session);
      }
    });
  }

  @Nullable
  private static TextRange getExpressionRange(final XDebuggerEvaluator evaluator, final Project project, final ValueHintType type, final Editor editor,
                                              final int offset) {
    SelectionModel selectionModel = editor.getSelectionModel();
    int selectionStart = selectionModel.getSelectionStart();
    int selectionEnd = selectionModel.getSelectionEnd();
    if ((type == ValueHintType.MOUSE_CLICK_HINT || type == ValueHintType.MOUSE_ALT_OVER_HINT) && selectionModel.hasSelection()
        && selectionStart <= offset && offset <= selectionEnd) {
      return new TextRange(selectionStart, selectionEnd);
    }
    return evaluator.getExpressionRangeAtOffset(project, editor.getDocument(), offset, false);
  }

  public boolean canShowHint(@NotNull final Project project) {
    return isEnabled(project);
  }

  public int getValueLookupDelay(final Project project) {
    XDebugSession session = XDebuggerManager.getInstance(project).getCurrentSession();
    if (session != null) {
      XStackFrame stackFrame = session.getCurrentStackFrame();
      if (stackFrame != null) {
        XDebuggerEvaluator evaluator = stackFrame.getEvaluator();
        if (evaluator != null) {
          return evaluator.getValuePopupDelay();
        }
      }
    }
    return 700;
  }
}
