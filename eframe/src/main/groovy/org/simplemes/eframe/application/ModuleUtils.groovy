/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.application

import org.simplemes.eframe.misc.ClassPathUtils

/**
 * Utility methods to detect the modules used by the current application.
 */
class ModuleUtils {

  /**
   * A singleton, used for simplified unit testing with a mocked class.
   */
  static ModuleUtils instance = new ModuleUtils()

  /**
   * Returns the modules from the class path in readable format.
   * Sorts important modules at top (simplemes, then micronaut, then alphabetical).
   * @return The list of modules in use from the class path.  Sorted.
   */
  List<String> getModules() {
    List<String> res = []

    def l = ClassPathUtils.instance.jarFiles
    //l.each {println it}
    def modules = l.findAll { it =~ /(\.\d)+\.jar/ }

    for (s in modules) {
      def index = s.lastIndexOf('/')
      if (index >= 0) {
        s = s[index + 1..-1] - '.jar'
        def matches = s =~ /-[\d]+[\.(\d)+]+/
        def version = '?'
        if (matches) {
          s = s - matches[0]
          version = matches[0] - '-'
        }
        res << (String) "$s:$version"
      }
    }

    // Now, sort by importance, then alphabetical
    return res.toSorted() { a, b -> compareModules((String) a, (String) b) }
  }

  /**
   * A list of important modules that should be at the top.
   */
  List<String> importantModules = ['webix', 'eframe']

  int compareModules(String a, String b) {
    // check for important module
    for (s in importantModules) {
      if (a.contains(s)) {
        return -1
      }
      if (b.contains(s)) {
        return 1
      }
    }

    // Special case for mes-* modules
    if (a.startsWith('mes-')) {
      if (b.startsWith('mes-')) {
        return a.compareToIgnoreCase(b)
      } else {
        return -1
      }
    }
    if (b.startsWith('mes-')) {
      return 1
    }

    // Special case for micronaut modules
    if (a.startsWith('micronaut')) {
      if (b.startsWith('micronaut')) {
        return a.compareToIgnoreCase(b)
      } else {
        return -1
      }
    }
    if (b.startsWith('micronaut')) {
      return 1
    }

    return a.compareToIgnoreCase(b)
  }

}

