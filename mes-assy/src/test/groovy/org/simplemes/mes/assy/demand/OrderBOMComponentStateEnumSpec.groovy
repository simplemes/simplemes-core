package org.simplemes.mes.assy.demand

import org.simplemes.eframe.i18n.GlobalUtils
import org.simplemes.eframe.test.BaseSpecification

/*
 * Copyright Michael Houston 2016. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Test the enum class.
 */
class OrderBOMComponentStateEnumSpec extends BaseSpecification {

  def "test toString()"() {
    expect: "all enums build toString() as expected"
    for (t in OrderComponentStateEnum.enumConstants) {
      assert t.toString() == t.name()
    }
  }

  def "test toLocalizedString()"() {
    expect: "all enums build toString() as expected"
    def locale = Locale.GERMAN
    OrderComponentStateEnum.EMPTY.toStringLocalized(locale) == GlobalUtils.lookup("orderComponentState.EMPTY.label", locale)
    OrderComponentStateEnum.PARTIAL.toStringLocalized(locale) == GlobalUtils.lookup("orderComponentState.PARTIAL.label", locale)
    OrderComponentStateEnum.FULL.toStringLocalized(locale) == GlobalUtils.lookup("orderComponentState.FULL.label", locale)
    OrderComponentStateEnum.OVER.toStringLocalized(locale) == GlobalUtils.lookup("orderComponentState.OVER.label", locale)
  }

}
