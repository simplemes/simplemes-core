package sample.service


import sample.domain.Order

import javax.inject.Singleton
import javax.transaction.Transactional

/*
 * Copyright Michael Houston 2017. All rights reserved.
 * Original Author: mph
 *
*/

/**
 *
 */
interface OrderServiceInterface {
  Order save(Order order)
}

@Singleton
class OrderService implements OrderServiceInterface {

  @Transactional
  Order save(Order order) {
    order.save()
  }
}