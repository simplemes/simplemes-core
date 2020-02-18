/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.custom

import groovy.transform.ToString
import org.simplemes.eframe.data.format.BasicFieldFormat
import org.simplemes.eframe.system.BasicStatus

import javax.inject.Singleton

/**
 * Defines an internal addition for the framework that is used to specify some features
 * for the framework (e.g. BasicStatus codes, etc).
 */
@Singleton
@ToString(includeNames = true, includePackage = false)
class InternalAddition extends BaseAddition implements AdditionInterface {

  /**
   * Defines the elements needed/provided by this addition.
   */
  AdditionConfiguration addition = Addition.configure {
    encodedType BasicStatus
    encodedType BasicFieldFormat
  }

}
