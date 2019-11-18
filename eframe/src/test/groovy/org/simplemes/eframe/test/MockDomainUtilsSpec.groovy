package org.simplemes.eframe.test

import org.simplemes.eframe.domain.DomainUtils
import sample.domain.AllFieldsDomain

/*
 * Copyright Michael Houston 2018. All rights reserved.
 * Original Author: mph
 *
*/

/**
 *
 */
class MockDomainUtilsSpec extends BaseSpecification {


  def "verify that gerPersistentFields returns the fields from the fieldOrder list"() {
    given: 'a mocked domainUtils'
    new MockDomainUtils(this, AllFieldsDomain).install()

    when: 'the persistent fields are retrieved'
    def fields = DomainUtils.instance.getPersistentFields(AllFieldsDomain)

    then: 'the list contains the right fields and types'
    //def field = fields.find { it.name == 'name' }
    fields.find { it.name == 'name' }.type == String
    fields.find { it.name == 'count' }.type == Integer
    fields.find { it.name == 'qty' }.type == BigDecimal

    and: 'the toString  methods work'
    fields.find { it.name == 'qty' }.toString() != null

  }
}
