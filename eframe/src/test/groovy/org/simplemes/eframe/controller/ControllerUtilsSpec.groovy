/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.controller

import ch.qos.logback.classic.Level
import org.simplemes.eframe.application.EFrameConfiguration
import org.simplemes.eframe.application.Holders
import org.simplemes.eframe.domain.DomainUtils
import org.simplemes.eframe.test.BaseSpecification
import org.simplemes.eframe.test.CompilerTestUtils
import org.simplemes.eframe.test.MockAppender
import org.simplemes.eframe.test.MockControllerUtils
import org.simplemes.eframe.test.MockDomainUtils
import org.simplemes.eframe.test.UnitTestUtils
import org.simplemes.eframe.web.task.TaskMenuControllerUtils
import org.simplemes.eframe.web.task.TaskMenuItem
import org.simplemes.eframe.web.ui.UIDefaults
import sample.controller.AllFieldsDomainController
import sample.controller.RMAController
import sample.controller.SampleParentController
import sample.domain.AllFieldsDomain
import sample.domain.SampleParent
import sample.pogo.FindWorkResponseDetail

/**
 * Tests.
 */
class ControllerUtilsSpec extends BaseSpecification {

  def "verify that getDomain can find by controller class name"() {
    given: 'a mock list of domains'
    DomainUtils.instance = Mock(DomainUtils)
    DomainUtils.instance.allDomains >> [SampleParent]

    and: 'a mock controller'
    def src = """
    package sample
    
    class SampleParentController {
    }
    """
    def clazz = CompilerTestUtils.compileSource(src)

    expect: 'the correct domain class is found'
    ControllerUtils.instance.getDomainClass(clazz.newInstance()) == SampleParent
  }

  def "verify that getDomain supports static domainClass value is supported"() {
    given: 'a mock controller'
    def src = """
    package sample
    
    import sample.domain.SampleParent
    
    class NotSampleParentController {
      static domainClass = SampleParent
    }
    """
    def clazz = CompilerTestUtils.compileSource(src)

    expect: 'the correct domain class is found'
    ControllerUtils.instance.getDomainClass(clazz.newInstance()) == SampleParent
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

    and: 'a mocked domain utils'
    new MockDomainUtils(this, AllFieldsDomain).install()

    expect: 'the correct domain class is found'
    ControllerUtils.instance.getDomainClass(clazz.newInstance()) == AllFieldsDomain
  }

  def "verify calculateFromAndSizeForList works for basic cases"() {
    expect:
    ControllerUtils.instance.calculateFromAndSizeForList(map) == result

    where:
    map                      | result
    [from: 8, size: 10]      | [8, 10]
    [from: '8', size: '10']  | [8, 10]
    [start: 11, count: 10]   | [1, 10]
    [start: 20, count: 10]   | [2, 10]
    [start: '10', count: 10] | [1, 10]
    [start: '11', count: 10] | [1, 10]
    [start: '20', count: 10] | [2, 10]
    [from: 888, size: 9999]  | [888, Holders.configuration.maxRowLimit]
  }

  def "verify calculateFromAndSizeForList supports configurable max limit"() {
    given: 'a configured max limit'
    def originalConfig = Holders.configuration
    Holders.configuration = new EFrameConfiguration(maxRowLimit: 237)

    expect:
    ControllerUtils.instance.calculateFromAndSizeForList([from: 12, size: 500]) == [12, 237]

    cleanup:
    Holders.configuration = originalConfig
  }

  def "verify calculateFromAndSizeForList with allowNulls true and false"() {
    expect: 'a POGO response class with a domain reference'
    ControllerUtils.instance.calculateFromAndSizeForList(params, allowNulls) == result

    where:
    params | allowNulls | result
    [:]    | true       | [null, null]
    [:]    | false      | [0, UIDefaults.PAGE_SIZE]
  }


  def "verify calculateSortingForList works for basic cases"() {
    expect:
    ControllerUtils.instance.calculateSortingForList(map) == result

    where:
    map                           | result
    [:]                           | [null, null]
    [sort: 'A']                   | ['A', 'asc']
    [sort: 'A', order: 'desc']    | ['A', 'desc']
    ["sort[B]": 'desc']           | ['B', 'desc']
    ["sort[B]": 'asc']            | ['B', 'asc']
    ["sort[B]": 'asc', sort: 'A'] | ['B', 'asc']  // Both types, toolkit style wins
  }

