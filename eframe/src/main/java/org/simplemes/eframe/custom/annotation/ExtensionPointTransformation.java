/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.custom.annotation;

import org.codehaus.groovy.ast.*;
import org.codehaus.groovy.ast.expr.*;
import org.codehaus.groovy.ast.stmt.*;
import org.codehaus.groovy.control.CompilePhase;
import org.codehaus.groovy.control.SourceUnit;
import org.codehaus.groovy.control.messages.SimpleMessage;
import org.codehaus.groovy.syntax.Token;
import org.codehaus.groovy.syntax.Types;
import org.codehaus.groovy.transform.ASTTransformation;
import org.codehaus.groovy.transform.GroovyASTTransformation;
import org.simplemes.eframe.ast.ASTUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


/**
 * This annotation allows the application developer to mark a method as an extension point.  This
 * allows other modules to add logic to core methods without changing the methods themselves.
 * The core developer marks the method as an ExtensionPoint, then the module developer
 * will create a bean that provides pre/post methods that are executed when the method is called.
 * <p>
 * This annotation adds code before the method body and before the return statement(s) to invoked
 * the added extension (custom) code.
 * <p>
 * An optional comment can be used in generation of ASCII Doctor file that contains all
 * of the current module's extension points.
 */
@SuppressWarnings({"DefaultAnnotationParam"})
@GroovyASTTransformation(phase = CompilePhase.CANONICALIZATION)
public class ExtensionPointTransformation implements ASTTransformation {

  /**
   * Basic Constructor.
   */
  @SuppressWarnings("unused")
  public ExtensionPointTransformation() {
  }

  public void visit(ASTNode[] astNodes, SourceUnit sourceUnit) {
    //System.out.println("visit");
    if (sourceUnit.getAST() == null) {
      return;
    }
    ClassNode ourAnnotationNode = new ClassNode(ExtensionPoint.class);
    for (ASTNode astNode : astNodes) {
      if (astNode instanceof MethodNode) {
        //System.out.println("Checking astNode2 = " + astNode);
        MethodNode methodNode = (MethodNode) astNode;
        List<AnnotationNode> annotations = methodNode.getAnnotations(ourAnnotationNode);
        if (annotations.size() > 0) {
          //System.out.println("found");
          transformMethod(methodNode, annotations.get(0), sourceUnit);
        }
      }
    }
  }

  /**
   * Transform the given method with added features for the @ExtensionPoint annotation.
   *
   * @param methodNode The method to transform.
   * @param annotation The annotation node on the method.
   * @param sourceUnit The source the class came from.
   */
  private void transformMethod(MethodNode methodNode, AnnotationNode annotation, SourceUnit sourceUnit) {
    if (!validate(methodNode, annotation, sourceUnit)) {
      return;
    }

    addInvokePre(methodNode, annotation, sourceUnit);
    int count = wrapReturnsRecursive(methodNode.getCode(), methodNode, annotation, sourceUnit);

    if (count == 0) {
      SimpleMessage message = new SimpleMessage("No 'return' statements found in @ExtensionPoint method " + methodNode.getName(), sourceUnit);
      sourceUnit.getErrorCollector().addError(message);
    }

  }

