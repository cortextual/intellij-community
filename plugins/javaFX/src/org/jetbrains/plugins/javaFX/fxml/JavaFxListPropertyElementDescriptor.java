package org.jetbrains.plugins.javaFX.fxml;

import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.xml.XmlAttribute;
import com.intellij.psi.xml.XmlTag;
import com.intellij.util.ArrayUtil;
import com.intellij.xml.XmlAttributeDescriptor;
import com.intellij.xml.XmlElementDescriptor;
import com.intellij.xml.XmlElementsGroup;
import com.intellij.xml.XmlNSDescriptor;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.Nullable;

/**
 * User: anna
 * Date: 1/10/13
 */
public class JavaFxListPropertyElementDescriptor implements XmlElementDescriptor {
  private final PsiClass myPsiClass;
  private final String myName;

  public JavaFxListPropertyElementDescriptor(PsiClass psiClass, String name) {
    myPsiClass = psiClass;
    myName = name;
  }

  @Override
  public String getQualifiedName() {
    return getName();
  }

  @Override
  public String getDefaultName() {
    return getName();
  }

  @Override
  public XmlElementDescriptor[] getElementsDescriptors(XmlTag context) {
    //todo
    
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
    return XmlAttributeDescriptor.EMPTY;
  }

  @Nullable
  @Override
  public XmlAttributeDescriptor getAttributeDescriptor(@NonNls String attributeName, @Nullable XmlTag context) {
    return null;
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
    return myPsiClass.findFieldByName(myName, true);
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
  public void init(PsiElement element) {}

  @Override
  public Object[] getDependences() {
    return ArrayUtil.EMPTY_OBJECT_ARRAY;
  }
}
