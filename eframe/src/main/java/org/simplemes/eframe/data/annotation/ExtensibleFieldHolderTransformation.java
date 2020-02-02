/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.data.annotation;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.codehaus.groovy.ast.*;
import org.codehaus.groovy.ast.expr.*;
import org.codehaus.groovy.ast.stmt.BlockStatement;
import org.codehaus.groovy.ast.stmt.ExpressionStatement;
import org.codehaus.groovy.ast.stmt.Statement;
import org.codehaus.groovy.control.CompilePhase;
import org.codehaus.groovy.control.SourceUnit;
import org.codehaus.groovy.control.messages.SimpleMessage;
import org.codehaus.groovy.transform.ASTTransformation;
import org.codehaus.groovy.transform.GroovyASTTransformation;
import org.simplemes.eframe.ast.ASTUtils;
import org.simplemes.eframe.domain.annotation.DomainEntity;

import java.lang.reflect.Modifier;
import java.util.List;


/**
 * This class provides the compile-time AST transformation that adds an extensible field to the given
 * domain class.  This injects the custom field holder as a persistent field with a given maxSize.  Also
 * injects a static (non-persistent) field that holds the custom field definitions for the runtime validation.
 */
@SuppressWarnings("DefaultAnnotationParam")
@GroovyASTTransformation(phase = CompilePhase.CANONICALIZATION)
public class ExtensibleFieldHolderTransformation implements ASTTransformation {

  /**
   * Basic Constructor.
   */
  @SuppressWarnings("unused")
  public ExtensibleFieldHolderTransformation() {
  }

  public void visit(ASTNode[] astNodes, SourceUnit sourceUnit) {
    //System.out.println("visit");
    if (sourceUnit.getAST() == null) {
      return;
    }
    for (ASTNode astNode : astNodes) {
      if (astNode instanceof FieldNode) {
        FieldNode fieldNode = (FieldNode) astNode;
        ClassNode classNode = fieldNode.getDeclaringClass();
        validateUsage(classNode, sourceUnit);
        //addComplexFieldHolder(classNode, sourceUnit);
        addCustomFieldName(fieldNode.getName(), classNode, sourceUnit);
        addJsonProperty(fieldNode, sourceUnit);
        addValueSetter(classNode, sourceUnit);
        addValueGetter(classNode, sourceUnit);
        addConfigurableTypeAccessors(classNode, sourceUnit);
      }
    }
  }

  /**
   * Validates the annotation was used correctly.
   *
   * @param classNode  The class this annotation was used in.
   * @param sourceUnit The source location.
   */
  private void validateUsage(ClassNode classNode, SourceUnit sourceUnit) {
    List<AnnotationNode> annotations = classNode.getAnnotations(new ClassNode(DomainEntity.class));
    if (annotations.size() <= 0) {
      SimpleMessage message = new SimpleMessage("@ExtensibleFieldHolder must be used in class marked with @DomainEntity" + classNode, sourceUnit);
      sourceUnit.getErrorCollector().addError(message);
    }
  }


  /**
   * Adds a static property to the clazz that defines the field name.
   *
   * @param customFieldName The custom field name.
   * @param node            The node for the class itself.
   * @param sourceUnit      The source unit being processed.
   */
  private void addCustomFieldName(String customFieldName, ClassNode node, SourceUnit sourceUnit) {
    Expression initialValue = new ConstantExpression(customFieldName);
    ASTUtils.addField(ExtensibleFieldHolder.HOLDER_FIELD_NAME, Object.class, Modifier.PUBLIC | Modifier.STATIC, 0,
        initialValue, node, sourceUnit);
  }

  /**
   * Adds the Jackson JsonProperty annotation to the custom field holder so that it is serialized with a special name
   * that is ignored on de-serialization.
   *
   * @param fieldNode  The field to add the property annotation to.
   * @param sourceUnit The source.
   */
  private void addJsonProperty(FieldNode fieldNode, SourceUnit sourceUnit) {
    String fieldName = fieldNode.getName();

    // Adds a getter for the field to specify the JsonProperty name on just the getter.
    MethodNode getterNode = ASTUtils.addGetter(fieldNode, fieldNode.getDeclaringClass(), Modifier.PUBLIC, sourceUnit);

    // Add the Jackson annotation to rename the field.  This will prevent overlaying the holder value during imports.
    AnnotationNode annotationNode = new AnnotationNode(new ClassNode(JsonProperty.class));
    annotationNode.addMember("value", new ConstantExpression("_" + fieldName));
    getterNode.addAnnotation(annotationNode);
  }

  /**
   * Adds the Configurable Type accessors for any properties that are Configurable Types.
   *
   * @param node       The node for the class itself.
   * @param sourceUnit The source unit being processed.
   */
  private void addConfigurableTypeAccessors(ClassNode node, SourceUnit sourceUnit) {
    for (FieldNode fieldNode : node.getFields()) {
      for (ClassNode interfaceNode : fieldNode.getType().getInterfaces()) {
        if (interfaceNode.getName().equals("org.simplemes.eframe.data.ConfigurableTypeInterface")) {
          addValueSetter(node, sourceUnit, fieldNode.getName());
          addValueGetter(node, sourceUnit, fieldNode.getName());
        }
      }
    }

  }

