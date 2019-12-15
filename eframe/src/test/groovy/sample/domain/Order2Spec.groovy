package sample.domain


import io.micronaut.test.annotation.MicronautTest
import org.simplemes.eframe.application.Holders
import spock.lang.Specification

/*
 * Copyright Michael Houston 2019. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Test.
 */
@MicronautTest
class Order2Spec extends Specification {
  // TODO: Delete

  //@Inject Order2Repository order2Repository

  def setupSpec() {
    //def start = System.currentTimeMillis()
    //def server = ApplicationContext.run(EmbeddedServer)
    //println "server = $server"
    println "all = ${Holders.applicationContext.getAllBeanDefinitions()*.name}"
  }


  def "verify that repository works"() {
    Order2Repository order2Repository = Holders.applicationContext.getBean(Order2Repository)
    when: ''
    Order2 order2 = new Order2("M1001")
    println "order2X = $order2"
    println "all = ${Holders.applicationContext.allBeanDefinitions*.name}"
    order2Repository.save(order2)
    //println "order2Repository = $order2Repository"

    then: ''
    def order2Read = order2Repository.findById(order2.uuid).orElse(null)
    println "order2Read = $order2Read"
    order2Read.order == 'M1001'

    and: 'the record can be deleted'
    order2Repository.deleteById(order2.uuid)
    def order2Read2 = order2Repository.findById(order2.uuid).orElse(null)
    order2Read2 == null
  }
}
