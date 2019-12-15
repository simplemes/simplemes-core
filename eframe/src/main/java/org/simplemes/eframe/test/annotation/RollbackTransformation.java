package org.simplemes.eframe.test.annotation;

import org.codehaus.groovy.ast.*;
import org.codehaus.groovy.ast.expr.*;
import org.codehaus.groovy.ast.stmt.*;
import org.codehaus.groovy.control.CompilePhase;
import org.codehaus.groovy.control.SourceUnit;
import org.codehaus.groovy.syntax.Token;
import org.codehaus.groovy.transform.ASTTransformation;
import org.codehaus.groovy.transform.GroovyASTTransformation;
import org.simplemes.eframe.domain.annotation.DomainEntityHelper;

import java.io.IOException;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;


/**
 * This annotation can be used to mark a method to force a rollback when the test ends.
 * <p>
 * <b>Note</b>: This should only be used in tests.
 */
@SuppressWarnings({"DefaultAnnotationParam"})
@GroovyASTTransformation(phase = CompilePhase.CANONICALIZATION)
public class RollbackTransformation implements ASTTransformation {

  /**
   * Basic Constructor.
   */
  @SuppressWarnings("unused")
  public RollbackTransformation() {
  }

  public void visit(ASTNode[] astNodes, SourceUnit sourceUnit) {
    //System.out.println("visit");
    if (sourceUnit.getAST() == null) {
      return;
    }

    // find all methods annotated with @Rollback.
    ClassNode ourAnnotationNode = new ClassNode(Rollback.class);
    for (ASTNode astNode : astNodes) {
      if (astNode instanceof MethodNode) {
        MethodNode methodNode = (MethodNode) astNode;
        //System.out.println("Checking astNode = " + astNode);
        List<AnnotationNode> annotations = methodNode.getAnnotations(ourAnnotationNode);
        if (annotations.size() > 0) {
          //System.out.println("  Has Annotation");
          // All methods are returned by getAllDeclaredMethods(), even super-class methods.
          // We will use the line number to determine if this method is really in the source file being compiled.
          if (methodNode.getLineNumber() >= 0) {
            System.out.println("  method = " + methodNode + " class =" + methodNode.getDeclaringClass().getNameWithoutPackage());
            //log("  method = " + methodNode+ " class ="+methodNode.getDeclaringClass().getNameWithoutPackage());
            //encloseMethodAsInnerClass(methodNode, sourceUnit);
            callToHandler(methodNode);
          }
        }
      }
    }
  }

  void log(String msg) {
    try {
      msg = msg + "\n";
      Files.write(Paths.get("c:\\tmp\\out.log"), msg.getBytes(), StandardOpenOption.APPEND);
    } catch (IOException ignored) {
      //exception handling left as an exercise for the reader
      ignored.printStackTrace();
    }
  }

  protected void callToHandler(MethodNode method) {

    // TODO: is this needed? method.getParameters()[0].setClosureSharedVariable(true);

    ClosureExpression body = new ClosureExpression( //

        // new Parameter[] { new Parameter(ClassHelper.OBJECT_TYPE, "stringParam") }, //
        Parameter.EMPTY_ARRAY,
        // new Parameter[0],
        // method.getParameters(),
        // null, //

        method.getCode());

    VariableScope scope = new VariableScope(method.getVariableScope());
    // method.getVariableScope().getDeclaredVariables().forEach((k, v) -> {
    // scope.putDeclaredVariable(v);
    // });
    body.setVariableScope(scope);

    MethodCallExpression callExp = new MethodCallExpression( //
        VariableExpression.THIS_EXPRESSION, //
        "handleWrapped", //
        new ArgumentListExpression(body));

    BlockStatement block = new BlockStatement();
    block.addStatement(new ExpressionStatement(callExp));
    method.setCode(block);
  }