  /**
   * Validates that the annotation is correct for the given method.
   *
   * @param methodNode The method to transform.
   * @param annotation The annotation node on the method.
   * @param sourceUnit The source the class came from.
   * @return False if the validation failed.  Messages are written by the compiler.
   */
  private boolean validate(MethodNode methodNode, AnnotationNode annotation, SourceUnit sourceUnit) {
    Expression valueExpression = annotation.getMember("value");
    if (valueExpression == null) {
      SimpleMessage message = new SimpleMessage("No Interface in @ExtensionPoint annotation for method " + methodNode.getName(), sourceUnit);
      sourceUnit.getErrorCollector().addError(message);
      return false;
    }

    ClassNode interfaceType = valueExpression.getType();
    String preMethodName = "pre" + ASTUtils.upperCaseFirstLetter(methodNode.getName());
    String postMethodName = "post" + ASTUtils.upperCaseFirstLetter(methodNode.getName());
    MethodNode preMethodNode = null;
    MethodNode postMethodNode = null;

    // Check the interface methods with the right parameters.
    for (MethodNode interfaceMethod : interfaceType.getMethods()) {
      if (interfaceMethod.getName().equals(preMethodName)) {
        preMethodNode = interfaceMethod;
      }
      if (interfaceMethod.getName().equals(postMethodName)) {
        postMethodNode = interfaceMethod;
      }
    }
    if (preMethodNode == null) {
      SimpleMessage message = new SimpleMessage("Interface '" + interfaceType.getName() + "' does not have the expected method '" +
          preMethodName + "' the @ExtensionPoint on method " + methodNode.getName(), sourceUnit);
      sourceUnit.getErrorCollector().addError(message);
      return false;
    }
    if (postMethodNode == null) {
      SimpleMessage message = new SimpleMessage("Interface '" + interfaceType.getName() + "' does not have the expected method '" +
          postMethodName + "' the @ExtensionPoint on method " + methodNode.getName(), sourceUnit);
      sourceUnit.getErrorCollector().addError(message);
      return false;
    }

    // Check the pre method arguments.
    if (!validateParameters(Arrays.asList(preMethodNode.getParameters()), Arrays.asList(methodNode.getParameters()), interfaceType,
        preMethodName, methodNode.getName(), sourceUnit)) {
      return false;
    }
    // Check the post method arguments, with the return value added before the real arguments.
    //List<Parameter> adjustedParametersForPost = Arrays.asList(methodNode.getParameters());
    List<Parameter> adjustedParametersForPost = new ArrayList<>();
    adjustedParametersForPost.add(new Parameter(methodNode.getReturnType(), "returnValue"));
    adjustedParametersForPost.addAll(Arrays.asList(methodNode.getParameters()));
    if (!validateParameters(Arrays.asList(postMethodNode.getParameters()), adjustedParametersForPost, interfaceType,
        postMethodName, methodNode.getName(), sourceUnit)) {
      return false;
    }

    // Now, check the return types for the post method from the interface vs. the core method.
    String postMethodReturnType = postMethodNode.getReturnType().getName();
    String methodReturnType = methodNode.getReturnType().getName();
    if (!postMethodReturnType.equals(methodReturnType)) {
      SimpleMessage message = new SimpleMessage("Interface '" + interfaceType.getName() + "' post method '" + postMethodName +
          "' does return the correct type " +
          " for the @ExtensionPoint on method " + methodNode.getName() + ". Found: " + postMethodReturnType + ", expected: " +
          methodReturnType, sourceUnit);
      sourceUnit.getErrorCollector().addError(message);
      return false;

    }

    return true;
  }

  /**
   * Compare two list of parameters to make sure they are the same.
   *
   * @param parameters1   The first set of params.
   * @param parameters2   The second set of params.
   * @param interfaceType The interface the parameters come from.
   * @param methodName1   The first method name (for error messages).
   * @param methodName2   The second method name (for error messages).
   * @param sourceUnit    The source.
   * @return True if validation passes.
   */
  @SuppressWarnings("BooleanMethodIsAlwaysInverted")
  private boolean validateParameters(List<Parameter> parameters1, List<Parameter> parameters2, ClassNode interfaceType,
                                     String methodName1, String methodName2, SourceUnit sourceUnit) {
    if (parameters1.size() != parameters2.size()) {
      //System.out.println(parameters1.size() +" vs. "+parameters2.size());
      SimpleMessage message = new SimpleMessage("Interface '" + interfaceType.getName() + "' method '" + methodName1 +
          "' has different parameters than the @ExtensionPoint method " + methodName2, sourceUnit);
      sourceUnit.getErrorCollector().addError(message);
      return false;
    }

    // Now, compare the types in each list
    for (int i = 0; i < parameters1.size(); i++) {
      String className1 = parameters1.get(i).getType().getName();
      String className2 = parameters2.get(i).getType().getName();
      if (!className1.equals(className2)) {
        String names = parameters2.get(i).getName() + "/" + parameters1.get(i).getName();
        SimpleMessage message = new SimpleMessage("Interface '" + interfaceType.getName() + "' method '" + methodName1 +
            "' has different parameters than the @ExtensionPoint method " + methodName2 +
            ".  Parameter class mismatch on " + names + ": " + className1 + "" + " vs. " + className2 + "",
            sourceUnit);
        sourceUnit.getErrorCollector().addError(message);
        return false;
      }
    }

    return true;
  }

  /**
   * Transform the given method with a ExtensionPointHelper.invokePost() call.
   *
   * @param methodNode The method to transform.
   * @param sourceUnit The source the class came from.
   */
  private void addInvokePre(MethodNode methodNode, AnnotationNode annotation, SourceUnit sourceUnit) {
    Expression targetEntityExpression = annotation.getMember("value");
    ClassNode interfaceType = targetEntityExpression.getType();

    BlockStatement blockStatement = (BlockStatement) methodNode.getCode();
    List<Statement> statements = blockStatement.getStatements();

    // Insert pre call.
    // ExtensionPointHelper.instance.invokePre(SampleExtensionInterface, 'preCoreMethod', argument1)
    String customMethodName = "pre" + ASTUtils.upperCaseFirstLetter(methodNode.getName());
    List<Expression> argumentList = new ArrayList<>();
    argumentList.add(new ClassExpression(interfaceType));
    argumentList.add(new ConstantExpression(customMethodName));
    for (Parameter parameter : methodNode.getParameters()) {
      argumentList.add(new VariableExpression(parameter));
    }

    MethodCallExpression methodCall = new MethodCallExpression(
        new PropertyExpression(new ClassExpression(new ClassNode(ExtensionPointHelper.class)), "instance"),
        "invokePre",
        new ArgumentListExpression(argumentList));
    BlockStatement invokeStatement = new BlockStatement();
    invokeStatement.addStatement(new ExpressionStatement(methodCall));
    statements.add(0, invokeStatement);
  }


