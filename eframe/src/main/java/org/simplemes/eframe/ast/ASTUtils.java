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
import org.codehaus.groovy.classgen.Verifier;
import org.codehaus.groovy.control.SourceUnit;
import org.codehaus.groovy.control.messages.SimpleMessage;
import org.codehaus.groovy.syntax.SyntaxException;
import org.codehaus.groovy.syntax.Token;
import org.codehaus.groovy.syntax.Types;
import org.simplemes.eframe.domain.annotation.DomainEntityHelper;
import org.simplemes.eframe.domain.annotation.DomainEntityInterface;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spockframework.runtime.model.FeatureMetadata;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Common utilities used by most AST (annotation) transformations.
 * Provides methods to add fields and methods to a class during compile time.
 */
@SuppressWarnings({"WeakerAccess", "UnusedReturnValue", "SameParameterValue"})
public class ASTUtils {

  /**
   * The logger.
   */
  private static final Logger log = LoggerFactory.getLogger(ASTUtils.class);

  /**
   * A list of classes to handles specially in the invokeGroovyMethod().  Any sub-classes of these
   * will be treated as the parent class when finding the groovy method.
   */
  private static final List<Class> parentClassesForInvoke = Arrays.asList(new Class[]{DomainEntityInterface.class, ResultSet.class});

  /**
   * Adds the given field to class.  Compile will fail if it already exists.
   *
   * @param fieldName    The name of the field.
   * @param fieldType    The type of the field.
   * @param modifiers    The modifiers (e.g. public or static).  See Modifier.public.
   * @param initialValue The initial value expression.
   * @param node         The AST Class Node to add this field to.
   * @param sourceUnit   The compiler source unit being processed (used to errors).
   * @return A list containing the nodes created ([0] = fieldNode, [1] = getterMethod, [2] = settingMethod.
   */
  public static List<ASTNode> addField(String fieldName, Class fieldType, int modifiers,
                                       Expression initialValue, ClassNode node, SourceUnit sourceUnit) {
    return addField(fieldName, fieldType, modifiers, true, initialValue, node, sourceUnit);
  }

