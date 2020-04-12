
/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.custom.annotation;

import org.codehaus.groovy.transform.GroovyASTTransformationClass;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

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
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
@GroovyASTTransformationClass("org.simplemes.eframe.custom.annotation.ExtensionPointTransformation")
public @interface ExtensionPoint {

  /**
   * The required value for the annotation.  This is a interface class that marks all extensions to the given method.
   * Typically, this means the interface includes a pre and post method.
   *
   * @return The interface class.
   */
  Class value();

  /**
   * An optional comment that is used in the generation of ASCII Doctor file that contains all
   * of the current module's extension points.
   *
   * @return The comment.
   */
  String comment() default "";

}

