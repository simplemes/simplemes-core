/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.misc

import org.simplemes.eframe.test.BaseSpecification

/**
 * Tests.
 */
class UUIDUtilsSpec extends BaseSpecification {

  def "verify that "() {
    expect: 'the conversion worked'
    UUIDUtils.convertToUUIDIfPossible(value) == results

    where:
    value                                                   | results
    '71d361b0-fd23-42a3-abed-3d35c5790bed'                  | UUID.fromString('71d361b0-fd23-42a3-abed-3d35c5790bed')
    UUID.fromString('71d361b0-fd23-42a3-abed-3d35c5790bed') | UUID.fromString('71d361b0-fd23-42a3-abed-3d35c5790bed')
    'NotValidNotValidNotValidNotValidNote'                  | 'NotValidNotValidNotValidNotValidNote'
    'NotValid-NotValid-NotValid-NotValid-'                  | 'NotValid-NotValid-NotValid-NotValid-'
    'NotValid'                                              | 'NotValid'
    ''                                                      | ''
    null                                                    | null

  }
}