  /**
   * Adds the value getter to the annotated class.  This allows generic access to the custom fields by the caller.
   * Adds the getFieldValue() method.
   *
   * @param classNode  The class to add the method to.
   * @param sourceUnit The compiler source unit being processed (used to errors).
   */
  private void addValueGetter(ClassNode classNode, SourceUnit sourceUnit) {
    addValueGetter(classNode, sourceUnit, null);
  }

  /**
   * Adds the value getter to the annotated class.  This allows generic access to the custom fields by the caller.
   * Adds the getFieldValue() method.
   *
   * @param classNode  The class to add the method to.
   * @param sourceUnit The compiler source unit being processed (used to errors).
   * @param prefix     The field prefix to use for the getter (e.g. prefix='rmaType' will use a prefix of 'rmaType_').
   */
  private void addValueGetter(ClassNode classNode, SourceUnit sourceUnit, String prefix) {
    String getterName = "getFieldValue";
    Parameter[] parameters = {
        new Parameter(new ClassNode(String.class), "fieldName")
    };
    if (prefix != null) {
      getterName = "get" + upperCaseFirstLetter(prefix) + "Value";
    }

    // Make sure the method doesn't exist already
    if (ASTUtils.methodExists(classNode, getterName, parameters)) {
      sourceUnit.getErrorCollector().addError(new SimpleMessage(getterName + "() already exists in " + classNode, sourceUnit));
      return;
    }

    ArgumentListExpression argumentListExpression = new ArgumentListExpression();
    argumentListExpression.addExpression(new VariableExpression("this"));
    argumentListExpression.addExpression(new VariableExpression("fieldName"));
    if (prefix != null) {
      argumentListExpression.addExpression(new ConstantExpression(prefix));
    }

    PropertyExpression instanceExpression = new PropertyExpression(
        new ClassExpression(new ClassNode("org.simplemes.eframe.custom.ExtensibleFieldHelper", Modifier.PUBLIC, null)),
        "instance");

    MethodCallExpression method = new MethodCallExpression(
        instanceExpression,
        "getFieldValue", argumentListExpression);
    BlockStatement methodBody = new BlockStatement(new Statement[]{new ExpressionStatement(method)}, new VariableScope());

    classNode.addMethod(getterName,
        Modifier.PUBLIC,
        ClassHelper.OBJECT_TYPE,
        parameters,
        null, methodBody);
  }

  /**
   * Adds the value setter to the annotated class.  This allows generic access to the field by the caller.
   * Adds the setFieldValue() method.
   *
   * @param classNode  The class to add the method to.
   * @param sourceUnit The compiler source unit being processed (used to errors).
   */
  private void addValueSetter(ClassNode classNode, SourceUnit sourceUnit) {
    addValueSetter(classNode, sourceUnit, null);
  }

  /**
   * Adds the value setter to the annotated class.  This allows generic access to the field by the caller.
   * Adds the setFieldValue() method.
   *
   * @param classNode  The class to add the method to.
   * @param sourceUnit The compiler source unit being processed (used to errors).
   * @param prefix     The field prefix to use for the getter (e.g. prefix='rmaType' will use a prefix of 'rmaType_').
   */
  private void addValueSetter(ClassNode classNode, SourceUnit sourceUnit, String prefix) {
    // Someday, we may want to let the IDE see the set/getFieldValue() methods.  Looks like a plugin is needed:
    // https://youtrack.jetbrains.com/issue/IDEA-217030?_ga=2.21253177.348530212.1568915239-1388074175.1566386790
    String setterName = "setFieldValue";
    Parameter[] parameters = {
        new Parameter(new ClassNode(String.class), "fieldName"),
        new Parameter(new ClassNode(Object.class), "value")
    };
    if (prefix != null) {
      setterName = "set" + upperCaseFirstLetter(prefix) + "Value";
    }

    // Make sure the method doesn't exist already
    if (ASTUtils.methodExists(classNode, setterName, parameters)) {
      sourceUnit.getErrorCollector().addError(new SimpleMessage(setterName + "() already exists in " + classNode, sourceUnit));
    }

    ArgumentListExpression argumentListExpression = new ArgumentListExpression();
    argumentListExpression.addExpression(new VariableExpression("this"));
    argumentListExpression.addExpression(new VariableExpression("fieldName"));
    argumentListExpression.addExpression(new VariableExpression("value"));
    if (prefix != null) {
      argumentListExpression.addExpression(new ConstantExpression(prefix));
    }

    PropertyExpression instanceExpression = new PropertyExpression(
        new ClassExpression(new ClassNode("org.simplemes.eframe.custom.ExtensibleFieldHelper", Modifier.PUBLIC, null)),
        "instance");

    MethodCallExpression method = new MethodCallExpression(
        instanceExpression,
        "setFieldValue", argumentListExpression);
    BlockStatement methodBody = new BlockStatement(new Statement[]{new ExpressionStatement(method)}, new VariableScope());

    classNode.addMethod(setterName,
        Modifier.PUBLIC,
        ClassHelper.VOID_TYPE,
        parameters,
        null, methodBody);
  }

  /**
   * Shifts the first letter of the string to uppercase.
   *
   * @param s The string to adjust.
   * @return The adjusted string.
   */
  String upperCaseFirstLetter(String s) {
    if (s == null || s.length() == 0) {
      return null;
    }
    String first = s.substring(0, 1).toUpperCase();

    return first + s.substring(1);
  }

}
