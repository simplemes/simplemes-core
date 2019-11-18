package org.simplemes.eframe.web.task.controller

import groovy.json.JsonSlurper
import io.micronaut.http.HttpHeaders
import org.simplemes.eframe.application.EFrameConfiguration
import org.simplemes.eframe.application.Holders
import org.simplemes.eframe.test.BaseSpecification
import org.simplemes.eframe.test.CompilerTestUtils
import org.simplemes.eframe.test.JavascriptTestUtils
import org.simplemes.eframe.test.MockBean
import org.simplemes.eframe.test.MockControllerUtils
import org.simplemes.eframe.test.MockObjectMapper
import org.simplemes.eframe.test.MockPrincipal

/*
 * Copyright Michael Houston 2018. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Tests.
 */
class TaskMenuControllerSpec extends BaseSpecification {

  void cleanup() {
    // reset the configuration
    Holders.configuration = new EFrameConfiguration()
  }

  def "verify that index produces legal Javascript"() {
    given: 'a mock controller with task menus'
    def src = """
    package sample
    import org.simplemes.eframe.web.task.TaskMenuItem
        
    class _ATestController {
      def taskMenuItems = [new TaskMenuItem(folder: 'admin:7000', name: 'user', uri: '/user', displayOrder: 7050)]
    }
    """
    def clazz = CompilerTestUtils.compileSource(src)
    new MockControllerUtils(this, [clazz]).install()

    and: 'the object mapper is mocked for the test'
    new MockObjectMapper(this).install()

    and: 'a mock child'
    new MockBean(this, clazz, clazz.newInstance()).install()

    when: ''
    def res = new TaskMenuController().index(new MockPrincipal())
    def text = res.body().toString()
    //println "text = $text"

    then: 'the javascript is legal'
    JavascriptTestUtils.checkScriptFragment(text)

    and: 'the json text is legal'
    def start = text.indexOf("tk._setTaskMenuString('") + 23
    def end = text.lastIndexOf("')") - 1
    def s = text[start..end]
    //println "s = $s"
    //println "JSON = ${groovy.json.JsonOutput.prettyPrint(s)}"

    and: 'the content is correct'
    def json = new JsonSlurper().parseText(s) as List
    json.size() == 1
    json[0].id == 'admin'
    json[0].value == lookup('taskMenu.admin.label')
    json[0].tooltip == lookup('taskMenu.admin.tooltip')
    def adminChildren = json[0].data as List
    adminChildren[0].id == 'user'
    adminChildren[0].value == lookup('taskMenu.user.label')
    adminChildren[0].tooltip == lookup('taskMenu.user.tooltip')
  }

  def "verify that index handles sub-folders"() {
    given: 'a mock controller with task menus'
    def src = """
    package sample
    import org.simplemes.eframe.web.task.TaskMenuItem
        
    class _ATestController {
      def taskMenuItems = [new TaskMenuItem(folder: 'admin:7000.other:6000', name: 'user', uri: '/user', displayOrder: 7050)]
    }
    """
    def clazz = CompilerTestUtils.compileSource(src)
    new MockControllerUtils(this, [clazz]).install()

    and: 'the object mapper is mocked for the test'
    new MockObjectMapper(this).install()

    and: 'a mock child'
    new MockBean(this, clazz, clazz.newInstance()).install()

    when: ''
    def res = new TaskMenuController().index(new MockPrincipal())
    def text = res.body().toString()
    //println "text = $text"

    then: 'the javascript is legal'
    JavascriptTestUtils.checkScriptFragment(text)

    and: 'the json text is legal'
    def start = text.indexOf("tk._setTaskMenuString('") + 23
    def end = text.lastIndexOf("')") - 1
    def s = text[start..end]
    //println "s = $s"
    //println "JSON = ${groovy.json.JsonOutput.prettyPrint(s)}"

    and: 'the content is correct'
    def json = new JsonSlurper().parseText(s) as List
    json.size() == 1
    json[0].id == 'admin'
    json[0].value == lookup('taskMenu.admin.label')
    json[0].tooltip == lookup('taskMenu.admin.tooltip')
    def adminChildren = json[0].data as List
    adminChildren[0].id == 'other'
    def otherChildren = adminChildren[0].data as List
    otherChildren[0].id == 'user'
  }

  def "verify that index uses the global cacheStableResources setting"() {
    given: 'a configuration with a cache setting'
    Holders.configuration = new EFrameConfiguration(cacheStableResources: 237)

    and: 'a mock controller with task menus'
    def src = """
    package sample
    import org.simplemes.eframe.web.task.TaskMenuItem
        
    class _ATestController {
      def taskMenuItems = [new TaskMenuItem(folder: 'admin:7000', name: 'user', uri: '/user', displayOrder: 7050)]
    }
    """
    def clazz = CompilerTestUtils.compileSource(src)
    new MockControllerUtils(this, [clazz]).install()

    and: 'the object mapper is mocked for the test'
    new MockObjectMapper(this).install()

    and: 'a mock child'
    new MockBean(this, clazz, clazz.newInstance()).install()

    when: 'the task menu is generated'
    def res = new TaskMenuController().index(new MockPrincipal())

    then: 'the the cache control is correct'
    res.header(HttpHeaders.CACHE_CONTROL) == 'max-age=237'
  }

}
