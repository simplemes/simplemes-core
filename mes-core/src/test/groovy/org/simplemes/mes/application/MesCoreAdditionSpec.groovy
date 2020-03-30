package org.simplemes.mes.application

import org.simplemes.eframe.test.BaseSpecification
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

  def "verify that getInitialDataLoaders contains the right classes"() {
    expect:
    new MesCoreAddition().initialDataLoaders.containsAll([InitialData])
  }

  def "verify that getAssets contains the mes dashboard assets"() {
    when: 'the addition is loaded'
    def assets = new MesCoreAddition().assets

    then: 'the correct assets are in the list'
    assets.size() == 2
    and: 'the assets are correct'
    assets[0].page == 'dashboard/index'
    assets[0].script == '/assets/mes_dashboard.js'
    assets[0].css == null
    assets[1].page == 'dashboard/index'
    assets[1].script == null
    assets[1].css == '/assets/mes_dashboard.css'
  }

}
