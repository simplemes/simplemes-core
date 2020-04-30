/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package sample.pogo

import groovy.transform.ToString
import org.simplemes.eframe.custom.domain.FlexType

import javax.persistence.ManyToOne

/**
 * The sample response from the OrderController.findComponents() method.
 * Represents a sample component list.
 */
@SuppressWarnings("unused")
@ToString(includeNames = true, includePackage = false)
class FindComponentResponseDetail {

  /**
   * A unique ID for the row.
   */
  def id

  /**
   * The component.
   */
  String component

  /**
   * The display sequence.
   */
  int sequence = 10

  /**
   * The qty required.
   */
  BigDecimal qtyRequired

  /**
   * The qty assembled.
   */
  BigDecimal qtyAssembled

  /**
   * True if row can be removed.
   */
  boolean canBeRemoved

  /**
   * True if row can be assembled.
   */
  boolean canBeAssembled = true

  @ManyToOne(targetEntity = FlexType)
  FlexType assemblyData


}
