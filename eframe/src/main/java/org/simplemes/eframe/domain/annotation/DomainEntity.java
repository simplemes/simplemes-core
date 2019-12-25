
package org.simplemes.eframe.domain.annotation;

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
@GroovyASTTransformationClass("org.simplemes.eframe.domain.annotation.DomainEntityTransformation")
public @interface DomainEntity {

  /**
   * The name of the static field to create for holding the domain's repository.
   */
  String DEFAULT_REPOSITORY_FIELD_NAME = "repository";

  /**
   * Defines the field name to store the custom values in.  Default is 'customFields'.
   *
   * @return the field name.
   */
  String repositoryFieldName() default DEFAULT_REPOSITORY_FIELD_NAME;

  /**
   * The repository class (interface) for this domain entity.  If not given, then will add 'Repository' to the
   * domain class name for the repository class.
   *
   * @return The repository class (interface).
   */
  Class repository() default Object.class;
}

