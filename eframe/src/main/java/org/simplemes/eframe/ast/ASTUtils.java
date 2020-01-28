/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.ast;


import io.micronaut.core.util.StringUtils;
import org.codehaus.groovy.ast.*;
import org.codehaus.groovy.ast.expr.*;
import org.codehaus.groovy.ast.stmt.BlockStatement;
import org.codehaus.groovy.ast.stmt.ExpressionStatement;
import org.codehaus.groovy.ast.stmt.ReturnStatement;
import org.codehaus.groovy.ast.stmt.Statement;
import org.codehaus.groovy.classgen.Verifier;
import org.codehaus.groovy.control.SourceUnit;
import org.codehaus.groovy.control.messages.SimpleMessage;
import org.codehaus.groovy.syntax.SyntaxException;
import org.codehaus.groovy.syntax.Token;
import org.codehaus.groovy.syntax.Types;
import org.simplemes.eframe.domain.annotation.DomainEntityHelper;
import org.spockframework.runtime.model.FeatureMetadata;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

/**
 * Common utilities used by most AST (annotation) transformations.
 * Provides methods to add fields and methods to a class during compile time.
 */
@SuppressWarnings({"WeakerAccess", "UnusedReturnValue", "SameParameterValue"})
public class ASTUtils {
  /**
   * Adds the given field to class.  Compile will fail if it already exists.
   *
   * @param fieldName    The name of the field.
   * @param fieldType    The type of the field.
   * @param modifiers    The modifiers (e.g. public or static).  See Modifier.public.
   * @param maxSize      The max size of the field that will hold the custom values.  0 Means no constraint is added for this field.
   * @param initialValue The initial value expression.
   * @param node         The AST Class Node to add this field to.
   * @param sourceUnit   The compiler source unit being processed (used to errors).
   * @return A list containing the nodes created ([0] = fieldNode, [1] = getterMethod, [2] = settingMethod.
   */
  public static List<ASTNode> addField(String fieldName, Class fieldType, int modifiers, int maxSize,
                                       Expression initialValue, ClassNode node, SourceUnit sourceUnit) {
    return addField(fieldName, fieldType, modifiers, maxSize, true, initialValue, node, sourceUnit);
  }

  /**
   * Adds the given field to class.  Compile will fail if it already exists.
   *
   * @param fieldName          The name of the field.
   * @param fieldType          The type of the field.
   * @param modifiers          The modifiers (e.g. public or static).  See Modifier.public.
   * @param maxSize            The max size of the field that will hold the custom values.  0 Means no constraint is added for this field.
   * @param initialValue       The initial value expression.
   * @param addGetterAndSetter If true, then add the gett/setter.
   * @param node               The AST Class Node to add this field to.
   * @param sourceUnit         The compiler source unit being processed (used to errors).
   * @return A list containing the nodes created ([0] = fieldNode, [1] = getterMethod, [2] = settingMethod.
   */
  public static List<ASTNode> addField(String fieldName, Class fieldType, int modifiers, int maxSize, boolean addGetterAndSetter,
                                       Expression initialValue, ClassNode node, SourceUnit sourceUnit) {
    //System.out.println("Adding " + fieldName+", maxSize="+maxSize);
    List<ASTNode> res = new ArrayList<>();
    if (!hasField(node, fieldName)) {
      //System.out.println("adding fieldName = " + fieldName);
      // Only set if not defined.  Assume the user defined it correctly.
      ClassNode fieldTypeNode = new ClassNode(fieldType);
      FieldNode fieldNode = new FieldNode(fieldName, modifiers, fieldTypeNode, new ClassNode(node.getClass()), initialValue);
      node.addField(fieldNode);
      if (maxSize > 0) {
        addConstraint(node, fieldName, true, maxSize, sourceUnit);
      }
      res.add(fieldNode);
      if (addGetterAndSetter) {
        int methodModifier = (modifiers & Modifier.STATIC);
        MethodNode getter = addGetter(fieldNode, node, Modifier.PUBLIC | methodModifier, sourceUnit);
        MethodNode setter = addSetter(fieldNode, node, Modifier.PUBLIC | methodModifier, sourceUnit);
        res.add(getter);
        res.add(setter);
      }
    } else {
      // Already exists, so fail with a compile error.
      sourceUnit.getErrorCollector().addError(new SimpleMessage(fieldName + " already exists in " + node, sourceUnit));
    }
    return res;
  }

