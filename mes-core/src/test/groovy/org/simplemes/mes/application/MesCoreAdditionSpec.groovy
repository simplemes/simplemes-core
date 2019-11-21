package org.simplemes.mes.application

import org.simplemes.eframe.test.BaseSpecification
import org.simplemes.mes.MESCorePackage
import org.simplemes.mes.demand.LSNStatus
import org.simplemes.mes.demand.OrderStatus
import org.simplemes.mes.floor.WorkCenterStatus
import org.simplemes.mes.system.InitialData

/*
 * Copyright Michael Houston 2018. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Tests.
 */
class MesCoreAdditionSpec extends BaseSpecification {

  def "verify that getEncodedTypes contains the right types"() {
    expect:
    new MesCoreAddition().encodedTypes.containsAll([LSNStatus, OrderStatus, WorkCenterStatus])
  }

  def "verify that getTopLevelDomainClasses contains the right classes"() {
    expect:
    new MesCoreAddition().domainPackageClasses.containsAll([MESCorePackage])
  }

  def "verify that getInitialDataLoaders contains the right classes"() {
    expect:
    new MesCoreAddition().initialDataLoaders.containsAll([InitialData])
  }

  // TODO: Port Search additions from old mes-core module.
}
