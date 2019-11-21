package org.simplemes.mes.floor.domain

import grails.gorm.annotation.Entity
import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString
import org.simplemes.mes.floor.WorkCenterStatus
import org.simplemes.mes.misc.FieldSizes

/**
 * Defines a work location or machine on the floor where work is performed.
 */
@Entity
@ToString(includeNames = true, includePackage = false, excludes = ['dateCreated', 'lastUpdated', 'errors', 'dirty', 'dirtyPropertyNames', 'attached'])
@EqualsAndHashCode(includes = ['workCenter'])
class WorkCenter {

  /**
   * The work center's name (key field).
   */
  String workCenter

  /**
   * The work center's name (short description).
   */
  String title

  /**
   * The Work Center's overall status.  This is one of the pre-defined WorkCenterStatus codes.
   */
  WorkCenterStatus overallStatus

  /**
   * The date this record was last updated.
   */
  Date lastUpdated

  /**
   * The date this record was created
   */
  Date dateCreated

  /**
   * <i>Internal definitions for GORM framework.</i>
   */
  static constraints = {
    workCenter(maxSize: FieldSizes.MAX_CODE_LENGTH, unique: true, nullable: false, blank: false)
    title(maxSize: FieldSizes.MAX_TITLE_LENGTH, nullable: true, blank: true)
    //workCenterComponents(nullable: true)
  }

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