  /**
   * Adds a new method and forwards calls to this new method.
   *
   * @param methodNode intercepted method
   * @param sourceUnit The source code.
   */
  private void encloseMethodAsInnerClass(MethodNode methodNode, SourceUnit sourceUnit) {
    // TODO: Check for test only? validateAnnotation(methodNode, sourceUnit);

    /*
      Adds this code to wrap around the original method code.
      RollbackHelper.getTransactionManager().executeWrite {_$status ->
        // Original Method
        _$status.setRollbackOnly()
      }

     */

    // TODO: Look at ClosureExpression()
    // and : https://stackoverflow.com/questions/45305978/groovy-ast-transformation-closure-visibility
    //       https://stackoverflow.com/questions/38099327/ast-transformation-to-wrap-entire-method-body-in-a-closure

    Statement originalCode = methodNode.getCode();
    //Parameter[] parameters = methodNode.getParameters();
    //ClassNode[] exceptions = methodNode.getExceptions();

    //ClassNode innerClass = new ClassNode(methodNode.getName()+"Closure",Modifier.FINAL,new ClassNode(Closure.class));
    //innerClass.addMethod(new MethodNode("doCall",Modifier.PUBLIC, methodNode.getReturnType(),
    //    parameters,exceptions,originalCode));

    BlockStatement finallyStatement = new BlockStatement();
    //CatchStatement catchStatement = new CatchStatement();
    //finallyStatement.addStatement();
    // return = DomainEntityHelper.instance.XYZ(Object)
    MethodCallExpression method = new MethodCallExpression(
        new PropertyExpression(new ClassExpression(new ClassNode(DomainEntityHelper.class)), "instance"),
        "getApplicationContext",
        new ArgumentListExpression(Parameter.EMPTY_ARRAY));
    //finallyStatement.addStatement(new ExpressionStatement(method));
    finallyStatement.addStatement(createPrintlnAst("Finally:", "this"));


    BlockStatement methodBody = new BlockStatement();
    TryCatchStatement tryCatch = new TryCatchStatement(originalCode, finallyStatement);
    tryCatch.addCatch(new CatchStatement(new Parameter(new ClassNode(Throwable.class), "t"),
        createPrintlnAst("Catch:", "this")));
    methodBody.addStatement(tryCatch);
    //methodBody.addStatement(innerClass);
    methodNode.setCode(methodBody);
  }

  /**
   * Adds a new method and forwards calls to this new method.
   *
   * @param methodNode intercepted method
   * @param sourceUnit The source code.
   */
  private void shadowMethodOld(MethodNode methodNode, SourceUnit sourceUnit) {
    String parentClassName = methodNode.getDeclaringClass().getNameWithoutPackage();
    // CODE:   return __method__

    // Add new method to forward call
    String methodName = methodNode.getName();
    MethodNode forwardTo = addForwardToMethodAst(methodNode);

    BlockStatement methodBody = new BlockStatement();

    // Get the start time (in ms).
    // CODE:                                                         def __startTime__ = System.currentTimeMillis()
    Expression left0 = new VariableExpression("__startTime__", new ClassNode(Long.class));
    Token assign0 = Token.newSymbol("=", -1, -1);
    Expression right0 = new MethodCallExpression(new ClassExpression(new ClassNode(System.class)), "currentTimeMillis",
        new ArgumentListExpression());
    Expression assignment0 = new DeclarationExpression(left0, assign0, right0);
    methodBody.addStatement(new ExpressionStatement(assignment0));

    // Define the variable needed to hold the function
    // CODE:                                                         def __method__ = null
    Expression left1 = new VariableExpression("__" + methodName + "__");
    Token assign1 = Token.newSymbol("=", -1, -1);
    Expression right1 = new ConstantExpression(null);
    Expression assignment1 = new DeclarationExpression(left1, assign1, right1);
    methodBody.addStatement(new ExpressionStatement(assignment1));

    // Define the variable to track unhandled exceptions from the method call.
    // CODE:                                                         def __exception__ = false
    Expression left1a = new VariableExpression("__exception__");
    Token assign1a = Token.newSymbol("=", -1, -1);
    Expression right1a = new ConstantExpression(false);
    Expression assignment1a = new DeclarationExpression(left1a, assign1a, right1a);
    methodBody.addStatement(new ExpressionStatement(assignment1a));

    // Now call the actual method inside of a try block.
    BlockStatement tryBlock = new BlockStatement();
    // CODE:                                                       try {
    // CODE:                                                         __method__ = $method$()
    Expression left2 = new VariableExpression("__" + methodName + "__");
    Token assign2 = Token.newSymbol("=", -1, -1);
    Expression right2 = new MethodCallExpression(new VariableExpression("this"), forwardTo.getName(),
        new ArgumentListExpression(methodNode.getParameters()));
    Expression assignment2 = new BinaryExpression(left2, assign2, right2);
    tryBlock.addStatement(new ExpressionStatement(assignment2));
    //methodBody.addStatement(new ExpressionStatement(assignment2));

    // Add return statement
    // CODE:                                                         return __method__
    tryBlock.addStatement(new ReturnStatement(left2));

    BlockStatement catchBody = new BlockStatement();
    // Now, create a catch block to detect uncaught exceptions.
    // CODE:                                                       } catch(Throwable t) {
    // CODE:                                                         __exception__ = true
    // CODE:                                                         throw t
    Expression left2a = new VariableExpression("__exception__");
    Token assign2a = Token.newSymbol("=", -1, -1);
    Expression right2a = new ConstantExpression(true);
    //Expression assignment2a = new DeclarationExpression(left2a, assign2a, right2a);
    Expression assignment2a = new BinaryExpression(left2a, assign2a, right2a);
    catchBody.addStatement(new ExpressionStatement(assignment2a));
    //catchBody.addStatement(createPrintlnAst("  Catch","__exception__"));

    ThrowStatement throwStatement = new ThrowStatement(new VariableExpression("t"));
    catchBody.addStatement(throwStatement);

    ClassNode exceptionType = ClassHelper.make(Throwable.class);
    Parameter exceptionParameter = new Parameter(exceptionType, "t");
    CatchStatement catchStatement = new CatchStatement(exceptionParameter, catchBody);


    // Now, populate the finally block with the code for the metrics.
    // CODE:                                                       } finally {
    BlockStatement finallyBlock = new BlockStatement();
    // Now, calculate the end time (ms).
    // CODE:                                                         def __endTime__ = System.currentTimeMillis()
    Expression left3 = new VariableExpression("__endTime__", new ClassNode(Long.class));
    Token assign3 = Token.newSymbol("=", -1, -1);
    Expression right3 = new MethodCallExpression(new ClassExpression(new ClassNode(System.class)), "currentTimeMillis",
        new ArgumentListExpression());
    Expression assignment3 = new DeclarationExpression(left3, assign3, right3);
    finallyBlock.addStatement(new ExpressionStatement(assignment3));

    // Now, calculate the elapsed time.
    // We need to use the Long,minus() method for some reason in Groovy 2.4.13.
    // The original def __elapsedTime__ = __endTime__ - __startTime__ triggered an NPE in:
    //   at org.codehaus.groovy.classGen.asm.sc.StaticTypesCallSiteWriter.makeSingleArgumentCall(StaticTypesCallSiteWriter.java:635)
    // Probably caused by use of the '-' operator in the wrong way.
    // CODE:                                                         Long __elapsedTime__ = __endTime__.minus(__startTime__)
    // Static method call?
    VariableExpression left4 = new VariableExpression("__elapsedTime__", new ClassNode(Long.class));
    VariableExpression endTimeVariable = new VariableExpression("__endTime__");
    List<Expression> argumentList4 = new ArrayList<>();
    argumentList4.add(new VariableExpression("__startTime__"));
    Expression methodStatement4 = new MethodCallExpression(endTimeVariable,
        "minus",
        new ArgumentListExpression(argumentList4));
    Expression assignment4 = new DeclarationExpression(left4, Token.newSymbol("=", -1, -1), methodStatement4);
    finallyBlock.addStatement(new ExpressionStatement(assignment4));

    // Now, call the performance tracker with the results of our method call.
    // CODE:     PerformanceTracker.getInstance().trackCall("SampleController.method",__startTime__, __elapsedTime__)
    ClassExpression classExpression5 = new ClassExpression(ClassHelper.make("org.simplemes.eframe.perf.PerformanceTracker"));
    List<Expression> argumentList = new ArrayList<>();
    argumentList.add(new ConstantExpression(parentClassName + "." + methodName));
    argumentList.add(new VariableExpression("__startTime__"));
    argumentList.add(new VariableExpression("__elapsedTime__"));
    argumentList.add(new VariableExpression("__exception__"));

    MethodCallExpression getInstanceExpression = new MethodCallExpression(classExpression5, "getInstance",
        new ArgumentListExpression());
    Expression methodStatement5 = new MethodCallExpression(getInstanceExpression,
        "trackCall",
        new ArgumentListExpression(argumentList));
    //finallyBlock.addStatement(createPrintlnAst("  Finally","__exception__"));
    //finallyBlock.addStatement(createPrintlnAst("Finally:","transactionStatus"));
    finallyBlock.addStatement(new ExpressionStatement(methodStatement5));

    // Now, build the try/catch/finally statement.
    TryCatchStatement tryCatch = new TryCatchStatement(tryBlock, finallyBlock);
    tryCatch.addCatch(catchStatement);

    methodBody.addStatement(tryCatch);
    //methodBody.addStatement(createPrintlnAst("Calling " + methodName));


    methodNode.setCode(methodBody);
  }


