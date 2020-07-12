/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package sample.domain

import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString
import io.micronaut.data.annotation.AutoPopulated
import io.micronaut.data.annotation.Id
import io.micronaut.data.annotation.MappedEntity
import io.micronaut.data.annotation.MappedProperty
import io.micronaut.data.model.DataType
import org.simplemes.eframe.domain.annotation.DomainEntity

import javax.annotation.Nullable
import javax.persistence.ManyToOne

/**
 * A test/Sample grand child domain class.
 */
@MappedEntity
@DomainEntity
@ToString(includePackage = false, includeNames = true, excludes = ['sampleChild'])
@EqualsAndHashCode(includes = ['sampleChild', 'grandKey'])
class SampleGrandChild {

  // ********************************************************
  // * Note: Do not change these without running all tests.
  // *       These fields are used by many tests.
  // ********************************************************

  @ManyToOne
  @MappedProperty(type = DataType.UUID)
  SampleChild sampleChild

  String grandKey
  @Nullable String title
  @Id @AutoPopulated
  @MappedProperty(type = DataType.UUID)
  UUID uuid


  @SuppressWarnings("unused")
  static fieldOrder = ['grandKey', 'title']

}

