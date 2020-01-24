/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package sample.domain

import groovy.transform.ToString
import io.micronaut.data.annotation.AutoPopulated
import io.micronaut.data.annotation.DateCreated
import io.micronaut.data.annotation.DateUpdated
import io.micronaut.data.annotation.Id
import io.micronaut.data.annotation.MappedEntity
import io.micronaut.data.annotation.MappedProperty
import io.micronaut.data.model.DataType
import org.simplemes.eframe.domain.annotation.DomainEntity

import javax.annotation.Nullable
import javax.persistence.ManyToMany
import javax.persistence.ManyToOne
import javax.persistence.OneToMany

/**
 * Tests of sub-class-like behavior using interfaces. Adds a field subTitle to the SampleParent.
 */

@MappedEntity
@DomainEntity
@ToString(includeNames = true, includePackage = false)
@SuppressWarnings("unused")
class SampleSubClass implements SampleParentInterface {

  String name
  @Nullable String title
  @Nullable String notes
  @Nullable String notDisplayed
  @Nullable String moreNotes = 'Default Notes'

  @DateCreated
  @MappedProperty(type = DataType.TIMESTAMP, definition = 'TIMESTAMP WITH TIME ZONE') Date dateCreated

  @DateUpdated
  @MappedProperty(type = DataType.TIMESTAMP, definition = 'TIMESTAMP WITH TIME ZONE')
  Date dateUpdated


  /**
   * A reference to another domain object.
   */
  @Nullable @ManyToOne(targetEntity = AllFieldsDomain) AllFieldsDomain allFieldsDomain

  /**
   * A list of foreign references.
   */
  @ManyToMany(mappedBy = "sample_parent_afd")
  List<AllFieldsDomain> allFieldsDomains = []

  /**
   * A list of children.
   */
  @OneToMany(mappedBy = "sampleParent")
  List<SampleChild> sampleChildren

  @Id @AutoPopulated UUID uuid

  static fieldOrder = SampleParent.fieldOrder << 'subTitle'
  static keys = SampleParent.keys

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
