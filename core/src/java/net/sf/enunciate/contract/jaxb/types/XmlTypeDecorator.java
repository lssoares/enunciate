package net.sf.enunciate.contract.jaxb.types;

import com.sun.mirror.type.*;
import com.sun.mirror.util.TypeVisitor;
import net.sf.jelly.apt.decorations.TypeMirrorDecorator;
import net.sf.jelly.apt.decorations.type.DecoratedTypeMirror;

import java.util.Collection;

/**
 * A decorator that decorates the relevant type mirrors as xml type mirrors.
 *
 * @author Ryan Heaton
 */
public class XmlTypeDecorator implements TypeVisitor {

  private XmlTypeMirror decoratedTypeMirror;
  private String errorMessage = null;

  /**
   * Decorate a type mirror as an xml type.
   *
   * @param typeMirror The type mirror to decorate.
   * @return The xml type for the specified type mirror.
   * @throws XmlTypeException If the type is invalid or unknown as an xml type.
   */
  public static XmlTypeMirror decorate(TypeMirror typeMirror) throws XmlTypeException {
    if (typeMirror instanceof XmlTypeMirror) {
      return ((XmlTypeMirror) typeMirror);
    }
    XmlTypeDecorator instance = new XmlTypeDecorator();
    typeMirror.accept(instance);

    if (instance.errorMessage != null) {
      throw new XmlTypeException(instance.errorMessage);
    }

    return instance.decoratedTypeMirror;
  }

  public void visitTypeMirror(TypeMirror typeMirror) {
    this.decoratedTypeMirror = null;
    this.errorMessage = "Unknown xml type: " + typeMirror;
  }

  public void visitPrimitiveType(PrimitiveType primitiveType) {
    this.decoratedTypeMirror = new XmlPrimitiveType(primitiveType);
  }

  public void visitVoidType(VoidType voidType) {
    this.decoratedTypeMirror = null;
    this.errorMessage = "Void is not a valid xml type.";
  }

  public void visitReferenceType(ReferenceType referenceType) {
    this.decoratedTypeMirror = null;
    this.errorMessage = "Unknown xml type: " + referenceType;
  }

  public void visitDeclaredType(DeclaredType declaredType) {
    this.decoratedTypeMirror = null;
    this.errorMessage = "Unknown xml type: " + declaredType;
  }

  public void visitClassType(ClassType classType) {
    try {
      XmlClassType xmlClassType = new XmlClassType(classType);
      if (xmlClassType.isCollection()) {
        visitCollectionType(classType);
      }
      else {
        this.decoratedTypeMirror = xmlClassType;
      }
    }
    catch (XmlTypeException e) {
      this.errorMessage = e.getMessage();
    }
  }

  protected void visitCollectionType(DeclaredType classType) {
    //if it's a colleciton type, the xml type is its component type.
    Collection<TypeMirror> actualTypeArguments = classType.getActualTypeArguments();
    if (actualTypeArguments.isEmpty()) {
      //no type arguments, java.lang.Object type.
      this.decoratedTypeMirror = KnownXmlType.ANY_TYPE;
    }

    TypeMirror componentType = actualTypeArguments.iterator().next();
    componentType.accept(this);
  }

  public void visitEnumType(EnumType enumType) {
    try {
      this.decoratedTypeMirror = new XmlEnumType(enumType);
    }
    catch (XmlTypeException e) {
      this.errorMessage = e.getMessage();
    }
  }

  public void visitInterfaceType(InterfaceType interfaceType) {
    DecoratedTypeMirror type = (DecoratedTypeMirror) TypeMirrorDecorator.decorate(interfaceType);
    if (type.isCollection()) {
      visitCollectionType(interfaceType);
    }
    else {
      this.decoratedTypeMirror = null;
      this.errorMessage = "An interface type cannot be an xml type.";
    }
  }

  public void visitAnnotationType(AnnotationType annotationType) {
    this.decoratedTypeMirror = null;
    this.errorMessage = "An annotation type cannot be an xml type.";
  }

  public void visitArrayType(ArrayType arrayType) {
    arrayType.getComponentType().accept(this);

    if (this.errorMessage != null) {
      this.errorMessage = "Problem with the array component type: " + this.errorMessage;
    }
  }

  public void visitTypeVariable(TypeVariable typeVariable) {
    Collection<ReferenceType> bounds = typeVariable.getDeclaration().getBounds();
    if (bounds.isEmpty()) {
      this.decoratedTypeMirror = KnownXmlType.ANY_TYPE;
    }
    else {
      bounds.iterator().next().accept(this);
      if (this.errorMessage != null) {
        this.errorMessage = "Problem with the type variable bounds: " + this.errorMessage;
      }
    }
  }

  public void visitWildcardType(WildcardType wildcardType) {
    Collection<ReferenceType> upperBounds = wildcardType.getUpperBounds();
    if (upperBounds.isEmpty()) {
      this.decoratedTypeMirror = KnownXmlType.ANY_TYPE;
    }
    else {
      upperBounds.iterator().next().accept(this);

      if (this.errorMessage != null) {
        this.errorMessage = "Problem with wildcard bounds: " + this.errorMessage;
      }
    }
  }
}