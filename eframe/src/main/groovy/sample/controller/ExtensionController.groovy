package sample.controller

import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Error
import io.micronaut.http.annotation.Get
import org.simplemes.eframe.application.Holders
import sample.StartRequest
import sample.domain.Order
import sample.service.WorkService

import javax.annotation.Nullable

/*
 * Copyright Michael Houston 2018. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Test controller for extension mechanism.
 */
@Controller("/sample/extension")
class ExtensionController {
// TODO: Delete soon.  Remove from home.ftl.
  @SuppressWarnings(["GroovyMissingReturnStatement", "GrMethodMayBeStatic"])
  @Get('read')
  String read() {
    def result = 'no order found'
    Order.withTransaction {
      def order = Order.findByOrder('ABC')
      if (order) {
        result = order.toString()
      }
    }
    return result
  }

  @SuppressWarnings(["GroovyMissingReturnStatement", "GrMethodMayBeStatic"])
  @Get('test{?fail}')
  String test(@Nullable Boolean fail) {
    if (fail == null) {
      fail = false
    }

    Order.withTransaction {
      def order = Order.findByOrder('ABC')
      if (!order) {
        new Order(order: 'ABC').save()
      }
    }

    // Now perform the start
    def res = 'unknown status'
    //def orderService = Holders.applicationContext.getBean(OrderService)
    def workService = Holders.applicationContext.getBean(WorkService)
    workService.start(new StartRequest(order: 'ABC', qty: 1.2), fail)
    Order.withTransaction {
      def order = Order.findByOrder('ABC')
      res = order.toString()
    }

    return res
  }

  @Error
  HttpResponse error(HttpRequest request, Exception exception) {
    exception.printStackTrace()
    return HttpResponse.status(HttpStatus.BAD_REQUEST, exception.toString())
  }
}
