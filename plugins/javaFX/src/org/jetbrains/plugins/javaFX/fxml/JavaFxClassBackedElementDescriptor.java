package org.jetbrains.plugins.javaFX.fxml;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.*;
import com.intellij.psi.xml.XmlAttribute;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;
import com.intellij.util.ArrayUtil;
import com.intellij.xml.XmlAttributeDescriptor;
import com.intellij.xml.XmlElementDescriptor;
import com.intellij.xml.XmlElementsGroup;
import com.intellij.xml.XmlNSDescriptor;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * User: anna
 * Date: 1/9/13
 */
public class JavaFxClassBackedElementDescriptor implements XmlElementDescriptor {
  private final PsiClass myPsiClass;
  private final String myName;

  public JavaFxClassBackedElementDescriptor(String name, XmlTag tag) {
    myName = name;
    myPsiClass = findPsiClass(name, JavaFXNSDescriptor.parseImports((XmlFile)tag.getContainingFile()), tag, tag.getProject());
  }

  private static PsiClass findPsiClass(String name, List<String> imports, XmlTag tag, Project project) {
    PsiClass psiClass = null;
    if (imports != null) {
      JavaPsiFacade psiFacade = JavaPsiFacade.getInstance(project);

      PsiFile file = tag.getContainingFile();
      for (String anImport : imports) {
        if (StringUtil.endsWith(anImport, "." + name)) {
          psiClass = psiFacade.findClass(anImport, file.getResolveScope()); 
        } else if (StringUtil.endsWith(anImport, ".*")) {
          psiClass = psiFacade.findClass(StringUtil.trimEnd(anImport, "*") + name, file.getResolveScope());
        }
        if (psiClass != null) {
          return psiClass;
        }
      }
    }
    return psiClass;
  }

  @Override
  public String getQualifiedName() {
    return myPsiClass != null ? myPsiClass.getQualifiedName() : getName();
  }

  @Override
  public String getDefaultName() {
    return getName();
  }

  @Override
  public XmlElementDescriptor[] getElementsDescriptors(XmlTag context) {
    if (context != null) {
      //todo
      XmlElementDescriptor descriptor = context.getDescriptor();
      if (descriptor instanceof JavaFxClassBackedElementDescriptor) {
        
      } else if (descriptor instanceof JavaFxListPropertyElementDescriptor) {
        
      }
    }
    return XmlElementDescriptor.EMPTY_ARRAY;
  }

  @Nullable
  @Override
  public XmlElementDescriptor getElementDescriptor(XmlTag childTag, XmlTag contextTag) {
    final String name = childTag.getName();
    if (StringUtil.isCapitalized(name)) {
      return new JavaFxClassBackedElementDescriptor(name, childTag);
    }
    else {
      return myPsiClass != null ? new JavaFxListPropertyElementDescriptor(myPsiClass, name) : null;
    }
  }

  @Override
  public XmlAttributeDescriptor[] getAttributesDescriptors(@Nullable XmlTag context) {
    if (context != null) {
      //todo
      final String name = context.getName();
      if (Comparing.equal(name, getName()) && myPsiClass != null) {
        final PsiField[] fields = myPsiClass.getAllFields();
        if (fields.length > 0) {
          final XmlAttributeDescriptor[] simpleAttrs = new XmlAttributeDescriptor[fields.length];
          for (int i = 0; i < fields.length; i++) {
            simpleAttrs[i] = new JavaFxPropertyBackedAttributeDescriptor(fields[i].getName(), myPsiClass);
          }
          return simpleAttrs;
        }
      }
    }
    return XmlAttributeDescriptor.EMPTY;
  }

  @Nullable
  @Override
  public XmlAttributeDescriptor getAttributeDescriptor(@NonNls String attributeName, @Nullable XmlTag context) {
    return new JavaFxPropertyBackedAttributeDescriptor(attributeName, (PsiClass)getDeclaration());
  }

  @Nullable
  @Override
  public XmlAttributeDescriptor getAttributeDescriptor(XmlAttribute attribute) {
    return getAttributeDescriptor(attribute.getName(), attribute.getParent());
  }

  @Override
  public XmlNSDescriptor getNSDescriptor() {
    return null;
  }

  @Nullable
  @Override
  public XmlElementsGroup getTopGroup() {
    return null;
  }

  @Override
  public int getContentType() {
    return CONTENT_TYPE_UNKNOWN;
  }

  @Nullable
  @Override
  public String getDefaultValue() {
    return null;
  }

  @Override
  public PsiElement getDeclaration() {
    return myPsiClass;
  }

  @Override
  public String getName(PsiElement context) {
    return getName();
  }

  @Override
  public String getName() {
    return myName;
  }

  @Override
  public void init(PsiElement element) {
  }

  @Override
  public Object[] getDependences() {
    return ArrayUtil.EMPTY_OBJECT_ARRAY;
  }
}
