package org.simplemes.eframe.data.annotation


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
class ExtensibleFieldsSpec extends BaseSpecification {

  def "verify that the annotation creates the main storage field with getter and setter"() {
    given:
    def src = """
      import org.simplemes.eframe.data.annotation.ExtensibleFields
      
      @ExtensibleFields
      class TestClass {
      }
    """
    def clazz = CompilerTestUtils.compileSource(src)

    when: 'the object is created and the value is set'
    def o = clazz.newInstance()
    o._customFields = 'ABC'

    then: 'the storage field is created in the object'
    o._customFields == 'ABC'

    and: 'the setter and getter work'
    o.set_customFields('XYZ')
    o.get_customFields() == 'XYZ'
  }


}
