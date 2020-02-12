/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.misc

import org.simplemes.eframe.test.BaseSpecification

/**
 * Tests.
 */
class UUIDUtilsSpec extends BaseSpecification {

  def "verify that convertToUUIDIfPossible"() {
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

  def "verify that isUUID works"() {
    expect: 'the test worked'
    UUIDUtils.isUUID(value) == results

    where:
    value                                  | results
    '71d361b0-fd23-42a3-abed-3d35c5790bed' | true
    '71d361b0-xd23-42a3-abed-3d35c5790bed' | false
    '71d361b0-fd23'                        | false
    'NotValidNotValidNotValidNotValidNote' | false
    ''                                     | false
    null                                   | false
  }

  def "verify that isUUID is fast enough"() {
    when: 'the method is called a lot'
    def start = System.currentTimeMillis()
    for (i in (1..10000)) {
      UUIDUtils.isUUID('71d361b0-fd23-42a3-abed-3d35c5790bed')
    }

    then: 'the time is reasonable'
    def elapsed = System.currentTimeMillis() - start
    elapsed < 100          // Actual is about ‭0.0025‬ms per call.
  }
}
