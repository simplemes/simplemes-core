package org.simplemes.eframe.custom

import org.simplemes.eframe.EFramePackage
import org.simplemes.eframe.data.format.BasicFieldFormat
import org.simplemes.eframe.system.BasicStatus
import org.simplemes.eframe.test.BaseSpecification

/*
 * Copyright Michael Houston 2019. All rights reserved.
 * Original Author: mph
 *
*/

/**
 *  Tests.
 */
class InternalAdditionSpec extends BaseSpecification {

  def "verify that the internal addition has the right top-level values"() {
    when: 'the addition is created'
    def addition = new InternalAddition()

    then: 'the top-level values are as expected'
    addition.domainPackageClasses == [EFramePackage]
    addition.encodedTypes == [BasicStatus, BasicFieldFormat]
    addition.initialDataLoaders == []
    addition.name == 'InternalAddition'
  }
}
