package org.simplemes.eframe.domain.annotation

import org.simplemes.eframe.test.BaseSpecification
import org.simplemes.eframe.test.CompilerTestUtils
import sample.domain.Order2
import sample.domain.Order2Repository

/*
 * Copyright Michael Houston 2019. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Tests.
 */
class DomainEntityHelperSpec extends BaseSpecification {

  @SuppressWarnings("unused")
  static dirtyDomains = [Order2]
  //static specNeeds= SERVER

  def "verify that getRepository works for simple case"() {
    expect: 'the repository is found'
    DomainEntityHelper.instance.getRepository(Order2) instanceof Order2Repository
  }

  def "verify that the annotation adds the static repository holder"() {
    when: ' the class is compiled'
    def src = """
      import org.simplemes.eframe.domain.annotation.DomainEntity
      
      @DomainEntity(repository=sample.domain.Order2Repository)
      class TestClass {
        UUID uuid
      }
    """
    def clazz = CompilerTestUtils.compileSource(src)
    //println "clazz = ${clazz.declaredFields}"
    //println "clazz = ${clazz.declaredMethods}"

    then: 'the field has the repository '
    clazz.repository instanceof Order2Repository
  }

  def "verify that save will create a record"() {
    when: ' the record is saved'
    def order = new Order2('M1001')
    DomainEntityHelper.instance.save((DomainEntityInterface) order)

    then: 'the record is in the DB'
    def list = (List<Order2>) DomainEntityHelper.instance.list(Order2)
    list[0].uuid == order.uuid
    println "list = $list"
  }

}
