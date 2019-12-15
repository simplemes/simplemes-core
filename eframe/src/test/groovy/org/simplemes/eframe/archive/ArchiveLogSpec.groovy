package org.simplemes.eframe.archive

import org.simplemes.eframe.misc.FieldSizes
import org.simplemes.eframe.test.BaseSpecification
import org.simplemes.eframe.test.DomainTester

/*
 * Copyright Michael Houston 2018. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Tests.
 */
class ArchiveLogSpec extends BaseSpecification {

  static specNeeds = [SERVER]

  def "verify that user domain enforces constraints"() {
    expect: 'the constraints are enforced'
    DomainTester.test {
      domain ArchiveLog
      requiredValues recordID: 123L, className: 'ABC', keyValue: 'M1001', archiveReference: 'ref'
      maxSize 'className', FieldSizes.MAX_CLASS_NAME_LENGTH
      maxSize 'archiveReference', FieldSizes.MAX_PATH_LENGTH
      maxSize 'keyValue', FieldSizes.MAX_KEY_LENGTH
      notNullCheck 'className'
      notNullCheck 'archiveReference'
      fieldOrderCheck false  // No fieldOrder in this object.
    }
  }

}
