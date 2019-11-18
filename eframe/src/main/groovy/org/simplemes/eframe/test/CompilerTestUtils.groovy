package org.simplemes.eframe.test

import groovy.util.logging.Slf4j

/*
 * Copyright Michael Houston 2018. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Utility methods for testing annotations and other compiler features in unit tests.  Provides a way to compile
 * arbitrary code.
 */
@Slf4j
class CompilerTestUtils {

  /**
   * If set to true, then the source for the failed error is logged.
   */
  static boolean printCompileFailureSource = true

  /**
   * Convenience method that compiles a given source string for testing.
   * Used for unit testing compiler logic such as annotations.
   * @param src The groovy source.
   * @return The compiled class.
   */
  @SuppressWarnings(["SystemOutPrint", 'CatchException'])
  static Class compileSource(String src) {
    //println "src = $src"
    log.trace('src = {}', src)

    def clazz
    try {
      clazz = new GroovyClassLoader().parseClass(src)
    } catch (Exception e) {
      // We want to print the source if compile fails
      if (printCompileFailureSource) {
        System.out.println(src)
      }
      throw (e)
    }

    return clazz
  }

  /**
   * Convenience method that compiles a class for testing. The class is called TestClass.
   * This method builds the boiler plate around the class contents passed in.
   * Also sets any class level annotations.
   * <h3>Options</h3>
   * <ul>
   *   <li><b>contents</b> - The contents inside of the class (fields, methods, etc). </li>
   *   <li><b>annotation</b> - Annotations for the class itself. </li>
   * </ul>

   * @param options The groovy source of the class contents.  Everything inside of the class XYZ { . . .}.
   * @return The compiled class.
   */
  static Class compileSimpleClass(Map options) {
    def classContents = options.contents ?: ''
    def annotation = options.annotation ?: ''
    def src = """
      package sample

      import org.simplemes.ast.*
      import org.simplemes.eframe.data.annotation.ExtensibleFields
      import org.simplemes.eframe.custom.*
      import org.simplemes.eframe.custom.domain.FlexField
      import org.simplemes.eframe.custom.domain.FlexType

      $annotation
      class TestClass {
        $classContents
      }
    """

    return compileSource(src)

  }
}
