package org.simplemes.mes.tracking

import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString

/*
 * Copyright Michael Houston. All rights reserved.
 *
*/

/**
 * The request archive old ProductionLog records.  Used for the archiveOld() method.
 */
@ToString(includeNames = true, includePackage = false)
@EqualsAndHashCode(includes = ['ageDays', 'batchSize', 'delete'])
class ProductionLogArchiveRequest {

  /**
   * The age (in days) used to determine if a record is an 'old' record and eligible for archiving/deleting.
   * Supports fractions (<b>Required</b>).
   */
  BigDecimal ageDays

  /**
   * The size of the batch used when archiving these records.  This determines the database transaction size
   * and the size of the archive XML file (if records are not deleted).  (<b>Default:</b> 500)
   */
  BigDecimal batchSize = 500

  /**
   * If true, then the records are deleted, not archived (<b>Default:</b> false).
   */
  Boolean delete = false

}