  /**
   * Adds the given field to class.  Compile will fail if it already exists.
   *
   * @param fieldName          The name of the field.
   * @param fieldType          The type of the field.
   * @param modifiers          The modifiers (e.g. public or static).  See Modifier.public.
   * @param addGetterAndSetter If true, then add the gett/setter.
   * @param initialValue       The initial value expression.
   * @param node               The AST Class Node to add this field to.
   * @param sourceUnit         The compiler source unit being processed (used to errors).
   * @return A list containing the nodes created ([0] = fieldNode, [1] = getterMethod, [2] = settingMethod.
   */
  public static List<ASTNode> addField(String fieldName, Class fieldType, int modifiers, boolean addGetterAndSetter,
                                       Expression initialValue, ClassNode node, SourceUnit sourceUnit) {
    //System.out.println("Adding " + fieldName+", maxSize="+maxSize);
    List<ASTNode> res = new ArrayList<>();
    if (!hasField(node, fieldName)) {
      //System.out.println("adding fieldName = " + fieldName);
      // Only set if not defined.  Assume the user defined it correctly.
      ClassNode fieldTypeNode = new ClassNode(fieldType);
      FieldNode fieldNode = new FieldNode(fieldName, modifiers, fieldTypeNode, new ClassNode(node.getClass()), initialValue);
      node.addField(fieldNode);
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
  public static MethodNode addGetter(FieldNode fieldNode, ClassNode classNode, int modifier, SourceUnit sourceUnit) {
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
      //ListExpression blocks = (ListExpression) ann.getMembers().get("blocks");
      //for (Object o : blocks.getExpressions()) {
      //AnnotationConstantExpression ace = (AnnotationConstantExpression) o;
      //}
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


  /**
   * Shifts the first letter of the string to uppercase.
   *
   * @param s The string to adjust.
   * @return The adjusted string.
   */
  public static String upperCaseFirstLetter(String s) {
    if (s == null || s.length() == 0) {
      return null;
    }
    String first = s.substring(0, 1).toUpperCase();

    return first + s.substring(1);
  }

  /**
   * Invokes the given groovy class/method with arguments.  This is done to access
   * the groovy world from the Java code.  This is needed since the Java source tree is compiled before
   * the groovy code is compiled.
   * <p>We do this to avoid moving the Java source to a separate module.
   * <p><b>Note</b> This will return null if the method is not found or other invocation errors.  The error will be logged
   * as a warning.
   *
   * @param className  The fully qualified class name.  If the method ends in .instance, then the getIstance() method
   *                   will be called to find the actual object to invoke the method on.
   * @param methodName The method name.
   * @param args       The arguments.
   * @return The results of the method call.  Null if the class/method is not found.
   */
  @SuppressWarnings("unchecked")
  public static Object invokeGroovyMethod(String className, String methodName, Object... args) {
    try {
      boolean callGetInstance = false;
      if (className.endsWith(".instance")) {
        className = className.replace(".instance", "");
        callGetInstance = true;
      }
      Class[] paramTypes = new Class[args.length];
      for (int i = 0; i < args.length; i++) {
        if (args[i] != null) {
          paramTypes[i] = args[i].getClass();
          // Use the parent class for some common cases in order to find the right method.
          for (Class clazz : parentClassesForInvoke) {
            if (clazz.isAssignableFrom(args[i].getClass())) {
              paramTypes[i] = clazz;
            }
          }
        } else {
          // Unknown type, so try Object
          paramTypes[i] = Object.class;
        }
      }
      Class holdersClass = Class.forName(className);

      Object instance = null;
      if (callGetInstance) {
        Method getterMethod = ((Class<?>) holdersClass).getMethod("getInstance");
        instance = getterMethod.invoke(null);
      }

      Method method = ((Class<?>) holdersClass).getMethod(methodName, paramTypes);
      return method.invoke(instance, args);
    } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException e) {
      log.warn("Error invoking method " + className + "." + methodName + "(). ", e);
      return null;
    } catch (InvocationTargetException e) {
      // We need to wrap the original exception in a runtime exception to avoid adding InvocationTargetException
      // to every method in this class.
      throw new RuntimeException(e.getCause());
    }
  }

  /**
   * Finds the method from the given class, with the given arguments.  Checks for sub-class/implementers of
   * the method's parameters.
   *
   * @param clazz          The class to search for the method.
   * @param methodName     The method name.
   * @param parameterTypes The parameter classes for the method.  Null not allowed.
   * @return The method or an exception is thrown.
   */
  public static Method findMethod(Class clazz, String methodName, Class[] parameterTypes) throws NoSuchMethodException {
    Method[] methods = clazz.getMethods();
    for (Method method : methods) {
      if (!methodName.equals(method.getName())) {
        continue;
      }
      Class<?>[] methodParameterTypes = method.getParameterTypes();
      if (methodParameterTypes.length != parameterTypes.length) {
        continue;
      }

      boolean matches = true;
      for (int i = 0; i < parameterTypes.length; ++i) {
        if (!methodParameterTypes[i].isAssignableFrom(parameterTypes[i])) {
          matches = false;
        }
      }
      if (matches) {
        return method;
      }
    }
    //com.sun.proxy.$Proxy19.postCoreMethod(java.util.LinkedHashMap, java.lang.String, java.lang.Integer)
    StringBuilder sb = new StringBuilder();
    for (Class parameterType : parameterTypes) {
      if (sb.length() > 0) {
        sb.append(", ");
      }
      sb.append(parameterType.getName());
    }

    throw new NoSuchMethodException(clazz.getName() + "." + methodName + "(" + sb + ")");
  }
}
