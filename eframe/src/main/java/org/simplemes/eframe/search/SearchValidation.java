/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.search;/*
 * Copyright Michael Houston 2020. All rights reserved.
 * Original Author: mph
 *
 */

import com.fasterxml.jackson.annotation.JsonFilter;
import groovy.lang.Closure;
import org.codehaus.groovy.ast.AnnotationNode;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.FieldNode;
import org.codehaus.groovy.ast.expr.ClosureExpression;
import org.codehaus.groovy.ast.expr.ConstantExpression;
import org.codehaus.groovy.ast.expr.Expression;
import org.codehaus.groovy.ast.stmt.Statement;
import org.codehaus.groovy.control.SourceUnit;
import org.codehaus.groovy.control.messages.SimpleMessage;
import org.codehaus.groovy.control.messages.WarningMessage;

import java.util.List;

/**
 * Provides validations for Domain Entities that check for search mis-configurations.
 */
public class SearchValidation {

  /**
   * The most recent warning message.  Used for testing.
   */
  static public String lastWarningMessage = null;

  /**
   * Validates the search settings are correct.
   *
   * @param classNode  The class to validate.
   * @param sourceUnit The source location.
   */
  public static void validate(ClassNode classNode, SourceUnit sourceUnit) {
    // Make sure if the searchable exclude is used, then it has the @JsonFilter.
    List<AnnotationNode> annotations = classNode.getAnnotations(new ClassNode(JsonFilter.class));
    String filterName = null;
    if (annotations.size() > 0) {
      filterName = annotations.get(0).getMember("value").getText();
    }

    FieldNode fieldNode = classNode.getField("searchable");
    if (fieldNode != null) {
      if (!fieldNode.isStatic()) {
        SimpleMessage message = new SimpleMessage("Domain entity 'searchable' field must be static in " + classNode, sourceUnit);
        sourceUnit.getErrorCollector().addError(message);
        return;
      }
      Class fieldClass = getFieldClass(fieldNode);
      if (!(fieldClass == Boolean.class) && !(fieldClass == Closure.class)) {
        SimpleMessage message = new SimpleMessage("Domain entity 'searchable' field must be a boolean or a closure in " + classNode, sourceUnit);
        sourceUnit.getErrorCollector().addError(message);
        return;
      }

      if (!"searchableFilter".equals(filterName)) {
        if (hasExcludedFields(fieldNode)) {
          SimpleMessage message = new SimpleMessage("Domain entity must have @JsonFilter('searchableFilter') annotation since it has excluded fields in the 'searchable' field. Class: " + classNode, sourceUnit);
          sourceUnit.getErrorCollector().addError(message);
        } else {
          // No excluded, just make this a warning.
          WarningMessage message = new WarningMessage(WarningMessage.LIKELY_ERRORS,
              "Searchable Domains should have a @JsonFilter('searchableFilter') annotation in " + classNode,
              sourceUnit.getCST(), sourceUnit);
          lastWarningMessage = message.getMessage();
          sourceUnit.getErrorCollector().addWarning(message);
        }
      }
    }
  }

  /**
   * Returns true if the given searchable element has
   *
   * @param searchableNode The searchable field.
   * @return True if it has a closure with exclude elements.
   */
  private static boolean hasExcludedFields(FieldNode searchableNode) {
    Expression expression = searchableNode.getInitialValueExpression();
    if (expression instanceof ClosureExpression) {
      Statement closure = ((ClosureExpression) expression).getCode();
      // Uses a simple test on the text.  Could look at the expressions in detail for more robust check.
      return closure.getText().contains("exclude");
    }
    return false;
  }

  /**
   * Returns the effective class of the given searchable element value.
   *
   * @param searchableNode The searchable field.
   * @return The class type of the value (Object, Boolean or Closure).
   */
  private static Class getFieldClass(FieldNode searchableNode) {
    Expression expression = searchableNode.getInitialValueExpression();

    if (expression instanceof ClosureExpression) {
      return Closure.class;
    }
    if (expression instanceof ConstantExpression) {
      if (((ConstantExpression) expression).getValue() instanceof Boolean) {
        return Boolean.class;
      }
    }

    // Not known from initial value.
    return Object.class;
  }


}
