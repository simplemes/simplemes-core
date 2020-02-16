/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.domain.annotation

import org.simplemes.eframe.domain.annotation.DomainEntityInterface
import org.simplemes.eframe.test.BaseSpecification
import org.simplemes.eframe.test.CompilerTestUtils

/**
 * Tests.
 */
class DomainEntitySpec extends BaseSpecification {

  def "verify that the annotation adds the DomainEntityInterface for singleton use in the runtime"() {
    given:
    def src = """
      import org.simplemes.eframe.domain.annotation.DomainEntity
      
      @DomainEntity
      class TestClass {
        UUID uuid
      }
    """
    def clazz = CompilerTestUtils.compileSource(src)

    when: 'an instance is created'
    def o = clazz.newInstance()

    then: 'the instance has the marker interface'
    o instanceof DomainEntityInterface

  }


}
