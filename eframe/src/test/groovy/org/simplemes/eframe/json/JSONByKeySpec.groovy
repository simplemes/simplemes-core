/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.json



import groovy.json.JsonSlurper
import org.simplemes.eframe.application.Holders
import org.simplemes.eframe.test.BaseSpecification
import org.simplemes.eframe.test.CompilerTestUtils
import org.simplemes.eframe.test.UnitTestUtils
import org.simplemes.eframe.test.annotation.Rollback
import sample.domain.Order

/**
 * Tests.
 */
class JSONByKeySpec extends BaseSpecification {

  static specNeeds = [SERVER]

  @Rollback
  def "verify that serializer works for simple case"() {
    given: 'a POGO with the annotation on a field'
    def src = """
    package sample
    import org.simplemes.eframe.json.JSONByKey
    import sample.domain.Order
    
    class SampleClass {
      String barcode
      @JSONByKey
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

    then: 'the JSON is simple key.'
    def json = new JsonSlurper().parseText(s)
    !s.contains('status')
    json.order == order.order
    json.barcode == 'XYZ'
  }

  @Rollback
  def "verify that serializer/deserializer work on round-trip for simple case"() {
    given: 'a POGO with the annotation on a field'
    def src = """
    package sample
    import org.simplemes.eframe.json.JSONByKey
    import sample.domain.Order
    
    class SampleClass {
      String barcode
      @JSONByKey
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

  @Rollback
  def "verify that serializer/deserializer work on round-trip when the field name is not a domain name"() {
    given: 'a POGO with the annotation on a field'
    def src = """
    package sample
    import org.simplemes.eframe.json.JSONByKey
    import sample.domain.Order
    
    class SampleClass {
      String barcode
      @JSONByKey
      Order theOrder
    }
    """
    def clazz = CompilerTestUtils.compileSource(src)

    and: 'a domain object'
    def order = new Order(order: 'ABC').save()

    and: 'a POGO to serialize'
    def o = clazz.newInstance()
    o.theOrder = order
    o.barcode = 'XYZ'

    when: 'the JSON is created'
    def s = Holders.objectMapper.writeValueAsString(o)
    //println "JSON = ${groovy.json.JsonOutput.prettyPrint(s)}"

    and: 'the object is re-created'
    def o2 = Holders.objectMapper.readValue(s, clazz)

    then: 'the deserialized object is correct'
    o2.theOrder == order
    o2.barcode == o.barcode
  }

  @Rollback
  def "verify that record not found is handled gracefully"() {
    given: 'a POGO with the annotation on a field'
    def src = """
    package sample
    import org.simplemes.eframe.json.JSONByKey
    import sample.domain.Order
    
    class SampleClass {
      String barcode
      @JSONByKey
      Order order
    }
    """
    def clazz = CompilerTestUtils.compileSource(src)

    and: 'JSON with record key for non-existent Order'
    def s = """ {
      "barcode": "XYZ",
      "order": "GIBBERISH"
    }
    """

    when: 'the JSON is converted to an object'
    Holders.objectMapper.readValue(s, clazz)

    then: 'the right exception is thrown'
    def ex = thrown(Exception)
    UnitTestUtils.assertExceptionIsValid(ex, ['@JSONByKey', 'not', 'record', 'order', 'GIBBERISH', 'sample.SampleClass'])
  }

  @Rollback
  def "verify that using the wrong field name is detected gracefully"() {
    given: 'a POGO with a bad field reference'
    def src = """
    package sample
    import org.simplemes.eframe.json.JSONByKey
    
    class SampleClass {
      String barcode
      @JSONByKey
      String orderX
    }
    """
    def clazz = CompilerTestUtils.compileSource(src)

    and: 'JSON with record key for non-existent Order'
    def s = """ {
      "barcode": "XYZ",
      "orderX": "ABC"
    }
    """

    when: 'the JSON is converted to an object'
    Holders.objectMapper.readValue(s, clazz)

    then: 'the right exception is thrown'
    def ex = thrown(Exception)
    UnitTestUtils.assertExceptionIsValid(ex, ['@JSONByKey', 'not', 'domain', 'orderX', 'sample.SampleClass'])
  }

  @Rollback
  def "verify that deserialize using a domain field name with the wrong field type fails gracefully"() {
    given: 'a POGO with a bad field reference'
    def src = """
    package sample
    import org.simplemes.eframe.json.JSONByKey
    
    class SampleClass {
      String barcode
      @JSONByKey
      String order
    }
    """
    def clazz = CompilerTestUtils.compileSource(src)

    and: 'a domain object'
    def order = new Order(order: 'ABC').save()

    and: 'JSON with record key for existing Order'
    def s = """ {
      "barcode": "XYZ",
      "order": "${order.order}"
    }
    """

    when: 'the JSON is converted to an object'
    Holders.objectMapper.readValue(s, clazz)

    then: 'the right exception is thrown'
    def ex = thrown(Exception)
    UnitTestUtils.assertExceptionIsValid(ex, ['String', 'order', 'sample.SampleClass'])
  }

  @Rollback
  def "verify that serialize using a domain field name with the wrong field type fails gracefully"() {
    given: 'a POGO with a bad field reference'
    def src = """
    package sample
    import org.simplemes.eframe.json.JSONByKey
    
    class SampleClass {
      String barcode
      @JSONByKey
      String order
    }
    """
    def clazz = CompilerTestUtils.compileSource(src)
    def o = clazz.newInstance()
    o.order = '1234'

    when: 'the JSON is converted to an object'
    Holders.objectMapper.writeValueAsString(o)

    then: 'the right exception is thrown'
    def ex = thrown(Exception)
    UnitTestUtils.assertExceptionIsValid(ex, ['field', 'String', 'domain'])
  }

  @Rollback
  def "verify that serializer works for null value"() {
    given: 'a POGO with the annotation on a field'
    def src = """
    package sample
    import org.simplemes.eframe.json.JSONByKey
    import sample.domain.Order
    
    class SampleClass {
      String barcode
      @JSONByKey
      Order order
    }
    """
    def clazz = CompilerTestUtils.compileSource(src)

    and: 'a POGO to serialize'
    def o = clazz.newInstance()
    o.barcode = 'XYZ'

    when: 'the JSON is created'
    def s = Holders.objectMapper.writeValueAsString(o)
    //println "JSON = ${groovy.json.JsonOutput.prettyPrint(s)}"

    then: 'the JSON is simple key.'
    def json = new JsonSlurper().parseText(s)
    json.order == null
    json.barcode == 'XYZ'
  }
}
