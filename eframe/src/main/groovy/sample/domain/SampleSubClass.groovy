/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package sample.domain

import groovy.transform.ToString
import io.micronaut.data.annotation.AutoPopulated
import io.micronaut.data.annotation.Id
import io.micronaut.data.annotation.MappedEntity
import org.simplemes.eframe.domain.annotation.DomainEntity

import javax.annotation.Nullable

/**
 * Tests of sub-class. Adds a field subTitle to the SampleParent.
 */

@MappedEntity
@DomainEntity
@ToString(includeNames = true, includePackage = false)
class SampleSubClass /*extends SampleParent*/ { // TODO: Figure out how to implement common domain fields?

  @Nullable String subTitle

  @Id @AutoPopulated UUID uuid

  @SuppressWarnings("unused")
  static fieldOrder = ['subTitle']

  /**
   * Load initial records.  Dummy test records for test mode only.
   */
  static initialDataLoad() {
/*
    if (allowInitialDataLoad && !findByName('SAMPLE_SUB')) {
      new SampleSubClass(name: 'SAMPLE_SUB').save()
    }
*/
  }


}
