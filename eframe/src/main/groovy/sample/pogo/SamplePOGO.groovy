/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package sample.pogo

import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString
import org.simplemes.eframe.custom.domain.FlexType
import org.simplemes.eframe.data.annotation.ExtensibleFieldHolder
import org.simplemes.eframe.date.DateOnly
import org.simplemes.eframe.json.TypeableJSONInterface

/**
 * A simple POGO for testing.  Has most support field types.
 * Has the fields: name, title, qty, count, enabled, dateTime, dueDate.
 */
@ToString(includeNames = true, includePackage = false)
@EqualsAndHashCode
class SamplePOGO implements TypeableJSONInterface {
  String name
  String title
  BigDecimal qty
  Integer count
  Boolean enabled
  Date dateTime
  DateOnly dueDate

  // Test support for configurable fields in a POGO.
  FlexType assemblyData
  @ExtensibleFieldHolder
  String customFields

}
