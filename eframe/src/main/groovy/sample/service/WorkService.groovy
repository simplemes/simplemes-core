package sample.service

import org.simplemes.eframe.application.Holders
import sample.ProductionInterface
import sample.StartRequest
import sample.StartResponse
import sample.domain.Order

import javax.inject.Singleton
import javax.transaction.Transactional

/*
 * Copyright Michael Houston 2018. All rights reserved.
 * Original Author: mph
 *
*/

interface WorkServiceInterface {
  StartResponse start(StartRequest request)
}

/**
 * A sample service to perform work.
 */
@SuppressWarnings("GrMethodMayBeStatic")
@Singleton
class WorkService {

  /**
   * Simulate work start method.
   * @param request
   * @return
   */
  @Transactional
  StartResponse start(StartRequest request, boolean fail) {
    def order = Order.findByOrder(request.order)
    order.qtyToBuild += request.qty
    order.save()
    def response = new StartResponse(order: request.order, qty: request.qty)


    def allExtensions = Holders.applicationContext.getBeansOfType(ProductionInterface) as ProductionInterface[]
    //println "allExtensions = $allExtensions"
    for (extension in allExtensions) {
      extension.started(request, response, fail)
    }

    return response
  }
}
