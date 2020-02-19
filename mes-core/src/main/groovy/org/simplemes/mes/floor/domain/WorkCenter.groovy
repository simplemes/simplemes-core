package org.simplemes.mes.floor.domain

import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString
import io.micronaut.data.annotation.AutoPopulated
import io.micronaut.data.annotation.DateCreated
import io.micronaut.data.annotation.DateUpdated
import io.micronaut.data.annotation.Id
import io.micronaut.data.annotation.MappedEntity
import io.micronaut.data.annotation.MappedProperty
import io.micronaut.data.model.DataType
import org.simplemes.eframe.domain.annotation.DomainEntity
import org.simplemes.mes.floor.WorkCenterStatus
import org.simplemes.mes.misc.FieldSizes

import javax.persistence.Column

/**
 * Defines a work location or machine on the floor where work is performed.
 */
@MappedEntity
@DomainEntity
@ToString(includeNames = true, includePackage = false, excludes = ['dateCreated', 'dateUpdated'])
@EqualsAndHashCode(includes = ['workCenter'])
class WorkCenter {

  /**
   * The work center's name (key field).
   */
  // TODO: DDL Add unique constraint and non null on workCenter.
  @Column(length = FieldSizes.MAX_CODE_LENGTH, nullable = false)
  @MappedProperty(type = DataType.STRING, definition = 'VARCHAR(30) UNIQUE')
  String workCenter

  /**
   * The work center's name (short description).
   */
  @Column(length = org.simplemes.eframe.misc.FieldSizes.MAX_TITLE_LENGTH, nullable = true)
  String title

  /**
   * The Work Center's overall status.  This is one of the pre-defined WorkCenterStatus codes.
   */
  WorkCenterStatus overallStatus

  @DateCreated
  @MappedProperty(type = DataType.TIMESTAMP, definition = 'TIMESTAMP WITH TIME ZONE')
  Date dateCreated

  @DateUpdated
  @MappedProperty(type = DataType.TIMESTAMP, definition = 'TIMESTAMP WITH TIME ZONE')
  Date dateUpdated

  Integer version = 0

  @Id @AutoPopulated UUID uuid

  /**
   * Defines the default general field ordering for GUIs and other field listings/reports.
   */
  @SuppressWarnings("GroovyUnusedDeclaration")
  static fieldOrder = ['workCenter', 'title', 'overallStatus']

  /**
   * Called before validate happens.  Used to set the description if needed.
   */
  def beforeValidate() {
    // Set the status if one is not provided.
    overallStatus = overallStatus ?: WorkCenterStatus.default
  }

}