  def "verify determineBaseURI works"() {
    expect: 'the method works'
    assert ControllerUtils.instance.determineBaseURI(input) == result

    where:
    input                                                         | result
    '/parent/show?test=null'                                      | '/parent/show'
    '/parent/show/148f5327-b297-44bc-8ccc-46f8fd4c32e6?test=null' | '/parent/show'
    '/app/parent/show/148f5327-b297-44bc-8ccc-46f8fd4c32e6'       | '/app/parent/show'
    '/app/parent/show/14875327-164f'                              | '/app/parent/show/14875327-164f'  // Looks a little like a UUID, but is not.
    '/app/parent/show'                                            | '/app/parent/show'
    '/app/parent/show/'                                           | '/app/parent/show/'
  }

  def "verify getRootPath works"() {
    expect: 'the method works'
    ControllerUtils.instance.getRootPath(clazz) == result

    where:
    clazz                  | result
    SampleParentController | '/sampleParent'
    RMAController          | '/rma'
  }

  def "verify that getRootPath supports the intercepted form of the bean class"() {
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
    ControllerUtils.instance.getRootPath(clazz) == '/allFieldsDomain'
  }

  def "verify test delay works and logs a warning message"() {
    given: 'a mock appender for Info level only'
    def mockAppender = MockAppender.mock(ControllerUtils, Level.WARN)

    and: 'a 0.5 sec delay is configured'
    def originalDelay = Holders.configuration.testDelay
    Holders.configuration.testDelay = 100

    when: 'the delay is triggered'
    def start = System.currentTimeMillis()
    ControllerUtils.instance.delayForTesting('loc')

    then: 'the delay happened'
    (System.currentTimeMillis() - start) >= 100

    then: 'the log message is written'
    UnitTestUtils.assertContainsAllIgnoreCase(mockAppender.message, ['WARN', 'sleep', '100', 'testDelay'])

    cleanup:
    Holders.configuration.testDelay = originalDelay
  }

  def "verify that getAllBrowserPaths works"() {
    given: 'some mocked controllers'
    ControllerUtils.instance = Mock(ControllerUtils)
    ControllerUtils.instance.allControllers >> [AllFieldsDomainController, SampleParentController]

    and: 'the original getAllBrowserPaths is used'
    ControllerUtils.instance.allBrowserPaths >> { new ControllerUtils().allBrowserPaths }

    and: 'a mocked TaskMenuControllerUtils'
    def tasks = [new TaskMenuItem(folder: 'admin:7000', name: 'afd', uri: '/allFieldsDomain', displayOrder: 7050, clientRootActivity: true),
                 new TaskMenuItem(folder: 'admin:7000', name: 'parent', uri: '/sampleParent', displayOrder: 7050, clientRootActivity: true)]
    TaskMenuControllerUtils.instance = Mock(TaskMenuControllerUtils)
    TaskMenuControllerUtils.instance.coreTasks >> tasks

    when: 'the controllers a found'
    def paths = ControllerUtils.instance.allBrowserPaths

    then: 'it contains the expected paths'
    paths.contains('/allFieldsDomain')
    paths.contains('/sampleParent')

    cleanup:
    TaskMenuControllerUtils.instance = new TaskMenuControllerUtils()
  }

  def "verify that getRootPath works"() {
    expect: 'the path found'
    ControllerUtils.instance.getRootPath(SampleParentController) == '/sampleParent'
  }

  def "verify that getControllerByName works"() {
    given: 'some mocked controllers, using the original getControllerByName method for testing'
    new MockControllerUtils(this, SampleParentController, ['getControllerByName']).install()

    expect: 'the controllers is found'
    ControllerUtils.instance.getControllerByName('SampleParentController') == SampleParentController
  }

  def "verify that getDomainClass can find the uri"() {
    given: 'a mocked domain utils'
    new MockDomainUtils(this, SampleParent).install()

    expect: 'the correct domain class is found'
    ControllerUtils.instance.getDomainClass('/sampleParent/show') == SampleParent
  }

  def "verify that buildURI builds the URI correctly"() {
    given: 'a mocked domain utils'
    new MockDomainUtils(this, SampleParent).install()

    expect: 'the correct domain class is found'
    ControllerUtils.instance.buildURI(uri, params) == result

    where:
    uri                      | params               | result
    '/sampleParent'          | [a: 'b c', c: 'd e'] | '/sampleParent?a=b+c&c=d+e'
    '/sampleParent/show'     | [a: 'b', c: 'd']     | '/sampleParent/show?a=b&c=d'
    '/sampleParent/show?x=z' | [a: 'b', c: 'd']     | '/sampleParent/show?x=z&a=b&c=d'
  }

  def "verify that getListElementFromPOGO works for the supported cases"() {
    expect: 'the correct domain class is found'
    ControllerUtils.instance.getListElementFromPOGO(name) == result

    where:
    name                                 | result
    'sample.pogo.FindWorkResponse'       | FindWorkResponseDetail
    'sample.pogo.FindWorkResponseDetail' | FindWorkResponseDetail
  }

}
