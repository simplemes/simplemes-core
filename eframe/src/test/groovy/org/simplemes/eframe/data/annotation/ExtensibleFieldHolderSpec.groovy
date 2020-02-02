/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.data.annotation


import org.simplemes.eframe.test.BaseSpecification
import org.simplemes.eframe.test.CompilerTestUtils

/**
 * Tests.
 */
class ExtensibleFieldHolderSpec extends BaseSpecification {

  def "verify that the annotation uses the correct transformation"() {
    given:
    def src = """
      import org.simplemes.eframe.data.annotation.ExtensibleFieldHolder
      import org.simplemes.eframe.domain.annotation.DomainEntity

      @DomainEntity
      class TestClass {
        @ExtensibleFieldHolder
        String customFields
        UUID uuid
      }
    """
    def clazz = CompilerTestUtils.compileSource(src)

    expect: 'the setFieldValue method exists'
    clazz.getDeclaredMethods().find() { it.name == "setFieldValue" }
  }


}
