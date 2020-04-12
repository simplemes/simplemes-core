/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package sample.service

import org.simplemes.eframe.custom.annotation.ExtensionPoint
import sample.domain.Order

import javax.inject.Singleton
import javax.transaction.Transactional

/**
 * A test interface.
 */
interface OrderServiceInterface {
  Order save(Order order)
}

/**
 * A test interface for order release extension point..
 */
interface OrderReleaseInterface {
  String preRelease(String order)

  String postRelease(String response, String order)
}

@Singleton
class OrderService implements OrderServiceInterface {

  @Transactional
  Order save(Order order) {
    order.save()
  }

  @ExtensionPoint(value = OrderReleaseInterface, comment = "Sample Order Release Extension Point")
  String release(String order) {
    return 'released'
  }
}