package org.simplemes.eframe.custom

import groovy.transform.ToString
import org.simplemes.eframe.EFramePackage
import org.simplemes.eframe.data.format.BasicFieldFormat
import org.simplemes.eframe.system.BasicStatus

/*
 * Copyright Michael Houston 2018. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Defines an internal addition for the framework that is used to specify some features
 * for the framework (e.g. BasicStatus codes, etc).
 */
@ToString(includeNames = true, includePackage = false)
class InternalAddition extends BaseAddition implements AdditionInterface {

  /**
   * Defines the elements needed/provided by this addition.
   */
  AdditionConfiguration addition = Addition.configure {
    domainPackage EFramePackage
    encodedType BasicStatus
    encodedType BasicFieldFormat
  }

}
