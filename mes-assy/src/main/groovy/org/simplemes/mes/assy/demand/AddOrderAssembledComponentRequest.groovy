package org.simplemes.mes.assy.demand

import groovy.transform.ToString
import org.simplemes.eframe.custom.domain.FlexType
import org.simplemes.eframe.data.annotation.ExtensibleFieldHolder
import org.simplemes.eframe.json.JSONByKey
import org.simplemes.eframe.misc.NameUtils
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
  @JSONByKey
  Order order

  /**
   * The lsn the component was assembled on (<b>Optional</b>).
   */
  @JSONByKey
  LSN lsn

  /**
   * The Sequence for the BOM component being assembled (<b>Optional</b>).
   */
  Integer bomSequence

  /**
   * The work center the component was assembled in (<b>Optional</b>).
   */
  @JSONByKey
  WorkCenter workCenter

  /**
   * The component Product to be added to the order (<b>Required or a bomSequence required</b>).
   * Ignored if the bomSequence is >0.
   */
  @JSONByKey
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
   * <p>
   * <b>Note:</b> This is required if there are custom fields to be processed.  Otherwise, the custom fields
   * will be ignored.
   * See the mes-assy module reference for the OrderAssyService.addComponent() method in
   * <a href="http://docs.simplemes.org/latest/guide.html">docs.simplemes.org</a> for details.
   *
   */
  @JSONByKey
  FlexType assemblyData

  /**
   * The holder for the customer-defined assembly data.
   */
  @ExtensibleFieldHolder
  String fields

}
