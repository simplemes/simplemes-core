/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.web.ui.webix.freemarker

import freemarker.template.Configuration
import freemarker.template.TemplateModelException
import groovy.util.logging.Slf4j
import org.simplemes.eframe.misc.LogUtils
import org.simplemes.eframe.misc.TypeUtils

/**
 * Updates the Freemarker configuration needed for a standard freemarker setup.
 * Mainly creates the shared variables for all of the freemarker template directive model
 * (marker) implementations for the EFrame.
 */
@Slf4j
class FreemarkerDirectiveConfiguration {

  static markers = ['Asset', 'Button', 'ButtonGroup', 'Create', 'Dashboard', 'Edit', 'Field', 'Form', 'GUISetup',
                    'HTML', 'Lookup', 'Language', 'List', 'Menu', 'MenuItem', 'Messages', 'PreloadMessages', 'Show', 'Title']

  /**
   * Adds all of the framework markers to the freemarker configuration.
   * @param configuration The freemarker configuration to update.
   */
  static addSharedVariables(Configuration configuration) {
    for (marker in markers) {
      try {
        def className = "${FreemarkerDirectiveConfiguration.class.getPackage().name}.${marker}Marker"
        def clazz = TypeUtils.loadClass(className)
        configuration.setSharedVariable("ef${marker}", new FreemarkerDirective(clazz))
      } catch (TemplateModelException e) {
        LogUtils.logStackTrace(log, e, configuration)
      }
    }
  }
}
