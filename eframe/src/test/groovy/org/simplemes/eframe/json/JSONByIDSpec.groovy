package org.simplemes.eframe.json



import groovy.json.JsonSlurper
import org.simplemes.eframe.application.Holders
import org.simplemes.eframe.test.BaseSpecification
import org.simplemes.eframe.test.CompilerTestUtils
import org.simplemes.eframe.test.UnitTestUtils
import sample.domain.Order

/*
 * Copyright Michael Houston 2018. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Tests.
 */
class JSONByIDSpec extends BaseSpecification {

  static specNeeds = [JSON, SERVER]

  //TODO: Find alternative to @Rollback
  def "verify that serializer works for simple case"() {
    given: 'a POGO with the annotation on a field'
    def src = """
    package sample
    import org.simplemes.eframe.json.JSONByID
    import sample.domain.Order
    
    class SampleClass {
      String barcode
      @JSONByID
      Order order
    }
    """
    def clazz = CompilerTestUtils.compileSource(src)

    and: 'a domain object'
    def order = new Order(order: 'ABC').save()

    and: 'a POGO to serialize'
    def o = clazz.newInstance()
    o.order = order
    o.barcode = 'XYZ'

    when: 'the JSON is created'
    def s = Holders.objectMapper.writeValueAsString(o)
    //println "JSON = ${groovy.json.JsonOutput.prettyPrint(s)}"

    then: 'the JSON is simple ID.'
    def json = new JsonSlurper().parseText(s)
    !s.contains('status')
    json.order == order.id
    json.barcode == 'XYZ'
  }

  //TODO: Find alternative to @Rollback
  def "verify that serializer/deserializer work on round-trip for simple case"() {
    given: 'a POGO with the annotation on a field'
    def src = """
    package sample
    import org.simplemes.eframe.json.JSONByID
    import sample.domain.Order
    
    class SampleClass {
      String barcode
      @JSONByID
      Order order
    }
    """
    def clazz = CompilerTestUtils.compileSource(src)

    and: 'a domain object'
    def order = new Order(order: 'ABC').save()

    and: 'a POGO to serialize'
    def o = clazz.newInstance()
    o.order = order
    o.barcode = 'XYZ'

    when: 'the JSON is created'
    def s = Holders.objectMapper.writeValueAsString(o)
    //println "JSON = ${groovy.json.JsonOutput.prettyPrint(s)}"

    and: 'the object is re-created'
    def o2 = Holders.objectMapper.readValue(s, clazz)

    then: 'the deserialized object is correct'
    o2.order == o.order
    o2.barcode == o.barcode
  }

  //TODO: Find alternative to @Rollback
  def "verify that record not found is handled gracefully"() {
    given: 'a POGO with the annotation on a field'
    def src = """
    package sample
    import org.simplemes.eframe.json.JSONByID
    import sample.domain.Order
    
    class SampleClass {
      String barcode
      @JSONByID
      Order order
    }
    """
    def clazz = CompilerTestUtils.compileSource(src)

    and: 'JSON with record ID for non-existent Order'
    def s = """ {
      "barcode": "XYZ",
      "order": 989896
    }
    """

    when: 'the JSON is converted to an object'
    Holders.objectMapper.readValue(s, clazz)

    then: 'the right exception is thrown'
    def ex = thrown(Exception)
    UnitTestUtils.assertExceptionIsValid(ex, ['@JSONByID', 'not', 'record', 'order', '989896', 'sample.SampleClass'])
  }

  //TODO: Find alternative to @Rollback
  def "verify that using the wrong field name is detected gracefully"() {
    given: 'a POGO with a bad field reference'
    def src = """
    package sample
    import org.simplemes.eframe.json.JSONByID
    
    class SampleClass {
      String barcode
      @JSONByID
      String orderX
    }
    """
    def clazz = CompilerTestUtils.compileSource(src)

    and: 'JSON with record ID for non-existent Order'
    def s = """ {
      "barcode": "XYZ",
      "orderX": 989896
    }
    """

    when: 'the JSON is converted to an object'
    Holders.objectMapper.readValue(s, clazz)

    then: 'the right exception is thrown'
    def ex = thrown(Exception)
    UnitTestUtils.assertExceptionIsValid(ex, ['@JSONByID', 'not', 'domain', 'orderX', 'sample.SampleClass'])
  }

  //TODO: Find alternative to @Rollback
  def "verify that deserialize using a domain field name with the wrong field type fails gracefully"() {
    given: 'a POGO with a bad field reference'
    def src = """
    package sample
    import org.simplemes.eframe.json.JSONByID
    
    class SampleClass {
      String barcode
      @JSONByID
      String order
    }
    """
    def clazz = CompilerTestUtils.compileSource(src)

    and: 'a domain object'
    def order = new Order(order: 'ABC').save()

    and: 'JSON with record ID for non-existent Order'
    def s = """ {
      "barcode": "XYZ",
      "order": ${order.id}
    }
    """

    when: 'the JSON is converted to an object'
    Holders.objectMapper.readValue(s, clazz)

    then: 'the right exception is thrown'
    def ex = thrown(Exception)
    UnitTestUtils.assertExceptionIsValid(ex, ['String', 'type', 'mismatch', 'order', Order.name, 'sample.SampleClass'])
  }

  //TODO: Find alternative to @Rollback
  def "verify that serialize using a domain field name with the wrong field type fails gracefully"() {
    given: 'a POGO with a bad field reference'
    def src = """
    package sample
    import org.simplemes.eframe.json.JSONByID
    
    class SampleClass {
      String barcode
      @JSONByID
      String order
    }
    """
    def clazz = CompilerTestUtils.compileSource(src)
    def o = clazz.newInstance()
    o.order = 1234

    when: 'the JSON is converted to an object'
    Holders.objectMapper.writeValueAsString(o)

    then: 'the right exception is thrown'
    def ex = thrown(Exception)
    UnitTestUtils.assertExceptionIsValid(ex, ['id', 'property', 'sample.SampleClass'])
  }

}
