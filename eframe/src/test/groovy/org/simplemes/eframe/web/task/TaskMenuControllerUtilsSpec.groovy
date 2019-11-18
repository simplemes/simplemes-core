package org.simplemes.eframe.web.task

import io.micronaut.context.ApplicationContext
import org.simplemes.eframe.application.Holders
import org.simplemes.eframe.test.BaseSpecification
import org.simplemes.eframe.test.CompilerTestUtils
import org.simplemes.eframe.test.MockControllerUtils
import spock.lang.Shared

/*
 * Copyright Michael Houston 2018. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Tests.
 */
class TaskMenuControllerUtilsSpec extends BaseSpecification {

  @Shared
  ApplicationContext originalApplicationContext

  def "verify that determineCoreTasks finds the core tasks in a controller"() {
    given: 'a mock controller with task menus'
    def src = """
    package sample
    import org.simplemes.eframe.web.task.TaskMenuItem
        
    class _ATestController {
      def taskMenuItems = [new TaskMenuItem(folder: 'admin:7000', name: 'user', uri: '/user', displayOrder: 7050)]
    }
    """
    def clazz = CompilerTestUtils.compileSource(src)
    new MockControllerUtils(this, [clazz], ['getControllerByName']).install()

    and: 'a mock application context'
    originalApplicationContext = Holders.applicationContext
    Holders.applicationContext = Mock(ApplicationContext)
    Holders.applicationContext.findOrInstantiateBean(*_) >> Optional.of(clazz.newInstance())

    when: 'the core list of task menus is returned'
    def taskMenus = TaskMenuControllerUtils.instance.coreTasks

    then: 'the expected task is in the list'
    taskMenus.size() == 1
    taskMenus[0].name == 'user'
    taskMenus[0].uri == '/user'
    taskMenus[0].controllerClass == clazz

    cleanup:
    Holders.applicationContext = originalApplicationContext
  }
}
