package org.simplemes.eframe.archive

import grails.gorm.annotation.Entity
import org.simplemes.eframe.misc.FieldSizes

/*
 * Copyright Michael Houston. All rights reserved.
 *
*/

/**
 * This tracks archiving of old records no longer needed in the active database.  A record is written for each top-level
 * object that is archived.
 *
 */
@Entity
class ArchiveLog {
  /**
   * The date/time this record was archived.
   */
  Date dateArchived = new Date()

  /**
   * The record ID for the archive record.
   */
  Long recordID

  /**
   * The class of the domain object that was archived.
   */
  String className

  /**
   * The main key value(s) for the domain object that was archived.  Multiple values are separated by
   * {@link org.simplemes.eframe.misc.TextUtils#VALUE_SEPARATOR}. <b>Optional.</b>
   */
  String keyValue

  /**
   * The archive file reference.  This is used by the archive module to restore the record if needed.
   */
  String archiveReference

  /**
   * Internal constraints.
   */
  static constraints = {
    dateArchived(nullable: false)
    className(maxSize: FieldSizes.MAX_CLASS_NAME_LENGTH, nullable: false, blank: false)
    archiveReference(maxSize: FieldSizes.MAX_PATH_LENGTH, nullable: false, blank: false)
    keyValue(maxSize: FieldSizes.MAX_KEY_LENGTH, nullable: true, blank: true)
  }

  /**
   * This is searchable.
   */
  static searchable = true
  /**
   * Load initial records - test data.
   */
/*
  @SuppressWarnings("UnnecessaryQualifiedReference")
  static Map<String, List<String>> initialDataLoad() {
    ArchiveLog.withTransaction {
      if (ArchiveLog.list().size()==0) {
        def random = new Random()
        def date = new Date()-300
        for (i in 1901..2101) {
          def s = new SimpleDateFormat("yyyy-MM-dd").format(date)
          def fileName = "$s/M${i}.arc"
          new ArchiveLog(recordID: random.nextLong(),
                         className: 'org.simplemes.eframe.sample.Order',
                         dateArchived: date,
            keyValue: "M$i",
            archiveReference: fileName
          ).save()
          int adj = (int) DateUtils.MILLIS_PER_DAY*(0.75*random.nextDouble())
          date = new Date(date.time+adj)
        }
      }
    }
    return null
  }
*/

  /**
   * Create human readable string for this record.
   * @return The string.
   */
  @Override
  String toString() {
    return "ArchiveLog{" +
      ", className='" + className + '\'' +
      ", keyValue='" + keyValue + '\'' +
      ", archiveReference='" + archiveReference + '\'' +
      '}'
  }
}