  /**
   * Checks for the existence of the given field.  If the field exists in a parent class, then this
   * method returns false.
   *
   * @param classNode The class to check.
   * @param fieldName The field to check.
   * @return True if the class already as the field.
   */
  static boolean hasField(ClassNode classNode, String fieldName) {
    FieldNode f = classNode.getField(fieldName);
    if (f == null) {
      return false;
    }
    //System.out.println(fieldName+" f:" + f.getOwner()+(f.getOwner().equals(classNode)));

    return f.getOwner().equals(classNode);
  }

  /**
   * Adds a setter method for the given field.
   *
   * @param fieldNode  The field to add the setter for.
   * @param classNode  The owning (class) AST node.
   * @param modifier   The modifiers (e.g. Modifier.PUBLIC)
   * @param sourceUnit The source the compiler used. (For error messages).
   * @return The MethodNode added.
   */
  static MethodNode addSetter(FieldNode fieldNode, ClassNode classNode, int modifier, SourceUnit sourceUnit) {
    // Make sure the method doesn't exist already
    ClassNode type = fieldNode.getType();
    String setterName = "set" + Verifier.capitalize(fieldNode.getName());
    Parameter[] parameters = {new Parameter(extractTypeFromGeneric(type), "value")};
    if (ASTUtils.methodExists(classNode, setterName, parameters)) {
      sourceUnit.getErrorCollector().addError(new SimpleMessage(setterName + "() already exists in " + classNode, sourceUnit));
    }
    return classNode.addMethod(setterName,
        modifier,
        ClassHelper.VOID_TYPE,
        parameters,
        null,
        new ExpressionStatement(
            new BinaryExpression(
                new FieldExpression(fieldNode),
                Token.newSymbol(Types.EQUAL, -1, -1),
                new VariableExpression("value"))));
  }

  /**
   * Adds a getter() method for the given field to the given class.
   *
   * @param fieldNode  The node in the AST structure.
   * @param classNode  The owning node (the class the method is added to).
   * @param modifier   The modifiers (e.g. Modifier.PUBLIC)
   * @param sourceUnit The source the compiler used. (For error messages).
   * @return The MethodNode added.
   */
  static MethodNode addGetter(FieldNode fieldNode, ClassNode classNode, int modifier, SourceUnit sourceUnit) {
    String getterName = "get" + StringUtils.capitalize(fieldNode.getName());
    // Make sure the method doesn't exist already
    if (ASTUtils.methodExists(classNode, getterName, Parameter.EMPTY_ARRAY)) {
      sourceUnit.getErrorCollector().addError(new SimpleMessage(getterName + "() already exists in " + classNode, sourceUnit));
    }
    ClassNode type = fieldNode.getType();
    MethodNode methodNode = new MethodNode(getterName,
        modifier,
        extractTypeFromGeneric(type),
        Parameter.EMPTY_ARRAY,
        null,
        new ReturnStatement(new FieldExpression(fieldNode)));
    classNode.addMethod(methodNode);
    return methodNode;
  }


