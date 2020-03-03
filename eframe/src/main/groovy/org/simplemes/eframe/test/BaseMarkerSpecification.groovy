/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.test

import groovy.util.logging.Slf4j

/**
 * This is a base class that provide additional support for testing the Freemarker marker (helper) classes.
 */
@Slf4j
class BaseMarkerSpecification extends BaseSpecification {

  /**
   * Executes the marker to generate the page content.
   * Options include: <b>source, controllerClass, uri, domainObject and dataModel.</b>
   *
   * @param options See {@link UnitTestRenderer} for options supported.
   * @return The generated content.
   */
  @SuppressWarnings("GroovyAssignabilityCheck")
  String execute(Map options) {
    return new UnitTestRenderer(options).render()
  }

}
