package org.simplemes.eframe.domain.annotation

import org.simplemes.eframe.domain.annotation.DomainEntityInterface
import org.simplemes.eframe.test.BaseSpecification
import org.simplemes.eframe.test.CompilerTestUtils

/*
 * Copyright Michael Houston 2018. All rights reserved.
 * Original Author: mph
 *
*/

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
      }
    """
    def clazz = CompilerTestUtils.compileSource(src)

    when: 'an instance is created'
    def o = clazz.newInstance()

    then: 'the instance has the marker interface'
    o instanceof DomainEntityInterface

  }


}