  /**
   * Adds a new private method with the same body as forwarding method.
   *
   * @param forwardingMethod the intercepted method.
   * @return a new method to which calls are forwarded.
   */
  private MethodNode addForwardToMethodAst(MethodNode forwardingMethod) {
    ClassNode parent = forwardingMethod.getDeclaringClass();
    String methodName = "$" + forwardingMethod.getName() + "$";
    ClassNode returnType = forwardingMethod.getReturnType();
    Parameter[] parameters = forwardingMethod.getParameters();
    ClassNode[] exceptions = forwardingMethod.getExceptions();
    Statement body = forwardingMethod.getCode();
    MethodNode forwardedTo = new MethodNode(methodName, Modifier.PRIVATE, returnType, parameters, exceptions, body);
    parent.addMethod(forwardedTo);
    return forwardedTo;
  }

  private Statement createPrintlnAst(String message, String variableName) {
    Expression left = new ConstantExpression(message);
    Expression right = new VariableExpression(variableName);
    Expression binaryExpression = new BinaryExpression(left, Token.newSymbol("+", -1, -1), right);

    return new ExpressionStatement(
        new MethodCallExpression(
            new VariableExpression("this"),
            new ConstantExpression("println"),
            new ArgumentListExpression(binaryExpression)
        )
    );
  }

}
