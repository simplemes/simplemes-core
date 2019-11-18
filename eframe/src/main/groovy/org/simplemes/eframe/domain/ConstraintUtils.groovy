package org.simplemes.eframe.domain


import org.grails.datastore.gorm.validation.constraints.eval.DefaultConstraintEvaluator
import org.grails.datastore.mapping.model.PersistentProperty

/*
 * Copyright Michael Houston 2018. All rights reserved.
 * Original Author: mph
 *
*/

/**
 *
 */
class ConstraintUtils {

  /**
   * A singleton, used for simplified unit testing with a mocked class.
   */
  static ConstraintUtils instance = new ConstraintUtils()

  /**
   * Determines the given property's maxSize (if any).
   * @param property The GORM property. (If null, then this method returns null).
   * @return The maxSize (can be null).
   */
  Integer getPropertyMaxSize(PersistentProperty property) {
    if (property == null) {
      return null
    }
    // This is based on the code in GORM:
    //  https://github.com/grails/grails-data-mapping/blob/347001e1e7a28e1b5b0b1cf888328ad51139764c/grails-datastore-gorm-validation/src/main/groovy/grails/gorm/validation/PersistentEntityValidator.groovy
    //  https://github.com/grails/grails-data-mapping/blob/347001e1e7a28e1b5b0b1cf888328ad51139764c/grails-datastore-gorm-validation/src/main/groovy/org/grails/datastore/gorm/validation/constraints/eval/DefaultConstraintEvaluator.java
    //  https://github.com/grails/grails-data-mapping/blob/347001e1e7a28e1b5b0b1cf888328ad51139764c/grails-datastore-gorm-validation/src/main/groovy/grails/gorm/validation/DefaultConstrainedProperty.groovy
    if (property.type == String) {
      def constraintsEvaluator = new DefaultConstraintEvaluator()
      def evaluated = constraintsEvaluator.evaluate(property.owner.javaClass)
      def single = evaluated[property.name]
      return single.maxSize
    }
    return null
  }

  /**
   * Determines the scale constraint for a given property.
   * @param property The property (typically a BigDecimal property).
   * @return The scale (decimal precision for the DB).  Null if no constraint found.
   */
  Integer getPropertyScale(PersistentProperty property) {
    if (property == null) {
      return null
    }
    if (property.type == BigDecimal) {
      def constraintsEvaluator = new DefaultConstraintEvaluator()
      def evaluated = constraintsEvaluator.evaluate(property.owner.javaClass)
      def single = evaluated[property.name]
      return single.scale
    }
    return null
  }

  /**
   * Determines the given constraint setting.
   * @param property The property.
   * @param constraintName The constraint to return (e.g. 'nullable').
   * @return The constraint setting.  Can be null.
   */
  Object getProperty(PersistentProperty property, String constraintName) {
    if (property == null) {
      return null
    }
    def constraintsEvaluator = new DefaultConstraintEvaluator()
    def evaluated = constraintsEvaluator.evaluate(property.owner.javaClass)
    def single = evaluated[property.name]
    if (!single) {
      return false
    }
    return single[constraintName]
  }

}
