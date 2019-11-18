package sample

import groovy.transform.ToString

/*
 * Copyright Michael Houston 2018. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * The start request POGO (sample).
 */
@ToString(includeNames = true, includePackage = false)
class StartRequest {
  String order
  BigDecimal qty = 1.0
}
