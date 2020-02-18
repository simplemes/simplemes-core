/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.custom

import groovy.util.logging.Slf4j
import org.simplemes.eframe.application.Holders

/**
 * Support methods and data for additions.  Includes a list of additions discovered.
 */
@Slf4j
class AdditionHelper {

  /**
   * A singleton to access addition helper methods/data.
   */
  static AdditionHelper instance = new AdditionHelper()

  /**
   * Returns the list of additions.
   * @return The list.
   */
  @SuppressWarnings("GroovyAssignabilityCheck")
  List<AdditionInterface> getAdditions() {
    def allAdditions = Holders.applicationContext.getBeansOfType(AdditionInterface) as List
    log.debug("getAdditions(): additions found {} ", allAdditions)
    return allAdditions
  }

}