  /**
   * Adds a Grails constraint that allows null values to the given class.
   *
   * @param classNode  The AST Node for the given class.
   * @param fieldName  The name of the field that allows nulls.
   * @param nullable   True if the field allows null.
   * @param maxSize    The maxSize of the value field.
   * @param sourceUnit The compiler source unit being processed (used to errors).
   */
  @SuppressWarnings({"UnusedParameters", "SameParameterValue"})
  static void addConstraint(ClassNode classNode, String fieldName, boolean nullable, int maxSize, SourceUnit sourceUnit) {
    FieldNode closure = classNode.getDeclaredField("constraints");
    if (closure == null) {
      // Create the constraints closure (empty) if not defined for the class.
      ClosureExpression expr = new ClosureExpression(Parameter.EMPTY_ARRAY, new BlockStatement());
      expr.setVariableScope(new VariableScope());
      classNode.addField("constraints", Modifier.STATIC | Modifier.PUBLIC, ClassHelper.OBJECT_TYPE, expr);
      closure = classNode.getDeclaredField("constraints");
      //System.out.println("Added constraints closure = " + closure);
    }
    if (closure != null) {
      ClosureExpression exp = (ClosureExpression) closure.getInitialExpression();
      BlockStatement block = (BlockStatement) exp.getCode();

      if (!isFieldInClosure(closure, fieldName)) {
        NamedArgumentListExpression argumentListExpression = new NamedArgumentListExpression();
        argumentListExpression.addMapEntryExpression(new ConstantExpression("nullable"), new ConstantExpression(nullable));
        argumentListExpression.addMapEntryExpression(new ConstantExpression("blank"), new ConstantExpression(true));
        argumentListExpression.addMapEntryExpression(new ConstantExpression("maxSize"), new ConstantExpression(maxSize));

        // Create a validator:
        // def validator = { val ->
        //    FieldExtensionHelper.validate(val)
        // }
        VariableExpression val = new VariableExpression("val");
        VariableExpression domainObject = new VariableExpression("domainObject");
        List<Expression> argumentList = new ArrayList<>();
        argumentList.add(val);
        argumentList.add(domainObject);
/*
        StaticMethodCallExpression closureMethodCall = new StaticMethodCallExpression(new ClassNode(FieldExtensionHelper.class),
            "validateFieldExtensionsForSave",
            new ArgumentListExpression(argumentList));
*/
        MethodCallExpression closureMethodCall = new MethodCallExpression(domainObject,
            "validateAllCustomFields",
            new ArgumentListExpression(argumentList));
        BlockStatement closureBody = new BlockStatement(new Statement[]{new ReturnStatement(closureMethodCall)},
            new VariableScope());
        Parameter[] closureParameters = {new Parameter(new ClassNode(Object.class), "val"),
            new Parameter(new ClassNode(Object.class), "domainObject")};

        VariableScope scope = new VariableScope();
        scope.putDeclaredVariable(val);
        ClosureExpression validator = new ClosureExpression(closureParameters, closureBody);
        validator.setVariableScope(scope);
        argumentListExpression.addMapEntryExpression(new ConstantExpression("validator"), validator);

        MethodCallExpression constExpr = new MethodCallExpression(VariableExpression.THIS_EXPRESSION,
            new ConstantExpression(fieldName),
            argumentListExpression);
        block.addStatement(new ExpressionStatement(constExpr));
      }
    }
  }

  /**
   * Adds a field to the standard Grails transients lists.
   *
   * @param classNode  The AST Node for the given class.
   * @param fieldName  The name of the field to add.
   * @param sourceUnit The compiler source unit being processed (used to errors).
   */
  @SuppressWarnings({"UnusedParameters", "SameParameterValue"})
  public static void addFieldToTransients(String fieldName, ClassNode classNode, SourceUnit sourceUnit) {
    FieldNode transientsNode = classNode.getDeclaredField("transients");
    ListExpression list = new ListExpression();
    if (transientsNode == null) {
      // Create the transients array (empty) if not defined for the class.
      classNode.addField("transients", Modifier.STATIC | Modifier.PUBLIC, ClassHelper.OBJECT_TYPE, list);
      //transientsNode = classNode.getDeclaredField("transients");
    } else {
      list = (ListExpression) transientsNode.getInitialExpression();
    }

    list.addExpression(new ConstantExpression(fieldName));
  }

  /**
   * Determines if the field is in the given closure (e.g. a constraints closure).
   *
   * @param closure   The closure.
   * @param fieldName The field to check.
   * @return true if the field is in the closure.
   */
  static boolean isFieldInClosure(FieldNode closure, String fieldName) {
    if (closure != null) {
      ClosureExpression exp = (ClosureExpression) closure.getInitialExpression();
      BlockStatement block = (BlockStatement) exp.getCode();
      List<Statement> statements = block.getStatements();
      for (Statement statement : statements) {
        if (statement instanceof ExpressionStatement && ((ExpressionStatement) statement).getExpression() instanceof MethodCallExpression) {
          MethodCallExpression methodCallExpression = (MethodCallExpression) ((ExpressionStatement) statement).getExpression();
          ConstantExpression constantExpression = (ConstantExpression) methodCallExpression.getMethod();
          if (constantExpression.getValue().equals(fieldName)) {
            return true;
          }
        }
      }
    }
    return false;
  }

