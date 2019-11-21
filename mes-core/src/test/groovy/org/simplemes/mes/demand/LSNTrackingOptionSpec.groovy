package org.simplemes.mes.demand

import org.simplemes.eframe.i18n.GlobalUtils
import spock.lang.Specification

/*
 * Copyright Michael Houston 2017. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Tests.
 */
class LSNTrackingOptionSpec extends Specification {

  def "test toStringLocalized with various locales"() {
    expect: 'the various enum toString() works'
    enumValue.toStringLocalized(locale) == GlobalUtils.lookup("lsnTrackingOption.${enumValue.id}.label", null, locale)

    where: 'all enums with several locales are tested'
    locale        | enumValue
    Locale.US     | LSNTrackingOption.ORDER_ONLY
    Locale.GERMAN | LSNTrackingOption.ORDER_ONLY
    Locale.US     | LSNTrackingOption.LSN_ALLOWED
    Locale.GERMAN | LSNTrackingOption.LSN_ALLOWED
    Locale.US     | LSNTrackingOption.LSN_ONLY
    Locale.GERMAN | LSNTrackingOption.LSN_ONLY
  }

  def "test flags"() {
    expect: 'the flags are set correctly'
    enumValue.isLSNAllowed() == lsnAllowed
    enumValue.isOrderAllowed() == orderAllowed

    where: 'all enums are tested with their flag values'
    enumValue                     | lsnAllowed | orderAllowed
    LSNTrackingOption.LSN_ALLOWED | true       | true
    LSNTrackingOption.ORDER_ONLY  | false      | true
    LSNTrackingOption.LSN_ONLY    | true       | false
  }

  def "the expected enums are defined"() {
    // This test exists to make sure any new options add to the enum are covered in the tests above
    // Update the tests above if this test fails
    expect: 'the right number'
    LSNTrackingOption.values().size() == 3

    and: 'the right values exist'
    LSNTrackingOption.values().contains(LSNTrackingOption.LSN_ALLOWED)
    LSNTrackingOption.values().contains(LSNTrackingOption.LSN_ONLY)
    LSNTrackingOption.values().contains(LSNTrackingOption.ORDER_ONLY)
  }
}
