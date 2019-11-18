package org.simplemes.eframe.security.domain

import grails.gorm.annotation.Entity
import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString
import groovy.util.logging.Slf4j
import io.micronaut.security.authentication.providers.PasswordEncoder
import org.simplemes.eframe.application.Holders
import org.simplemes.eframe.data.annotation.ExtensibleFields
import org.simplemes.eframe.misc.FieldSizes
import org.simplemes.eframe.security.PasswordEncoderService

/*
 * Copyright Michael Houston 2018. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * The framework/security User definition.
 */
@Slf4j
@Entity
@EqualsAndHashCode(includes = ['userName'])
@SuppressWarnings("unused")
@ToString(includePackage = false, includeNames = true, excludes = ['authoritySummary', 'errors',
  'dirtyPropertyNames', 'attached', 'dirty'])
@ExtensibleFields
class User {
  /**
   * The user name (e.g. logon ID).
   */
  String userName

  /**
   * The title (short) of this user.  Used for many displays/lists.
   */
  String title

  /**
   * The password (encrypted).
   */
  String password

  /**
   * If true, then this user is enabled.
   */
  boolean enabled = true

  /**
   * If true, then this user account has expired.
   */
  boolean accountExpired

  /**
   * If true, then this user account has been locked.
   */
  boolean accountLocked

  /**
   * If true, then the password is expired and must be changed on next login attempt.
   */
  boolean passwordExpired

  /**
   * This user's email.
   */
  String email

  /**
   * A list of the user roles this user has assigned in a comma-delimited form.
   * This is a transient value and is read from the roles and uses the title for display.
   */
  String authoritySummary


/**
 * The date this record was last updated.
 */
  Date lastUpdated

  /**
   * The date this record was created
   */
  Date dateCreated


  static hasMany = [userRoles: Role]

  /**
   * Internal field constraints.
   */
  static constraints = {
    userName(maxSize: FieldSizes.MAX_CODE_LENGTH, blank: false, unique: true)
    password(maxSize: 128, blank: false)
    title(maxSize: FieldSizes.MAX_TITLE_LENGTH, nullable: true, blank: true)
    email(maxSize: FieldSizes.MAX_URL_LENGTH, nullable: true, blank: true, email: true)
  }

  /**
   * Defines the order the fields are shown in the edit/show/etc GUIs.
   */
  static fieldOrder = ['userName', 'title', 'enabled', 'accountExpired', 'accountLocked', 'passwordExpired',
                       'email', 'userRoles']

  /**
   * Internal mappings.  This object is stored in the USR table.  
   */
  @SuppressWarnings("SpellCheckingInspection")
  static mapping = {
    table 'usr'
    password column: 'passwd'
    userRoles lazy: false
  }
  /**
   * Internal transient field list.
   */
  static transients = ['authoritySummary']


  /**
   * Called before new records are created.
   */
  def beforeInsert() {
    encodePassword()
  }

  /**
   * Called before a record is updated.
   */
  def beforeUpdate() {
    if (isDirty('password')) {
      encodePassword()
    }
  }

  /**
   * A list of the records created by the initial data load.
   * Used only for test cleanup by {@link org.simplemes.eframe.test.BaseSpecification}.
   */
  static initialDataRecords = ['User': ['admin']]


  /**
   * Load initial user records, only if there are no user records in the DB.
   */
  static Map<String, List<String>> initialDataLoad() {
    if (!findByUserName('admin')) {
      def adminUser = new User(userName: 'admin', password: 'admin', title: 'Admin User')
      def note = ""
      if (Holders.environmentDev || Holders.environmentTest) {
        adminUser.passwordExpired = false
        note = ", passwordExpired=false"
      } else {
        // Production environments get a default admin user with an expired password.
        adminUser.passwordExpired = true
      }
      // Add all roles to admin
      for (role in Role.list()) {
        adminUser.addToUserRoles(role)
      }

      adminUser.save()
      log.debug('Saving initial admin user "{}" {}', adminUser, note)
    }
    return initialDataRecords
  }


  /**
   * The password encoder to use.  Since injection is not desired on domains, we do it ourselves
   * and cache the encoder for later use.
   */
  static PasswordEncoder passwordEncoder

  /**
   * Finds the right encoder to use.  Works in live server and tests.
   * @return The encoder.
   */
  static PasswordEncoder getPasswordEncoder() {
    if (!passwordEncoder) {
      passwordEncoder = Holders.applicationContext?.getBean(PasswordEncoder) ?: new PasswordEncoderService()
    }
    return passwordEncoder
  }

  /**
   * Encodes the password before saving.
   */
  protected void encodePassword() {
    password = getPasswordEncoder().encode(password)
  }

  /**
   * Determines if the given password matches the user's current password.
   * @param secret The password to check.
   * @return true if it matches.
   */
  boolean passwordMatches(String secret) {
    return getPasswordEncoder().matches(secret, password)
  }

  /**
   * Finds all of the user roles this user has assigned and returns it as a comma-delimited list of roles (titles).
   * This reads the roles from the roles for the user and formats them for display.
   * @return The list of roles as a string.
   */
  String getAuthoritySummary() {
    if (authoritySummary) {
      return authoritySummary
    }
    StringBuilder sb = new StringBuilder()
    for (userRole in userRoles) {
      if (sb) {
        sb << ', '
      }
      sb << userRole.authority
    }

    authoritySummary = sb.toString()
    return authoritySummary
  }

}
