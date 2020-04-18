package org.simplemes.mes.assy.demand

import groovy.transform.ToString
import org.simplemes.eframe.custom.domain.FlexType
import org.simplemes.eframe.data.annotation.ExtensibleFieldHolder
import org.simplemes.eframe.misc.NameUtils

/*
 * Copyright Michael Houston 2016. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * This class contains the fields for a single row in the component assembly state for an order/LSN.
 */
@ToString(includeNames = true, includePackage = false)
class OrderComponentState {
  /**
   * The order to display the results in.  Generally bomSequence, but a value is assigned to non-BOM entries.
   */
  Integer sequence

  /**
   * The list of OrderAssembledComponent.sequence values that make up this record.  Allows quick removal.
   */
  List<Integer> sequencesForRemoval = []

  /**
   * The component Product required or assembled.
   */
  String component

  /**
   * The component Product with optional title that is required or assembled.
   */
  String componentAndTitle

  /**
   * The location in the work center that the component came from (<b>Optional</b>).
   */
  String location = NameUtils.DEFAULT_KEY

  /**
   * The number of pieces assembled.
   */
  BigDecimal qtyAssembled

  /**
   * The number of pieces required for this order/LSN.
   */
  BigDecimal qtyRequired

  /**
   * The FlexType used to define the assembly data for this component (<b>Optional</b>).
   */
  FlexType assemblyData

  /**
   * The holder for the customer-defined assembly data.
   */
  @ExtensibleFieldHolder
  String customFields


  /**
   * A String form of the assembly data values, with HTML formatting. If multiple different assembly records are found, then the
   * strings are displayed together (up to a total limit of 300 chars).
   */
  String assemblyDataAsString

  /**
   * The overall state of this component as a string.  Usually localized for the current request.
   */
  String overallStateString

  /**
   * The overall state as an ENUM.
   */
  OrderComponentStateEnum overallState

  /**
   * The percent assembled (can be greater than 100).
   */
  Integer percentAssembled

  /**
   * The qtys and overall state of this component as a string.  For example: '0/1 Empty'
   */
  String qtyAndStateString

}
