/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.misc

import io.micronaut.views.ModelAndView
import org.simplemes.eframe.controller.StandardModelAndView
import org.simplemes.eframe.security.domain.User
import org.simplemes.eframe.test.BaseSpecification
import org.simplemes.eframe.test.CompilerTestUtils
import sample.controller.AllFieldsDomainController
import sample.domain.AllFieldsDomain
import sample.domain.Order
import sample.domain.SampleParent
import spock.lang.Unroll

/**
 * Tests.
 */
class TypeUtilsSpec extends BaseSpecification {


  def "verify doesClassHaveStaticMethod works"() {
    expect:
    !TypeUtils.doesClassHaveStaticMethod(Order, 'initialDataLoad')
    TypeUtils.doesClassHaveStaticMethod(User, 'initialDataLoad')
  }

  def "verify that getStaticProperty works on an object"() {
    given: 'a class with a static property'
    def src = """
    package sample
    
    class SomeClass {
      static aProperty = 'ABC'
    }
    """
    def clazz = CompilerTestUtils.compileSource(src)

    when: ''
    def prop = TypeUtils.getStaticProperty(clazz.newInstance(), 'aProperty')

    then: ''
    prop == 'ABC'
  }

  def "verify that getStaticProperty works with a class passed in"() {
    given: 'a class with a static property'
    def src = """
    package sample
    
    class SomeClass {
      static belongsTo = 'ABC'
    }
    """
    def clazz = CompilerTestUtils.compileSource(src)

    expect: 'the method to find the property'
    TypeUtils.getStaticProperty(clazz, 'belongsTo') == 'ABC'

    and: 'the method is called for a property that does not exist'
    !TypeUtils.getStaticProperty(clazz, 'gibberish')
  }

  def "verify that toClassHierarchy works"() {
    when: 'the hierarchy is generated'
    def s = TypeUtils.toClassHierarchy(String)

    then: 'it is correct'
    s == """java.lang.Object\n java.lang.String"""
  }

  def "verify that toShortString works for supported cases"() {
    expect:
    TypeUtils.toShortString(input) == results

    where:
    input                                        | results
    null                                         | null
    'aString'                                    | 'aString'
    new SampleParent(name: 'ABC', title: 't')    | 'ABC'   // has toShortString()
    new AllFieldsDomain(name: 'XYZ', title: 't') | 'XYZ'   // Has no toShortString()
  }

  def "verify that toShortString works - add title option true"() {
    expect:
    TypeUtils.toShortString(input, true) == results

    where:
    input                                       | results
    null                                        | null
    'aString'                                   | 'aString'
    new SampleParent(name: 'ABC', title: 'xyz') | 'ABC (xyz)'   // has toShortString()
    new AllFieldsDomain(name: 'XYZ')            | 'XYZ'   // Has null title
  }

  def "verify that safeGetField works for supported cases"() {
    expect:
    def field = TypeUtils.safeGetField(clazz, name)
    if (results) {
      field?.name == results
    } else {
      field == null
    }

    where:
    clazz        | name        | results
    SampleParent | 'gibberish' | null
    SampleParent | 'name'      | 'name'
    SampleParent | 'xname'     | 'name'
  }

  def "verify getStaticPropertyInSuperClasses works with sub-classes"() {
    given: 'a sub-class with its own fieldOrder'
    def src = """
    package sample
    
    import sample.domain.SampleParent
    
    class SampleClass extends SampleParent {
      static fieldOrder = ['customField']
    }
    """
    def clazz = CompilerTestUtils.compileSource(src)

    when: 'the combined fieldOrder is found'
    def list = TypeUtils.getStaticPropertyInSuperClasses(clazz, 'fieldOrder')

    then: 'both lists are returned in the correct order - super-clas first'
    list[0] == SampleParent.fieldOrder
    list[1] == ['customField']
  }

  def "verify getStaticPropertyInSuperClasses works with sub-classes that do not have the static property"() {
    given: 'a sub-class with no fieldOrder'
    def src = """
    package sample
    
    import sample.domain.SampleParent
    
    class SampleClass extends SampleParent {
    }
    """
    def clazz = CompilerTestUtils.compileSource(src)

    when: 'the combined fieldOrder is found'
    def list = TypeUtils.getStaticPropertyInSuperClasses(clazz, 'fieldOrder')

    then: 'only one property is returned'
    list.size() == 1
    list[0] == SampleParent.fieldOrder
  }

  def "verify that getSuperClasses works"() {
    expect:
    TypeUtils.getSuperClasses(clazz).containsAll(results)

    where:
    clazz                | results
    SampleParent         | []
    StandardModelAndView | ModelAndView
  }

  def "verify that getDomain supports the intercepted form of the bean class"() {
    // Sometimes Micronaut registers the intercepted class, e.g. $SampleParentControllerDefinition$Intercepted.
    given: 'a mock controller'
    def src = '''
    package sample
    import sample.controller.AllFieldsDomainController
    
    class $AllFieldsDomainControllerDefinition$Intercepted extends AllFieldsDomainController {
    }
    '''
    def clazz = CompilerTestUtils.compileSource(src)


    expect: 'the correct domain class is found'
    TypeUtils.getRealClassFromBean(clazz) == AllFieldsDomainController
  }

  def "verify that getDomain leaves a non-intercepted class as-is"() {
    given: 'a mock controller'
    def src = '''
    package sample
    import sample.controller.AllFieldsDomainController
    
    class AllFieldsDomainController {
    }
    '''
    def clazz = CompilerTestUtils.compileSource(src)

    expect: 'the correct domain class is found'
    TypeUtils.getRealClassFromBean(clazz) == clazz
  }

  @Unroll
  def "verify that "() {
    expect: ''
    TypeUtils.isPrimitiveAssignableTo(clazz1, clazz2) == result

    where:
    clazz1  | clazz2  | result
    int     | Integer | true
    Integer | int     | true
    int     | int     | true
    long    | Long    | true
    Long    | long    | true
    long    | long    | true
    boolean | Boolean | true
    Boolean | boolean | true
    boolean | boolean | true
    Boolean | int     | false
    Boolean | Long    | false
  }
}
