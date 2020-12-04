/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.application

import org.simplemes.eframe.misc.ClassPathUtils
import org.simplemes.eframe.test.BaseSpecification

/**
 *  Tests.
 */
class ModuleUtilsSpec extends BaseSpecification {

  void cleanup() {
    ClassPathUtils.instance = new ClassPathUtils()
  }

  def "verify that getModules finds the expected modules"() {
    when: ''
    def list = ModuleUtils.instance.modules

    then: 'the list is returned'
    list.size() > 0

    and: 'the webix module is listed with a version'
    def webix = list.find { it.startsWith('webix') }
    webix
    webix =~ /:[\d]+[\.(\d)+]+/
  }

  /**
   * Builds a valid module list from the short list.
   * @param list A list (e.g. ['abc-2.1.1','pdq-2.1.1','webix-8.1.1']).
   * @return The list with .jar and the File.separator added for a legal .jar file name.
   */
  List<String> toModule(List<String> list) {
    def res = []
    for (s in list) {
      res << "/${s}.jar"
    }

    return res
  }

  def "verify that getModules sorts important modules first"() {
    given: 'a dummy module list of modules'
    ClassPathUtils.instance = Mock(ClassPathUtils)
    ClassPathUtils.instance.getJarFiles() >> toModule(['abc-2.1.1', 'pdq-2.1.1', "$importantModule-8.1.1"])

    when: 'the list is generated'
    def list = ModuleUtils.instance.modules

    then: 'the important module is first'
    list[0].contains(importantModule)

    where:
    importantModule | _
    'webix'         | _
    'eframe'        | _
    'mes-core'      | _
    'mes-assy'      | _
    'mes-xyzzy'     | _
  }

  def "verify that getModules sorts micronaut modules as important"() {
    given: 'a dummy module list of modules'
    ClassPathUtils.instance = Mock(ClassPathUtils)
    ClassPathUtils.instance.getJarFiles() >> toModule(['abc-2.1.1', "micronaut-jdbc-8.1.1", "micronaut-data-8.1.1", "micronaut-abc-8.1.1"])

    when: 'the list is generated'
    def list = ModuleUtils.instance.modules

    then: 'the micronaut modules are first, in the right order'
    list[0].contains('micronaut-abc')
    list[1].contains('micronaut-data')
    list[2].contains('micronaut-jdbc')
  }

  def "verify that getModules ignores intellij jars"() {
    given: 'a dummy module list of modules'
    ClassPathUtils.instance = Mock(ClassPathUtils)
    ClassPathUtils.instance.getJarFiles() >> toModule(['abc-2.1.1', 'idea_rt.jar', 'junit5-rt.jar', 'junit-rt.jar'])

    when: 'the list is generated'
    def list = ModuleUtils.instance.modules

    then: 'the intellij jars are not in the list'
    list.size() == 1
    list[0].contains('abc')
  }

}
