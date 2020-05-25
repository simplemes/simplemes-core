/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.security.domain

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString
import groovy.util.logging.Slf4j
import io.micronaut.data.annotation.AutoPopulated
import io.micronaut.data.annotation.DateCreated
import io.micronaut.data.annotation.DateUpdated
import io.micronaut.data.annotation.Id
import io.micronaut.data.annotation.MappedEntity
import io.micronaut.data.annotation.MappedProperty
import io.micronaut.data.annotation.Transient
import io.micronaut.data.model.DataType
import org.simplemes.eframe.application.Holders
import org.simplemes.eframe.domain.annotation.DomainEntity
import org.simplemes.eframe.misc.FieldSizes
import org.simplemes.eframe.security.PasswordEncoderService

import javax.annotation.Nullable
import javax.persistence.Column
import javax.persistence.ManyToMany

/**
 * The framework/security User definition.
 */
@Slf4j
@MappedEntity('usr')
@DomainEntity
@EqualsAndHashCode(includes = ['userName'])
@ToString(includePackage = false, includeNames = true, excludes = ['authoritySummary', 'password', 'encodedPassword'])
class User {
  /**
   * The user name (e.g. logon ID).
   */
  @Column(length = FieldSizes.MAX_CODE_LENGTH, nullable = false)   // TODO: Add unique to DDL.
  String userName

  /**
   * The display value (short) of this user.  Used for many displays/lists.
   */
  @Column(length = FieldSizes.MAX_TITLE_LENGTH)
  @Nullable String displayName

  /**
   * The raw password (un-encrypted, not saved).  Uses only when the password changes.
   */
  @JsonIgnore
  @Transient
  String password

  /**
   * The password (encrypted).
   */
  @JsonIgnore
  @Column(length = 128, nullable = false)
  String encodedPassword

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
  @Nullable @Column(length = FieldSizes.MAX_URL_LENGTH)
  String email

  /**
   * A list of the user roles this user has assigned in a comma-delimited form.
   * This is a transient value and is read from the roles and uses the title for display.
   */
  @JsonProperty("authoritySummary")
  @Transient
  String authoritySummary

  @ManyToMany(mappedBy = "userRole")
  List<Role> userRoles

  Integer version = 0

  @DateCreated
  @MappedProperty(type = DataType.TIMESTAMP, definition = 'TIMESTAMP WITH TIME ZONE')
  Date dateCreated

  @DateUpdated
  @MappedProperty(type = DataType.TIMESTAMP, definition = 'TIMESTAMP WITH TIME ZONE')
  Date dateUpdated

  /**
   * The internal unique ID for this record.
   */
  @Id @AutoPopulated UUID uuid


  /**
   * Defines the order the fields are shown in the edit/show/etc GUIs.
   */
  @SuppressWarnings("unused")
  static fieldOrder = ['userName', 'displayName', 'enabled', 'accountExpired', 'accountLocked', 'passwordExpired',
                       'email', 'userRoles']


  /**
   * Called before new records are created.
   */
  @SuppressWarnings("unused")
  def beforeValidate() {
    encodePassword()
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
      def adminUser = new User(userName: 'admin', password: 'admin', displayName: 'Admin User')
      def note = ""
      if (Holders.environmentDev || Holders.environmentTest) {
        adminUser.passwordExpired = false
        note = ", passwordExpired=false"
      } else {
        // Production environments get a default admin user with an expired password.
        adminUser.passwordExpired = true
      }
      // Add all roles to admin
      adminUser.userRoles = []
      for (role in Role.list()) {
        adminUser.userRoles << role
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
  static PasswordEncoderService passwordEncoder

  /**
   * Finds the right encoder to use.  Works in live server and tests.
   * @return The encoder.
   */
  static PasswordEncoderService getPasswordEncoder() {
    if (!passwordEncoder) {
      passwordEncoder = Holders.applicationContext?.getBean(PasswordEncoderService) ?: new PasswordEncoderService()
    }
    return passwordEncoder
  }

  /**
   * Encodes the raw password before saving, if needed.
   */
  protected void encodePassword() {
    if (password) {
      encodedPassword = getPasswordEncoder().encode(password)
      // Clear the raw password so it is not in memory.
      password = null
    }
  }

  /**
   * Determines if the given password matches the user's current password.
   * @param secret The password to check.
   * @return true if it matches.
   */
  boolean passwordMatches(String secret) {
    return getPasswordEncoder().matches(secret, encodedPassword)
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
    for (userRole in getUserRoles()) {
      if (sb) {
        sb << ', '
      }
      sb << userRole.toString()
    }
    authoritySummary = sb.toString()
    return authoritySummary
  }

}
