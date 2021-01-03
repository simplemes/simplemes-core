/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.data.annotation;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.codehaus.groovy.ast.*;
import org.codehaus.groovy.ast.expr.*;
import org.codehaus.groovy.ast.stmt.BlockStatement;
import org.codehaus.groovy.ast.stmt.ExpressionStatement;
import org.codehaus.groovy.ast.stmt.ReturnStatement;
import org.codehaus.groovy.ast.stmt.Statement;
import org.codehaus.groovy.control.CompilePhase;
import org.codehaus.groovy.control.SourceUnit;
import org.codehaus.groovy.control.messages.SimpleMessage;
import org.codehaus.groovy.transform.ASTTransformation;
import org.codehaus.groovy.transform.GroovyASTTransformation;
import org.simplemes.eframe.ast.ASTUtils;
import org.simplemes.eframe.custom.BaseFieldHolderMap;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


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
        addComplexFieldHolder(classNode, sourceUnit);
        addFieldHolderMap(fieldNode.getName(), classNode, sourceUnit);
        addFieldHolderGetter(fieldNode.getName(), classNode, sourceUnit);
        addFieldHolderSetter(fieldNode.getName(), classNode, sourceUnit);
        addCustomFieldName(fieldNode.getName(), classNode, sourceUnit);
        addJsonIgnore(fieldNode, sourceUnit);
        addValueSetter(classNode, sourceUnit);
        addValueGetter(classNode, sourceUnit);
        addPropertyMissingSetter(classNode, sourceUnit);
        addPropertyMissingGetter(classNode, sourceUnit);
      }
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
    List<ASTNode> nodes = ASTUtils.addField(ExtensibleFieldHolder.HOLDER_FIELD_NAME, Object.class, Modifier.PUBLIC | Modifier.STATIC,
        initialValue, node, sourceUnit);
    if (nodes.get(0) instanceof FieldNode) {
      addJsonIgnore((FieldNode) nodes.get(0), sourceUnit);
    }
  }

  /**
   * Adds the Jackson JsonProperty annotation to the custom field holder so that it is serialized with the given name.
   *
   * @param fieldNode    The field to add the property annotation to.
   * @param propertyName The name of the JSON property.
   * @param sourceUnit   The source.
   */
  private void addJsonProperty(FieldNode fieldNode, String propertyName, @SuppressWarnings("unused") SourceUnit sourceUnit) {
    AnnotationNode annotationNode = new AnnotationNode(new ClassNode(JsonProperty.class));
    annotationNode.addMember("value", new ConstantExpression(propertyName));
    fieldNode.addAnnotation(annotationNode);
  }

  /**
   * Adds the Jackson JsonIgnore annotation to the given field so that Jackson will ignore it.
   *
   * @param fieldNode  The field to add the property annotation to.
   * @param sourceUnit The source.
   */
  private void addJsonIgnore(FieldNode fieldNode, @SuppressWarnings("unused") SourceUnit sourceUnit) {
    // Add the Jackson annotation to rename the field.  This will prevent overlaying the holder value during imports.
    AnnotationNode annotationNode = new AnnotationNode(new ClassNode(JsonIgnore.class));
    fieldNode.addAnnotation(annotationNode);
  }

  /**
   * Adds the value getter to the annotated class.  This allows generic access to the custom fields by the caller.
   * Adds the getFieldValue() method.
   *
   * @param classNode  The class to add the method to.
   * @param sourceUnit The compiler source unit being processed (used to errors).
   */
  private void addValueGetter(ClassNode classNode, SourceUnit sourceUnit) {
    String getterName = "getFieldValue";
    Parameter[] parameters = {
        new Parameter(new ClassNode(String.class), "fieldName")
    };

    // Make sure the method doesn't exist already
    if (ASTUtils.methodExists(classNode, getterName, parameters)) {
      sourceUnit.getErrorCollector().addError(new SimpleMessage(getterName + "() already exists in " + classNode, sourceUnit));
      return;
    }

    ArgumentListExpression argumentListExpression = new ArgumentListExpression();
    argumentListExpression.addExpression(new VariableExpression("this"));
    argumentListExpression.addExpression(new VariableExpression("fieldName"));

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
    String setterName = "setFieldValue";
    Parameter[] parameters = {
        new Parameter(new ClassNode(String.class), "fieldName"),
        new Parameter(new ClassNode(Object.class), "value")
    };

    // Make sure the method doesn't exist already
    if (ASTUtils.methodExists(classNode, setterName, parameters)) {
      sourceUnit.getErrorCollector().addError(new SimpleMessage(setterName + "() already exists in " + classNode, sourceUnit));
    }

    ArgumentListExpression argumentListExpression = new ArgumentListExpression();
    argumentListExpression.addExpression(new VariableExpression("this"));
    argumentListExpression.addExpression(new VariableExpression("fieldName"));
    argumentListExpression.addExpression(new VariableExpression("value"));

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
   * Adds a Map field to the domain class to hold the complex custom field values such
   * as lists of references.  This is a transient field so that it is not persisted directly.
   * Initializes the field as an empty map.
   * <p>
   * <b>Note:</b> This ComplexCustomFieldSerializer depends on the _complexCustomFields Map added
   * to all extensible field domains.
   *
   * @param node       The node for the class itself.
   * @param sourceUnit The source unit being processed.
   */
  private void addComplexFieldHolder(ClassNode node, SourceUnit sourceUnit) {
    MapEntryExpression entry = new MapEntryExpression(new ConstantExpression(ExtensibleFieldHolder.COMPLEX_THIS_NAME),
        new VariableExpression("this"));
    List<MapEntryExpression> initEntries = new ArrayList<>();
    initEntries.add(entry);
    Expression init = new MapExpression(initEntries);
    List<ASTNode> list = ASTUtils.addField(ExtensibleFieldHolder.COMPLEX_CUSTOM_FIELD_NAME, Map.class,
        Modifier.PUBLIC | Modifier.TRANSIENT, init, node, sourceUnit);
    for (ASTNode n : list) {
      if (n instanceof FieldNode) {
        // Make it transient
        FieldNode fieldNode = (FieldNode) n;
        fieldNode.addAnnotation(new AnnotationNode(new ClassNode(io.micronaut.data.annotation.Transient.class)));
      }
    }
  }

  /**
   * Adds a FieldHolderMapInterface field to the domain class to hold the Map that fronts the JSON text
   * holder field.  This mostly delegates to the ExtensibleFieldHelper class.
   * This is a transient field that is not persisted directly.
   *
   * @param fieldName  The name of the holder field.
   * @param classNode  The node for the class itself.
   * @param sourceUnit The source unit being processed.
   */
  private void addFieldHolderMap(String fieldName, ClassNode classNode, SourceUnit sourceUnit) {
    List<ASTNode> list = ASTUtils.addField(fieldName + "Map", BaseFieldHolderMap.class,
        Modifier.PUBLIC | Modifier.TRANSIENT, false, null, classNode, sourceUnit);
    for (ASTNode n : list) {
      if (n instanceof FieldNode) {
        // Make it transient
        FieldNode fieldNode = (FieldNode) n;
        fieldNode.addAnnotation(new AnnotationNode(new ClassNode(io.micronaut.data.annotation.Transient.class)));
        addJsonProperty((FieldNode) n, "_" + fieldName, sourceUnit);
      }
    }
    addMapGetter(fieldName, classNode, sourceUnit);
  }

  /**
   * Adds the Map getter to the annotated class.  This delegates to the ExtensibleFieldHelper.
   *
   * @param fieldName  The name of the holder field.
   * @param classNode  The class to add the method to.
   * @param sourceUnit The compiler source unit being processed (used to errors).
   */
  private void addMapGetter(String fieldName, ClassNode classNode, SourceUnit sourceUnit) {
    String getterName = "get" + ASTUtils.upperCaseFirstLetter(fieldName) + "Map";
    Parameter[] parameters = {};

    // Make sure the method doesn't exist already
    if (ASTUtils.methodExists(classNode, getterName, parameters)) {
      sourceUnit.getErrorCollector().addError(new SimpleMessage(getterName + "() already exists in " + classNode, sourceUnit));
      return;
    }

    ArgumentListExpression argumentListExpression = new ArgumentListExpression();
    argumentListExpression.addExpression(new VariableExpression("this"));

    PropertyExpression instanceExpression = new PropertyExpression(
        new ClassExpression(new ClassNode("org.simplemes.eframe.custom.ExtensibleFieldHelper", Modifier.PUBLIC, null)),
        "instance");

    MethodCallExpression method = new MethodCallExpression(
        instanceExpression,
        "getExtensibleFieldMap", argumentListExpression);
    BlockStatement methodBody = new BlockStatement(new Statement[]{new ExpressionStatement(method)}, new VariableScope());

    classNode.addMethod(getterName,
        Modifier.PUBLIC,
        ClassHelper.make(BaseFieldHolderMap.class),
        parameters,
        null, methodBody);
  }

  /**
   * Adds the fields hold Text getter to the annotated class.  This delegates to the ExtensibleFieldHelper.
   * This is used to intercept the normal getter when the text field value is needed (e.g. for DB save()).
   *
   * @param fieldName  The name of the holder field.
   * @param classNode  The class to add the method to.
   * @param sourceUnit The compiler source unit being processed (used to errors).
   */
  private void addFieldHolderGetter(String fieldName, ClassNode classNode, SourceUnit sourceUnit) {
    String getterName = "get" + ASTUtils.upperCaseFirstLetter(fieldName);
    Parameter[] parameters = {};

    // Make sure the method doesn't exist already
    if (ASTUtils.methodExists(classNode, getterName, parameters)) {
      sourceUnit.getErrorCollector().addError(new SimpleMessage(getterName + "() already exists in " + classNode, sourceUnit));
      return;
    }

    ArgumentListExpression argumentListExpression = new ArgumentListExpression();
    argumentListExpression.addExpression(new VariableExpression("this"));
    argumentListExpression.addExpression(new ConstantExpression(fieldName));

    PropertyExpression instanceExpression = new PropertyExpression(
        new ClassExpression(new ClassNode("org.simplemes.eframe.custom.ExtensibleFieldHelper", Modifier.PUBLIC, null)),
        "instance");

    MethodCallExpression method = new MethodCallExpression(
        instanceExpression,
        "getExtensibleFieldsText", argumentListExpression);
    BlockStatement methodBody = new BlockStatement(new Statement[]{new ExpressionStatement(method)}, new VariableScope());

    classNode.addMethod(getterName,
        Modifier.PUBLIC,
        ClassHelper.make(String.class),
        parameters,
        null, methodBody);
  }

  /**
   * Adds the fields hold Text setter to the annotated class.  This delegates to the ExtensibleFieldHelper.
   * This is used to intercept the normal setter when the text field value is set (e.g. for DB retrieves).
   *
   * @param fieldName  The name of the holder field.
   * @param classNode  The class to add the method to.
   * @param sourceUnit The compiler source unit being processed (used to errors).
   */
  private void addFieldHolderSetter(String fieldName, ClassNode classNode, SourceUnit sourceUnit) {
    String setterName = "set" + ASTUtils.upperCaseFirstLetter(fieldName);
    Parameter[] parameters = {
        new Parameter(new ClassNode(String.class), "value")
    };

    // Make sure the method doesn't exist already
    if (ASTUtils.methodExists(classNode, setterName, parameters)) {
      sourceUnit.getErrorCollector().addError(new SimpleMessage(setterName + "() already exists in " + classNode, sourceUnit));
      return;
    }

    ArgumentListExpression argumentListExpression = new ArgumentListExpression();
    argumentListExpression.addExpression(new VariableExpression("this"));
    argumentListExpression.addExpression(new ConstantExpression(fieldName));
    argumentListExpression.addExpression(new VariableExpression("value"));

    PropertyExpression instanceExpression = new PropertyExpression(
        new ClassExpression(new ClassNode("org.simplemes.eframe.custom.ExtensibleFieldHelper", Modifier.PUBLIC, null)),
        "instance");

    MethodCallExpression method = new MethodCallExpression(
        instanceExpression,
        "setExtensibleFieldsText", argumentListExpression);
    BlockStatement methodBody = new BlockStatement(new Statement[]{new ExpressionStatement(method)}, new VariableScope());

    classNode.addMethod(setterName,
        Modifier.PUBLIC,
        ClassHelper.VOID_TYPE,
        parameters,
        null, methodBody);
  }

  /**
   * Adds the a propertyMissingSetter to access the custom fields easily.  Delegates to the
   * ExtensibleFieldHelper.
   *
   * @param classNode  The class to add the method to.
   * @param sourceUnit The compiler source unit being processed (used to errors).
   */
  private void addPropertyMissingSetter(ClassNode classNode, SourceUnit sourceUnit) {
    String methodName = "propertyMissing";
    Parameter[] parameters = {
        new Parameter(new ClassNode(String.class), "name"),
        new Parameter(ClassHelper.OBJECT_TYPE, "value")
    };

    // Make sure the method doesn't exist already
    if (ASTUtils.methodExists(classNode, methodName, parameters)) {
      sourceUnit.getErrorCollector().addError(new SimpleMessage(methodName + "() already exists in " + classNode, sourceUnit));
    }

    ArgumentListExpression argumentListExpression = new ArgumentListExpression();
    argumentListExpression.addExpression(new VariableExpression("this"));
    argumentListExpression.addExpression(new VariableExpression("name"));
    argumentListExpression.addExpression(new VariableExpression("value"));

    PropertyExpression instanceExpression = new PropertyExpression(
        new ClassExpression(new ClassNode("org.simplemes.eframe.custom.ExtensibleFieldHelper", Modifier.PUBLIC, null)),
        "instance");

    MethodCallExpression method = new MethodCallExpression(
        instanceExpression,
        "propertyMissingSetter", argumentListExpression);
    BlockStatement methodBody = new BlockStatement(new Statement[]{new ExpressionStatement(method)}, new VariableScope());

    classNode.addMethod(methodName,
        Modifier.PUBLIC,
        ClassHelper.DYNAMIC_TYPE,
        parameters,
        null, methodBody);
  }

  /**
   * Adds the a propertyMissingGetter to access the custom fields easily.  Delegates to the
   * ExtensibleFieldHelper.
   *
   * @param classNode  The class to add the method to.
   * @param sourceUnit The compiler source unit being processed (used to errors).
   */
  private void addPropertyMissingGetter(ClassNode classNode, SourceUnit sourceUnit) {
    String methodName = "propertyMissing";
    Parameter[] parameters = {
        new Parameter(new ClassNode(String.class), "name"),
    };

    // Make sure the method doesn't exist already
    if (ASTUtils.methodExists(classNode, methodName, parameters)) {
      sourceUnit.getErrorCollector().addError(new SimpleMessage(methodName + "() already exists in " + classNode, sourceUnit));
    }

    ArgumentListExpression argumentListExpression = new ArgumentListExpression();
    argumentListExpression.addExpression(new VariableExpression("this"));
    argumentListExpression.addExpression(new VariableExpression("name"));

    PropertyExpression instanceExpression = new PropertyExpression(
        new ClassExpression(new ClassNode("org.simplemes.eframe.custom.ExtensibleFieldHelper", Modifier.PUBLIC, null)),
        "instance");

    MethodCallExpression method = new MethodCallExpression(
        instanceExpression,
        "propertyMissingGetter", argumentListExpression);
    //BlockStatement methodBody = new BlockStatement(new Statement[]{new ExpressionStatement(method)}, new VariableScope());
    ReturnStatement returnStatement = new ReturnStatement(new ExpressionStatement(method));

    classNode.addMethod(methodName,
        Modifier.PUBLIC,
        ClassHelper.DYNAMIC_TYPE,
        parameters,
        null, returnStatement);
  }
}
