/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.archive.domain

import org.simplemes.eframe.archive.domain.ArchiveLog
import org.simplemes.eframe.misc.FieldSizes
import org.simplemes.eframe.test.BaseSpecification
import org.simplemes.eframe.test.DomainTester

/**
 * Tests.
 */
class ArchiveLogSpec extends BaseSpecification {

  @SuppressWarnings("unused")
  static specNeeds = SERVER

  def "verify that user domain enforces constraints"() {
    expect: 'the constraints are enforced'
    DomainTester.test {
      domain ArchiveLog
      requiredValues recordUUID: UUID.randomUUID(), className: 'ABC', keyValue: 'M1001', archiveReference: 'ref'
      maxSize 'className', FieldSizes.MAX_CLASS_NAME_LENGTH
      maxSize 'archiveReference', FieldSizes.MAX_PATH_LENGTH
      maxSize 'keyValue', FieldSizes.MAX_KEY_LENGTH
      notNullCheck 'className'
      notNullCheck 'archiveReference'
      fieldOrderCheck false  // No fieldOrder in this object.
    }
  }

}
