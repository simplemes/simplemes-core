/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.security.domain

import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString
import groovy.util.logging.Slf4j
import io.micronaut.data.annotation.AutoPopulated
import io.micronaut.data.annotation.DateCreated
import io.micronaut.data.annotation.DateUpdated
import io.micronaut.data.annotation.Id
import io.micronaut.data.annotation.MappedEntity
import io.micronaut.data.annotation.MappedProperty
import io.micronaut.data.model.DataType
import org.simplemes.eframe.domain.annotation.DomainEntity
import org.simplemes.eframe.misc.FieldSizes

import javax.annotation.Nullable
import javax.persistence.Column
import javax.validation.constraints.NotNull

/**
 * The domain object for refresh token persistence.
 */
@Slf4j
@MappedEntity
@DomainEntity
@EqualsAndHashCode(includes = ['refreshToken'])
@ToString(includePackage = false, includeNames = true)
class RefreshToken {
  /**
   * The refresh token (the encrypted/encoded form).
   */
  @MappedProperty(definition = "TEXT")
  String refreshToken

  /**
   * The user name (e.g. logon ID).
   */
  @Column(length = FieldSizes.MAX_CODE_LENGTH, nullable = false)
  String userName

  /**
   * If true, then this token can be used to create
   */
  @NotNull
  Boolean enabled = true

  /**
   * The number of times this token was used to refresh the access token.
   */
  Integer useAttemptCount = 0

  /**
   * The source (IP/page/etc) that the refresh request came from.
   */
  @Nullable
  @MappedProperty(definition = "TEXT")
  String requestSource

  @MappedProperty(type = DataType.TIMESTAMP, definition = 'TIMESTAMP WITH TIME ZONE')
  Date expirationDate

  @DateCreated
  @SuppressWarnings('unused')
  @MappedProperty(type = DataType.TIMESTAMP, definition = 'TIMESTAMP WITH TIME ZONE')
  Date dateCreated

  @DateUpdated
  @SuppressWarnings('unused')
  @MappedProperty(type = DataType.TIMESTAMP, definition = 'TIMESTAMP WITH TIME ZONE')
  Date dateUpdated

  /**
   * The internal unique ID for this record.
   */
  @Id @AutoPopulated
  @MappedProperty(type = DataType.UUID)
  UUID uuid


  /**
   * Defines the order the fields are shown in the edit/show/etc GUIs.
   */
  @SuppressWarnings("unused")
  static fieldOrder = ['refreshToken', 'userName', 'enabled', 'expirationDate', 'useAttemptCount', 'requestSource']


}
