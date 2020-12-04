/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.misc

/**
 * Utilities to process the class path definitions.
 */
class ClassPathUtils {

  /**
   * A singleton, used for simplified unit testing with a mocked class.
   */
  static ClassPathUtils instance = new ClassPathUtils()

  /**
   * Gets a list of the .jar files from the classpath.  Uses the META-INF/MANIFEST.MF file to find them all.
   * @return The list.
   */
  List<String> getJarFiles() {
    def res = []
    def suffix = "!/META-INF/MANIFEST.MF"
    def list = this.class.classLoader.getResources('META-INF/MANIFEST.MF')
    for (l in list) {
      def s = l.toString()
      if (s.contains(suffix)) {
        res << s - suffix
      }
    }

    return res
  }

}
