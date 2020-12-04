/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.misc

import org.simplemes.eframe.test.BaseSpecification

/**
 * Tests.
 */
class ClassPathUtilsSpec extends BaseSpecification {

  def "verify that getJarFiles works"() {
    when: 'the jar files are processed'
    def list = ClassPathUtils.instance.jarFiles

    then: 'the list is populated'
    list.size() > 0

    and: 'a known .jar file is in the list'
    list.find { it.contains('webix-') }
  }
}
