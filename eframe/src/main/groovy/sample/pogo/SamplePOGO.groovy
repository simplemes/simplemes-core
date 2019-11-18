package sample.pogo

import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString
import org.simplemes.eframe.date.DateOnly
import org.simplemes.eframe.json.TypeableJSONInterface

/*
 * Copyright Michael Houston 2018. All rights reserved.
 * Original Author: mph
 *
*/

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
}
