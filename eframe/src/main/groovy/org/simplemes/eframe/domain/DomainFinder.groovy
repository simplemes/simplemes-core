package org.simplemes.eframe.domain

import groovy.util.logging.Slf4j
import org.simplemes.eframe.custom.AdditionHelper

/*
 * Copyright Michael Houston 2018. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * A utility class to find the
 */
@Slf4j
class DomainFinder {

  /**
   * A singleton to access addition helper methods/data.
   */
  static DomainFinder instance = new DomainFinder()

  /**
   * Finds all of the top-level classes that contain domains in all configured modules.  Checks all
   * additions for defined in all modules found (via efBootstrap.yml).
   * @return The list of classes.  The startup logic will use these to find all classes in those packages.
   */
  List<Class> getTopLevelDomainClasses() {
    List<Class> classes = []

    def additions = AdditionHelper.instance.additions
    for (addition in additions) {
      classes.addAll(addition.domainPackageClasses)
    }
    log.debug("getDomainClasses(): additions found {} ", classes)
    return classes

  }

  /**
   * Attempts to load the class.  If it fails, then a WARN log message will be written.
   * @param className The class to load.
   * @return The loaded class.
   */
/*
  protected Class loadIfPossible(String className) {
    try {
      return TypeUtils.loadClass((String) className)
    } catch (ClassNotFoundException ignored) {
      log.warn('Error loading class {}', className)
    }
    return null
  }
*/

}
