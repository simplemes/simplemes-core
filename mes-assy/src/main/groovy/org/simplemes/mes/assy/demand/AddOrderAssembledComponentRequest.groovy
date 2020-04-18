package org.simplemes.mes.assy.demand

import groovy.transform.ToString
import org.simplemes.eframe.custom.domain.FlexType
import org.simplemes.eframe.data.annotation.ExtensibleFieldHolder
import org.simplemes.eframe.misc.NameUtils
import org.simplemes.mes.assy.demand.domain.OrderBOMComponent
import org.simplemes.mes.demand.domain.LSN
import org.simplemes.mes.demand.domain.Order
import org.simplemes.mes.floor.domain.WorkCenter
import org.simplemes.mes.product.domain.Product

/*
 * Copyright Michael Houston 2016. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * This class is the request to add a component to a work center's current setup.
 */
@ToString(includeNames = true, includePackage = false)
class AddOrderAssembledComponentRequest {

  /**
   * The order the component was assembled on (<b>Required</b>).
   */
  Order order

  /**
   * The lsn the component was assembled on (<b>Optional</b>).
   */
  LSN lsn

  /**
   * The BOM Component from the order's list of components (<b>Optional</b>).
   */
  OrderBOMComponent orderBOMComponent

  /**
   * The work center the component was assembled in (<b>Optional</b>).
   */
  WorkCenter workCenter

  /**
   * The component Product to be added to the order (<b>Required</b>).
   */
  Product component

  /**
   * The location in the work center that the component came from (<b>Optional</b>).
   */
  String location = NameUtils.DEFAULT_KEY

  /**
   * The number of pieces added (<b>Default 1.0</b>).  Will use the BOM Component.qty if provided.
   */
  BigDecimal qty

  /**
   * The FlexType used to define the assembly data for this component (<b>Optional</b>).
   */
  FlexType assemblyData

  /**
   * The holder for the customer-defined assembly data.
   */
  @ExtensibleFieldHolder
  String customFields

}
