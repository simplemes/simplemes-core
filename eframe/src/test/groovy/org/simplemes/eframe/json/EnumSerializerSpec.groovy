/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.json

import org.simplemes.eframe.application.Holders
import org.simplemes.eframe.test.BaseSpecification
import org.simplemes.eframe.test.CompilerTestUtils

/**
 *
 */
class EnumSerializerSpec extends BaseSpecification {

  @SuppressWarnings('unused')
  static specNeeds = [SERVER]

  def "verify that serializer works with enums without toStringLocalized"() {
    given: 'a domain object with an enum'
    def src = """
      import org.simplemes.eframe.domain.annotation.DomainEntity
      import org.simplemes.eframe.json.NoLocalizedEnum
      
      @DomainEntity
      class TestClass {
        UUID uuid
        NoLocalizedEnum enumValue
      }
    """
    def o = CompilerTestUtils.compileSource(src).getConstructor().newInstance()
    o.enumValue = NoLocalizedEnum.TODAY

    when: 'an enum is serialized'
    def s = Holders.objectMapper.writeValueAsString(o)

    then: 'the value is used'
    s.contains('"enumValue":"TODAY"')
  }


}

/**
 * Test enum without toStringLocalized()
 */
enum NoLocalizedEnum {
  TODAY()

}

