/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.custom

import groovy.util.logging.Slf4j
import org.simplemes.eframe.misc.TypeUtils
import org.yaml.snakeyaml.Yaml

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
   * A list of additions found by the framework.
   */
  List<AdditionInterface> additions

  /**
   * Returns the list of additions.
   * @return The list.
   */
  @SuppressWarnings("GroovyAssignabilityCheck")
  List<AdditionInterface> getAdditions() {
    if (additions == null) {
      def resources = getClass().classLoader.getResources('efBootstrap.yml')
      def res = []
      for (url in resources) {
        def inputStream = url.openStream()
        def list = getAdditions(inputStream, url.toString())
        res.addAll(list)
      }
      additions = res
      log.debug("getAdditions(): additions found {} ", additions)
    }
    return additions
  }


  /**
   * Returns the list of additions from the given input stream
   * @param inputStream The input stream for the .yml file.
   * @param source The name of the source (.yml file).
   * @return The list.
   */
  @SuppressWarnings("GroovyAssignabilityCheck")
  List<AdditionInterface> getAdditions(InputStream inputStream, String source) {
    List<AdditionInterface> res = []
    try {
      Yaml yaml = new Yaml()
      Iterable<Object> objects = yaml.loadAll(inputStream)
      for (o in objects) {
        if (o.eframe && o.eframe.additions) {
          for (className in o.eframe.additions) {
            try {
              def clazz = TypeUtils.loadClass(className)
              res << clazz.newInstance()
            } catch (Exception e) {
              // Ignore the sample code addition.  It is not shipped with the module
              if (className != 'sample.SampleAddition') {
                log.error('Could not load .yml file {}.  Exception: {}', source, e)
              }
            }
          }
        }
      }
    } finally {
      inputStream?.close()
    }
    return res
  }


}
