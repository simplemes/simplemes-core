package org.simplemes.eframe.test.annotation;

import org.codehaus.groovy.ast.*;
import org.codehaus.groovy.ast.expr.*;
import org.codehaus.groovy.ast.stmt.BlockStatement;
import org.codehaus.groovy.ast.stmt.ExpressionStatement;
import org.codehaus.groovy.control.CompilePhase;
import org.codehaus.groovy.control.SourceUnit;
import org.codehaus.groovy.syntax.SyntaxException;
import org.codehaus.groovy.transform.ASTTransformation;
import org.codehaus.groovy.transform.GroovyASTTransformation;
import org.simplemes.eframe.domain.annotation.DomainEntityHelper;
import org.spockframework.runtime.model.FeatureMetadata;

import java.util.List;


/**
 * This annotation can be used to mark a method to force a rollback when the test ends.
 * <p>
 * <b>Note</b>: This should only be used in tests.
 */
@SuppressWarnings({"DefaultAnnotationParam", "unused"})
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
          // All methods are returned by getAllDeclaredMethods(), even super-class methods.
          // We will use the line number to determine if this method is really in the source file being compiled.
          if (methodNode.getLineNumber() >= 0) {
            wrapWithRollback(methodNode, sourceUnit);
          }
        }
      }
    }
  }

/*
  void log(String msg) {
    try {
      msg = msg + "\n";
      Files.write(Paths.get("c:\\tmp\\out.log"), msg.getBytes(), StandardOpenOption.APPEND);
    } catch (IOException ignored) {
      //exception handling left as an exercise for the reader
      ignored.printStackTrace();
    }
  }
*/

  /**
   * Wraps the given method with a closure that is called within a transaction that is automatically rolled back.
   *
   * @param method The method to wrap.
   */
  protected void wrapWithRollback(MethodNode method, SourceUnit sourceUnit) {

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

    MethodCallExpression setRollbackMethod = new MethodCallExpression(new VariableExpression("status"),
        "setRollbackOnly", new ArgumentListExpression(Parameter.EMPTY_ARRAY));
    closureBlock.addStatement(new ExpressionStatement(setRollbackMethod));
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
