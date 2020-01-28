/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.test.annotation;

import org.codehaus.groovy.ast.ASTNode;
import org.codehaus.groovy.ast.AnnotationNode;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.MethodNode;
import org.codehaus.groovy.control.CompilePhase;
import org.codehaus.groovy.control.SourceUnit;
import org.codehaus.groovy.transform.ASTTransformation;
import org.codehaus.groovy.transform.GroovyASTTransformation;
import org.simplemes.eframe.ast.ASTUtils;

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
            ASTUtils.wrapWithTransaction(true, methodNode, sourceUnit);
          }
        }
      }
    }
  }

}