  /**
   * If the given AST class node is a generic, then extract the real, underlying class node.
   * Converts List&lt;String&gt; to String
   *
   * @param type The type.
   * @return The type or underlying type if a generic.
   */
  static ClassNode extractTypeFromGeneric(ClassNode type) {
    if (type.isUsingGenerics()) {
      final ClassNode nonGen = ClassHelper.makeWithoutCaching(type.getName());
      nonGen.setRedirect(type);
      nonGen.setGenericsTypes(null);
      nonGen.setUsingGenerics(false);
      return nonGen;
    } else {
      return type;
    }
  }

  /**
   * Determines if the given method already exists in the class node.
   *
   * @param classNode  The class to check for the method.
   * @param getterName The method name.
   * @param parameters The method parameters.
   * @return True if the method exists.
   */
  public static boolean methodExists(ClassNode classNode, String getterName, Parameter[] parameters) {
    List<MethodNode> methodNodes = classNode.getMethods(getterName);
    for (MethodNode methodNode : methodNodes) {
      // Compare the types of the parameters
      Parameter[] methodParameters = methodNode.getParameters();
      if (parameters.length == methodParameters.length) {
        boolean match = true;
        for (int i = 0; i < parameters.length; i++) {
          if (parameters[i].getType().getTypeClass() != methodParameters[i].getType().getTypeClass()) {
            match = false;
          }
        }
        if (match) {
          return true;
        }
      }
    }
    return false;
  }

  /**
   * Wraps the given method with a closure that is called within a transaction.  Supports forced rollback
   * option
   *
   * @param forceRollback If true, then adds a setRollbackOnly() call in the method.
   * @param method        The method to wrap.
   * @param sourceUnit    The source.
   */
  public static void wrapWithTransaction(boolean forceRollback, MethodNode method, SourceUnit sourceUnit) {

    /*
    Wraps the code with this logic:
      DomainEntityHelper.instance.getTransactionManager().executeWrite {_$status ->
        _$status.setRollbackOnly()

        // Original Method
      }

     */

    // Add the setRollbackOnly as the first statement in the closure.
    BlockStatement originalBlock = (BlockStatement) method.getCode();
    BlockStatement closureBlock = new BlockStatement();

    // Make sure this is not used in a method with a where clause.
    for (AnnotationNode ann : method.getAnnotations(new ClassNode(FeatureMetadata.class))) {
      if (ann.getMembers().get("parameterNames") != null) {
        ListExpression parameterNames = (ListExpression) ann.getMembers().get("parameterNames");
        if (parameterNames.getExpressions().size() > 0) {
          ConstantExpression nameExpr = (ConstantExpression) ann.getMembers().get("name");
          String methodName = nameExpr.getText();
          String message = "@Rollback does not support Spock 'where:' features.  Method '" + methodName + "'.";
          sourceUnit.addError(new SyntaxException(message, method));
        }
      }
      ListExpression blocks = (ListExpression) ann.getMembers().get("blocks");
      for (Object o : blocks.getExpressions()) {
        AnnotationConstantExpression ace = (AnnotationConstantExpression) o;
      }
    }

    if (forceRollback) {
      MethodCallExpression setRollbackMethod = new MethodCallExpression(new VariableExpression("status"),
          "setRollbackOnly", new ArgumentListExpression(Parameter.EMPTY_ARRAY));
      closureBlock.addStatement(new ExpressionStatement(setRollbackMethod));
    }

    closureBlock.addStatements(originalBlock.getStatements());

    ClosureExpression body = new ClosureExpression(
        new Parameter[]{new Parameter(ClassHelper.OBJECT_TYPE, "status")},
        closureBlock);

    VariableScope scope = new VariableScope(method.getVariableScope());
    body.setVariableScope(scope);

    MethodCallExpression executeMethod = new MethodCallExpression(
        new PropertyExpression(new ClassExpression(new ClassNode(DomainEntityHelper.class)), "instance"),
        "executeWrite",
        new ArgumentListExpression(body));

    BlockStatement block = new BlockStatement();
    block.addStatement(new ExpressionStatement(executeMethod));
    method.setCode(block);
  }


}
