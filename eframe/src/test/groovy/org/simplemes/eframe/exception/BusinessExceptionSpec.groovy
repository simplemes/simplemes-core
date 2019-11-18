package org.simplemes.eframe.exception

import org.simplemes.eframe.test.BaseSpecification

/*
 * Copyright Michael Houston 2018. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Tests.
 */
class BusinessExceptionSpec extends BaseSpecification {

  def "verify that constructors works with the expected arguments"() {
    expect: 'no errors'
    new BusinessException().toString()
    new BusinessException(code: 100, params: ['Order']).toString()
    new BusinessException(100, ['Order']).toString()
  }

}
