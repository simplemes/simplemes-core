package sample.service


import grails.gorm.transactions.Transactional
import sample.domain.Order

import javax.inject.Singleton

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