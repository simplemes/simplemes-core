package org.simplemes.eframe.custom.domain

import grails.gorm.annotation.Entity
import groovy.transform.EqualsAndHashCode

/*
 * Copyright Michael Houston. All rights reserved.
 *
*/

/**
 * Defines the basic user-defined data field used explicitly in domain class.  This defines the basic type of the field
 * and some hints for appearance and validations (list of valid values, etc).
 * This is normally a child of the FlexType to group related items together.  The actual values are stored in a
 * a user definable field (<b>flexFields</b>).
 */
@Entity
//@ToString(includePackage = false, includeNames = true)
@EqualsAndHashCode(includes = ['flexType'], callSuper = true)
class FlexField extends AbstractField {

  /**
   * This is a child of a parent FlexType
   */
  @SuppressWarnings("unused")
  static belongsTo = [flexType: FlexType]

  /**
   * Internal constraints.
   */
  @SuppressWarnings("unused")
  static constraints = {
  }

  /**
   * The primary keys for this object.
   */
  @SuppressWarnings("unused")
  static keys = ['flexType', 'fieldName']

  /**
   * Defines the order the fields are shown in the edit/show/etc GUIs.
   */
  // No added fields to be displayed.
  // Parent AbstractField class has all of the fields needed.
  @SuppressWarnings("unused")
  static fieldOrder = []

}
