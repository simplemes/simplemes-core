
package org.simplemes.eframe.test.annotation;

import org.codehaus.groovy.transform.GroovyASTTransformationClass;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation can be used to mark a method to force a rollback when the test ends.
 * <p>
 * <b>Note</b>: This should only be used in tests.  It does not support the use of the Spock 'where' clause.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
@GroovyASTTransformationClass("org.simplemes.eframe.test.annotation.RollbackTransformation")
public @interface Rollback {

}

