package org.simplemes.mes.assy.demand

import org.simplemes.eframe.i18n.GlobalUtils
import org.simplemes.eframe.test.BaseSpecification

/*
 * Copyright Michael Houston 2020. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Tests.
 */
class AssemblyModeEnumSpec extends BaseSpecification {

  def "verify that toString works for all core codes"() {
    expect: "all enums build toString() as expected"
    for (t in AssemblyModeEnum.enumConstants) {
      assert t.toString() == t.name()
    }
  }

  def "verify toLocalizedString works and all core settings have values in messages.properties"() {
    expect: "all enums build toString() as expected"
    def key = "assemblyMode.${code.name()}.label"
    def localized = GlobalUtils.lookup(key, locale)
    code.toStringLocalized() == localized
    !localized.contains(key)

    where:
    locale        | code
    Locale.GERMAN | AssemblyModeEnum.NONE
    Locale.GERMAN | AssemblyModeEnum.COMPLETE
    Locale.GERMAN | AssemblyModeEnum.START
    Locale.GERMAN | AssemblyModeEnum.START_COMPLETE
    Locale.US     | AssemblyModeEnum.START_COMPLETE
  }
}
