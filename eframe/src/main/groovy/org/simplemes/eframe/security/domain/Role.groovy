package org.simplemes.eframe.security.domain

import grails.gorm.annotation.Entity
import groovy.transform.EqualsAndHashCode
import groovy.util.logging.Slf4j
import org.simplemes.eframe.misc.FieldSizes

/*
 * Copyright Michael Houston 2018. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Defines a Role for security.  These roles are used by the access points (controllers) to determine which users
 * can have access to the system.
 */
@Slf4j
@Entity
@EqualsAndHashCode(includes = ['authority'])
@SuppressWarnings("unused")
class Role {

  /**
   * The Role name (authority).  Example: 'ADMIN'.
   */
  String authority

  /**
   * The short title of this object.
   */
  String title

  /**
   * Internal field constraints.
   */
  static constraints = {
    authority(maxSize: FieldSizes.MAX_CODE_LENGTH, blank: false, unique: true)
    title(maxSize: FieldSizes.MAX_TITLE_LENGTH, blank: false, nullable: false)
  }

  /**
   *  Build a human-readable version of this object.
   * @return The human-readable string.
   */
  @Override
  String toString() {
    return title
  }

  /**
   * Defines the domain(s) this initial data must be loaded before/after.
   */
  static initialDataLoadBefore = [User]

  /**
   * A list of the records created by the initial data load.
   * Used only for test cleanup by {@link org.simplemes.eframe.test.BaseSpecification}.
   */
  static initialDataRecords = [Role: ['Admin', 'Customizer', 'Manager', 'Designer']]


  /**
   * Load initial Role records.  Will make sure all of the needed roles exist.
   */
  static Map<String, List<String>> initialDataLoad() {
    def total = 0
    total += createRoleIfNeeded('ADMIN', 'Admin')
    total += createRoleIfNeeded('CUSTOMIZER', 'Customizer')
    total += createRoleIfNeeded('MANAGER', 'Manager')
    total += createRoleIfNeeded('DESIGNER', 'Designer')
    if (total) {
      log.debug("Created {} Role records", total)
    }
    return initialDataRecords
  }

  /**
   * Creates the role if it doesn't exist in the DB.
   * @param authority The role.
   * @param title The title.
   * return Number of records created.
   */
  static int createRoleIfNeeded(String authority, String title) {
    if (!findByAuthority(authority)) {
      new Role(authority: authority, title: title).save()
      return 1
    }
    return 0

  }


}
