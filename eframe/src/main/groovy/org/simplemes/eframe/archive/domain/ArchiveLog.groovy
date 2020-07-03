/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.archive.domain

import com.fasterxml.jackson.annotation.JsonFilter
import groovy.transform.EqualsAndHashCode
import io.micronaut.data.annotation.AutoPopulated
import io.micronaut.data.annotation.Id
import io.micronaut.data.annotation.MappedEntity
import io.micronaut.data.annotation.MappedProperty
import io.micronaut.data.model.DataType
import org.simplemes.eframe.domain.annotation.DomainEntity
import org.simplemes.eframe.misc.FieldSizes

import javax.persistence.Column

/**
 * This tracks archiving of old records no longer needed in the active database.  A record is written for each top-level
 * object that is archived.
 *
 */
@MappedEntity
@DomainEntity
@JsonFilter("searchableFilter")
@EqualsAndHashCode(includes = ['uuid'])
class ArchiveLog {

  /**
   * The date/time this record was archived.
   */
  @MappedProperty(type = DataType.TIMESTAMP, definition = 'TIMESTAMP WITH TIME ZONE')
  Date dateArchived = new Date()

  /**
   * The record UUID for the archived record.
   */
  UUID recordUUID

  /**
   * The class of the domain object that was archived.
   */
  @Column(length = FieldSizes.MAX_CLASS_NAME_LENGTH, nullable = false)
  String className

  /**
   * The main key value(s) for the domain object that was archived.  Multiple values are separated by
   * {@link org.simplemes.eframe.misc.TextUtils#VALUE_SEPARATOR}. <b>Optional.</b>
   */
  @Column(length = FieldSizes.MAX_KEY_LENGTH, nullable = true)
  String keyValue

  /**
   * The archive file reference.  This is used by the archive module to restore the record if needed.
   */
  @Column(length = FieldSizes.MAX_PATH_LENGTH, nullable = false)
  String archiveReference

  @Id @AutoPopulated UUID uuid

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
          new ArchiveLog(recordUUID: UUID.randomUUID(),
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
