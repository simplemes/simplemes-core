package org.simplemes.eframe.data.annotation;

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

import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Map;


/**
 * This class provides the compile-time AST transformation that adds an extensible field to the given
 * domain class.  This injects the custom field holder as a persistent field with a given maxSize.  Also
 * injects a static (non-persistent) field that holds the custom field definitions for the runtime validation.
 */
@SuppressWarnings("DefaultAnnotationParam")
@GroovyASTTransformation(phase = CompilePhase.CANONICALIZATION)
public class ExtensibleFieldsTransformation implements ASTTransformation {

  /*
   * The default field name for the static definition holder variable in the domain class.  Not configurable.
   */
  //public static final String DEFAULT_DEFINITION_HOLDER_FIELD_NAME = "_customFieldDef";

  /**
   * Basic Constructor.
   */
  public ExtensibleFieldsTransformation() {
  }

  public void visit(ASTNode[] astNodes, SourceUnit sourceUnit) {
    //System.out.println("visit");
    if (sourceUnit.getAST() == null) {
      return;
    }
    // find all methods annotated with @WithLogging
    ClassNode ourAnnotationNode = new ClassNode(ExtensibleFields.class);
    for (ASTNode astNode : astNodes) {
      if (astNode instanceof ClassNode) {
        //System.out.println("Checking astNode = " + astNode);
        ClassNode classNode = (ClassNode) astNode;
        List<AnnotationNode> annotations = classNode.getAnnotations(ourAnnotationNode);
        if (annotations.size() > 0) {
          AnnotationNode annotation = annotations.get(0);
          addCustomFieldHolder(classNode, annotation, sourceUnit);
          //addStaticCustomFieldDefinitionHolder(classNode, sourceUnit);
          addComplexFieldHolder(classNode, sourceUnit);
          addEmptyValidateAllCustomFields(classNode);
          addValueSetter(classNode, sourceUnit);
          addValueGetter(classNode, sourceUnit);
          addConfigurableTypeAccessors(classNode, sourceUnit);
        }
      }
    }
  }

  /**
   * Adds the custom field holder to the domain class.
   *
   * @param node       The node for the class itself.
   * @param annotation The annotation that defines the customer field name/size.
   * @param sourceUnit The source unit being processed.
   */
  private void addCustomFieldHolder(ClassNode node, AnnotationNode annotation, SourceUnit sourceUnit) {
    // Check the annotation to make sure we have the field the developer intended.
    String fieldName = ExtensibleFields.DEFAULT_FIELD_NAME;
    ConstantExpression fieldExpression = (ConstantExpression) annotation.getMember("fieldName");
    if (fieldExpression != null) {
      fieldName = fieldExpression.getValue().toString();
    }
    // Figure out the maxSize of the added column.
    int maxSize = ExtensibleFields.DEFAULT_MAX_SIZE;
    fieldExpression = (ConstantExpression) annotation.getMember("maxSize");
    if (fieldExpression != null) {
      maxSize = (Integer) fieldExpression.getValue();
    }

    if (fieldName != null) {
      List<ASTNode> nodes = ASTUtils.addField(fieldName, String.class, Modifier.PRIVATE, maxSize, null, node, sourceUnit);
      // Add the Jackson annotation to rename the field.  This will prevent overlaying the holder value during imports.
      AnnotationNode annotationNode = new AnnotationNode(new ClassNode(JsonProperty.class));
      annotationNode.addMember("value", new ConstantExpression("_" + fieldName));
      MethodNode getterNode = (MethodNode) nodes.get(1);
      getterNode.addAnnotation(annotationNode);
    }
    /*


     */
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
  /*
   * Adds static field to the domain class to hold the definition of the custom field extensions.
   * This is cached in the domain class to avoid DB lookup at runtime.  This mainly holds the
   * field type and max length.
   *
   * @param node       The node for the class itself.
   * @param sourceUnit The source unit being processed.
   */
/*
  private void addStaticCustomFieldDefinitionHolder(ClassNode node, SourceUnit sourceUnit) {
    // The definition list is just a normal list.  It will be set by FieldExtensionHelper.setupFieldExtensionsInDomains().
    ASTUtils.addField(DEFAULT_DEFINITION_HOLDER_FIELD_NAME, Object.class, Modifier.PUBLIC | Modifier.STATIC, 0, null, node, sourceUnit);
  }
*/

  /**
   * Adds a Map field to the domain class to hold the complex custom field values such
   * as lists of references.  This is a transient field so that persistence is handled by
   * non-Grails logic.  Initializes the field as an empty map.
   *
   * @param node       The node for the class itself.
   * @param sourceUnit The source unit being processed.
   */
  private void addComplexFieldHolder(ClassNode node, SourceUnit sourceUnit) {
    Expression init = new MapExpression();
    ASTUtils.addField(ExtensibleFields.COMPLEX_CUSTOM_FIELD_NAME, Map.class, Modifier.PUBLIC | Modifier.TRANSIENT, 0, init, node, sourceUnit);
    ASTUtils.addFieldToTransients(ExtensibleFields.COMPLEX_CUSTOM_FIELD_NAME, node, sourceUnit);
  }

  /**
   * Adds an empty validateAllCustomFields() method on the given class to avoid runtime errors if
   * there are custom fields defined (mainly in unit tests since the setupFields method is not called there).
   *
   * @param classNode The class the method is added to.
   */
  private void addEmptyValidateAllCustomFields(ClassNode classNode) {
    Parameter[] methodParameters = {new Parameter(new ClassNode(Object.class), "val"),
        new Parameter(new ClassNode(Object.class), "domainObject")};
    classNode.addMethod("validateAllCustomFields",
        Modifier.PUBLIC,
        ClassHelper.OBJECT_TYPE,
        methodParameters,
        null,
        new ReturnStatement(new ConstantExpression(true)));
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