  /**
   * Adds the call to the ExtensionPointHelper.invokePost() method for each return statement.
   * The statements are searched recursively for all return statements.
   *
   * @param statement  The statement to check.
   * @param annotation The annotation instance.
   * @param sourceUnit The source the class came from.
   * @return The number returns wrapped.
   */
  private int wrapReturnsRecursive(Statement statement, MethodNode methodNode, AnnotationNode annotation, SourceUnit sourceUnit) {
    int res = 0;
    if (statement instanceof BlockStatement) {
      res += wrapReturnStatements(statement, methodNode, annotation, sourceUnit);
      for (Statement s : ((BlockStatement) statement).getStatements()) {
        res += wrapReturnsRecursive(s, methodNode, annotation, sourceUnit);
      }
    } else if (statement instanceof IfStatement) {
      IfStatement ifStatement = (IfStatement) statement;
      res += wrapReturnStatements(ifStatement.getIfBlock(), methodNode, annotation, sourceUnit);
      res += wrapReturnStatements(ifStatement.getElseBlock(), methodNode, annotation, sourceUnit);
      res += wrapReturnsRecursive(ifStatement.getIfBlock(), methodNode, annotation, sourceUnit);
      res += wrapReturnsRecursive(ifStatement.getElseBlock(), methodNode, annotation, sourceUnit);
    }
    return res;
  }

  /**
   * Wraps any return statements in the block with a call to the ExtensionPointHelper.invokePost() method.
   *
   * @param wrapStatement The statement that might have a returnStatement to wrap.  Only wraps returns inside of BlockStatements.
   * @param methodNode    The method this statement is in.
   * @param annotation    The ExtensionPoint annotation node.
   * @param sourceUnit    The source the class came from.
   * @return The number returns wrapped.
   */
  private int wrapReturnStatements(Statement wrapStatement, MethodNode methodNode, AnnotationNode annotation, SourceUnit sourceUnit) {
    if (!(wrapStatement instanceof BlockStatement)) {
      return 0;
    }
    int count = 0;
    BlockStatement blockStatement = (BlockStatement) wrapStatement;

    Expression targetEntityExpression = annotation.getMember("value");
    ClassNode interfaceType = targetEntityExpression.getType();

    List<Statement> statements = blockStatement.getStatements();
    List<Expression> argumentList;

    // Find all returns, insert call to invokePost.
    // {
    //   Object returnValue = (original return expression);
    //   return ExtensionPointHelper.instance.invokePost(SampleExtensionInterface, 'postCoreMethod', returnValue, argument1);
    // }
    BlockStatement replacementStatement;
    for (int i = 0; i < statements.size(); i++) {
      Statement statement = statements.get(i);
      if (statement instanceof ReturnStatement) {
        ReturnStatement returnStatement = (ReturnStatement) statement;
        Expression returnExpression = returnStatement.getExpression();

        VariableExpression returnVariableExpression = new VariableExpression("_returnValue");
        DeclarationExpression declarationExpression = new DeclarationExpression(returnVariableExpression,
            Token.newSymbol(Types.EQUAL, -1, -1),
            returnExpression);
        replacementStatement = (new BlockStatement(new Statement[]{new ExpressionStatement(declarationExpression)}, new VariableScope()));

        String customPostMethodName = "post" + ASTUtils.upperCaseFirstLetter(methodNode.getName());
        argumentList = new ArrayList<>();
        argumentList.add(new ClassExpression(interfaceType));
        argumentList.add(new ConstantExpression(customPostMethodName));
        argumentList.add(returnVariableExpression);
        for (Parameter parameter : methodNode.getParameters()) {
          argumentList.add(new VariableExpression(parameter));
        }

        MethodCallExpression postMethodCall = new MethodCallExpression(
            new PropertyExpression(new ClassExpression(new ClassNode(ExtensionPointHelper.class)), "instance"),
            "invokePost",
            new ArgumentListExpression(argumentList));
        BlockStatement invokePostStatement = new BlockStatement();
        invokePostStatement.addStatement(new ExpressionStatement(postMethodCall));
        replacementStatement.addStatement(new ExpressionStatement(postMethodCall));
        statements.set(i, replacementStatement);
        count++;
      }
    }
    return count;
  }

}
