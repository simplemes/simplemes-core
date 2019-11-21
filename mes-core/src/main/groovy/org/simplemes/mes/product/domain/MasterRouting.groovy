package org.simplemes.mes.product.domain

import grails.gorm.annotation.Entity
import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString
import org.simplemes.mes.misc.FieldSizes

/*
 * Copyright Michael Houston. All rights reserved.
 *
*/

/**
 * Defines a routing for the production of one or more products.
 * A routing is a sequence of steps (operations) needed to manufacture a product.
 * These steps operations can be simple actions such as ASSEMBLE or TEST.
 * They may also be a composite operation that is made up of several actions.
 * <p/>
 * This sub-class is used to attach a routing to multiple products in general but not to a specific
 * product.
 *
 */
@Entity
@ToString(includeNames = true, includePackage = false)
@EqualsAndHashCode(includes = ['routing'])
class MasterRouting extends Routing {
  /**
   * The Routing as known to the users.  <b>Primary Code Field</b>.
   * <p/>
   * Maximum length is defined by {@link FieldSizes#MAX_PRODUCT_LENGTH}.
   */
  String routing

  /**
   * The routing's title (short description).
   * <p/>
   * Maximum length is defined by {@link FieldSizes#MAX_TITLE_LENGTH}.
   */
  String title

  /**
   * This domain is a top-level searchable element.
   */
  static searchable = true

  /**
   * Internal values.
   */
  static constraints = {
    routing(maxSize: FieldSizes.MAX_PRODUCT_LENGTH, unique: true, nullable: false, blank: false)
    title(maxSize: FieldSizes.MAX_TITLE_LENGTH, nullable: true)
  }

  /**
   * Defines the default general field ordering for GUIs and other field listings/reports.
   */
  @SuppressWarnings("GroovyUnusedDeclaration")
  static fieldOrder = ['routing', 'title', 'operations']

}
