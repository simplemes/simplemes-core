package org.simplemes.eframe.custom.domain

import grails.gorm.annotation.Entity
import groovy.transform.EqualsAndHashCode
import org.simplemes.eframe.misc.FieldSizes

/**
 * This class defines a single custom field for an application.  This field is inserted into the domain class
 * by the Enterprise Framework plugin at startup or when the custom field is changed.
 *
 */
@Entity
@EqualsAndHashCode
class FieldExtension extends AbstractField {

  /**
   * The domain class name this field is applied to (full package name and class).
   */
  String domainClassName

  /**
   * Internal field constraints.
   */
  @SuppressWarnings("unused")
  static constraints = {
    domainClassName(maxSize: FieldSizes.MAX_CLASS_NAME_LENGTH, nullable: false, unique: 'fieldName')
  }

  /**
   * Internal Mapping of fields to columns.
   */
  @SuppressWarnings("unused")
  static mapping = {
    cache true
  }

  /**
   * Delete will remove any references to this field from any FieldGUIExtensions
   */
  @SuppressWarnings("unused")
  def afterDelete() {
    FieldGUIExtension.removeReferencesToField(domainClassName, fieldName)
  }

  /**
   *  Returns human readable form of this object.
   * @return human readable string.
   */

  @Override
  String toString() {
    return "FieldExtension{" +
      super.toString() +
      ", domainClassName='" + domainClassName + '\'' +
      '}'
  }
}
