
package org.simplemes.eframe.data.annotation;

import org.codehaus.groovy.transform.GroovyASTTransformationClass;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation allows the application developer to mark a domain object as allowing extension fields (custom fields).
 * This marks the domain class as extensible with custom fields and allows the developer to store these extensible fields
 * into a column of their choosing with a user-definable column size/type.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
@GroovyASTTransformationClass("org.simplemes.eframe.data.annotation.ExtensibleFieldsTransformation")
public @interface ExtensibleFields {

  /**
   * The default field name to store the custom values in.  Default is 'customFields'.
   */
  String DEFAULT_FIELD_NAME = "_customFields";

  /*
   * The static field to store the custom definitions in.  Value is '_customFieldDef'.
   */
  //String DEFINITION_FIELD_NAME = "_customFieldDef";

  /**
   * Defines the maximum size of the custom field value holder.  Default is 1024.
   */
  int DEFAULT_MAX_SIZE = 1024;

  /**
   * The name of the transient complex custom field element.  Not configurable.  The custom field name is the key for
   * this Map.
   */
  String COMPLEX_CUSTOM_FIELD_NAME = "_complexCustomFields";

  /**
   * Defines the field name to store the custom values in.  Default is 'customFields'.
   *
   * @return the field name.
   */
  String fieldName() default DEFAULT_FIELD_NAME;

  /**
   * Defines the maximum size of the custom field value holder.  This is the maximum size of all custom fields for one
   * domain record.
   * If the size is larger than a varchar() can hold in the database,
   * the type of the column is changed to TEXT.  All of the custom fields combined cannot exceed this value, even
   * if the values are stored in a TEXT column.
   * Default: 1024.
   *
   * @return The max field length.
   */
  int maxSize() default DEFAULT_MAX_SIZE;
}
