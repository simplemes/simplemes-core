package org.simplemes.eframe.data


import org.grails.orm.hibernate.cfg.HibernateMappingContextConfiguration
import org.simplemes.eframe.custom.AdditionHelper
import org.simplemes.eframe.date.DateOnly

/*
 * Copyright Michael Houston 2018. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Modifies the hibernate configuration for framework supported features.
 * This includes adding support for EncodedType and DateOnlyType fields.
 */
class EframeHibernateMappingContextConfiguration extends HibernateMappingContextConfiguration {
  EframeHibernateMappingContextConfiguration() {
    // Generate a specific hibernate type for each encoded type found in all additions.
    for (clazz in encodedTypes) {
      registerTypeOverride(new EncodedType(clazz), [clazz.name] as String[])
    }
    registerTypeOverride(new DateOnlyType(), [DateOnly.name] as String[])
  }

  /**
   * Builds the type names for encoded types.  These are defined in the additions used for all modules.
   * @return The list of classes that are EncodedTypes.
   */
  List<Class> getEncodedTypes() {
    def res = []
    for (addition in AdditionHelper.instance.additions) {
      for (clazz in addition.encodedTypes) {
        res << clazz
      }
    }

    return res
  }

}
