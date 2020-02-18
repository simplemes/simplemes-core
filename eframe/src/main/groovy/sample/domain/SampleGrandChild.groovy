/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package sample.domain

import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString
import io.micronaut.data.annotation.AutoPopulated
import io.micronaut.data.annotation.Id
import io.micronaut.data.annotation.MappedEntity
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
  SampleChild sampleChild
  String grandKey
  @Nullable String title
  @Id @AutoPopulated UUID uuid


  @SuppressWarnings("unused")
  static fieldOrder = ['grandKey', 'title']

}

