
/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.data.annotation;

import org.codehaus.groovy.transform.GroovyASTTransformationClass;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation allows the application developer to mark a domain object as allowing extension fields (custom fields).
 * This marks the domain class as extensible with custom fields and allows the developer to store these extensible fields
 * into a column of their choosing with a user-definable JSONB column.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD})
@GroovyASTTransformationClass("org.simplemes.eframe.data.annotation.ExtensibleFieldHolderTransformation")
public @interface ExtensibleFieldHolder {

  /**
   * The field name that holds the custom field holder name.
   */
  String HOLDER_FIELD_NAME = "_customFieldName";

  /**
   * The name of the transient complex custom field element.  Not configurable.  The custom field name is the key for
   * this Map.
   */
  String COMPLEX_CUSTOM_FIELD_NAME = "_complexCustomFields";

  /**
   * The name of the element in the COMPLEX_CUSTOM_FIELD_NAME Map that holds a reference to itself.
   */
  String COMPLEX_THIS_NAME = "_this";

}

