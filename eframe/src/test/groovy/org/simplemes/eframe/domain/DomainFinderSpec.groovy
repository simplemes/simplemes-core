package org.simplemes.eframe.domain

import org.simplemes.eframe.EFramePackage
import org.simplemes.eframe.test.BaseSpecification
import sample.domain.SampleParent

/*
 * Copyright Michael Houston 2018. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Tests.
 */
class DomainFinderSpec extends BaseSpecification {

  def "verify that getDomainClasses works"() {
    when: 'the classes are read from the efBootstrap.yml file'
    def classes = new DomainFinder().topLevelDomainClasses

    then: 'the core package is in the list'
    classes.find { it == EFramePackage }

    and: 'the test package is in the list'
    classes.find { it == SampleParent }
  }


}
