package sample.domain

import io.micronaut.data.annotation.MappedProperty
import io.micronaut.data.model.DataType

/*
 * Copyright Michael Houston 2019. All rights reserved.
 * Original Author: mph
 *
*/

/**
 *
 */
class BaseWIPEntity {
  @MappedProperty(type = DataType.BIGDECIMAL/*, definition = 'TIMESTAMP WITH TIME ZONE'*/)
  BigDecimal qtyInQueue = 0.0
  @MappedProperty(type = DataType.BIGDECIMAL/*, definition = 'TIMESTAMP WITH TIME ZONE'*/)
  BigDecimal qtyInWork = 0.0
  @MappedProperty(type = DataType.BIGDECIMAL/*, definition = 'TIMESTAMP WITH TIME ZONE'*/)
  BigDecimal qtyDone = 0.0

  BigDecimal start() {
    return qtyInQueue
  }
}
