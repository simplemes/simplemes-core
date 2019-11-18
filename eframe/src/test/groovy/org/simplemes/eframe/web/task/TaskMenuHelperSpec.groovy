package org.simplemes.eframe.web.task

import ch.qos.logback.classic.Level
import org.simplemes.eframe.test.BaseSpecification
import org.simplemes.eframe.test.MockAppender
import sample.controller.SampleParentController

/*
 * Copyright (c) 2018 Simple MES, LLC.  All rights reserved.  See license.txt for license terms.
 */
/**
 * Tests.
 */
@SuppressWarnings("GroovyAssignabilityCheck")
class TaskMenuHelperSpec extends BaseSpecification {

  def "verify that the task menu is generated correctly for the simple case"() {
    given: 'a mock controller configuration for one menu item'
    def mockUtils = Mock(TaskMenuControllerUtils)
    1 * mockUtils.coreTasks >> [new TaskMenuItem(folder: 'demand:10.order:100', name: 'parent', uri: '/parent/index', displayOrder: 101)]

    when: 'the task menu is generated'
    def taskMenu = new TaskMenuHelper(taskMenuControllerUtils: mockUtils).taskMenu

    then: 'the folders are correct'
    taskMenu.root.folders.size() == 1
    taskMenu.root.folders[0].name == 'demand'
    taskMenu.root.folders[0].displayOrder == 10

    and: 'the sub folder is correct'
    taskMenu.root.folders[0].folders.size() == 1
    def subFolder = taskMenu.root.folders[0].folders[0]
    subFolder.name == 'order'
    subFolder.displayOrder == 100

    and: 'the correct menu items are found'
    subFolder.menuItems.size() == 1
    subFolder.menuItems[0].name == 'parent'
    subFolder.menuItems[0].displayOrder == 101
  }

  def "verify that the task menu is sorted"() {
    given: 'a mock controller configuration for 3 menu items'
    def mockUtils = Mock(TaskMenuControllerUtils)
    1 * mockUtils.getCoreTasks() >> [new TaskMenuItem(folder: 'system:100', name: 'A', uri: '/parent/index'),
                                     new TaskMenuItem(folder: 'core:120', name: 'B', uri: '/parent/index'),
                                     new TaskMenuItem(folder: 'alpha:110', name: 'C', uri: '/parent/index')]

    when: 'the task menu is generated'
    def taskMenu = new TaskMenuHelper(taskMenuControllerUtils: mockUtils).taskMenu

    then: 'the folders are in the right order'
    taskMenu.root.folders.size() == 3
    taskMenu.root.folders[0].name == 'system'
    taskMenu.root.folders[1].name == 'alpha'
    taskMenu.root.folders[2].name == 'core'
  }

  def "verify that a menu item with no folder shows up in the root"() {
    given: 'a mock controller configuration for 3 menu items'
    def mockUtils = Mock(TaskMenuControllerUtils)
    1 * mockUtils.getCoreTasks() >> [new TaskMenuItem(name: 'A', uri: '/parent/index')]

    when: 'the task menu is generated'
    def taskMenu = new TaskMenuHelper(taskMenuControllerUtils: mockUtils).taskMenu

    then: 'the folders are in the right order'
    taskMenu.root.folders.size() == 0
    taskMenu.root.menuItems.size() == 1
    taskMenu.root.menuItems[0].name == 'A'
  }

  def "verify that the bad menu items are gracefully handled and logged"() {
    given: 'a mock controller configuration for two good and one bad menu items'
    def mockUtils = Mock(TaskMenuControllerUtils)
    1 * mockUtils.getCoreTasks() >> [new TaskMenuItem(folder: 'good1', name: 'parent', uri: 'index'),
                                     new TaskMenuItem(folder: 'bad', uri: 'index', controllerClass: SampleParentController),
                                     new TaskMenuItem(folder: 'good2', name: 'parent', uri: 'index')]

    and: 'a mock appender for ERROR logging'
    def mockAppender = MockAppender.mock(TaskMenuHelper, Level.ERROR)

    when: 'the task menu is generated'
    def taskMenu = new TaskMenuHelper(taskMenuControllerUtils: mockUtils).taskMenu

    then: 'the 2 good entries are in the list'
    taskMenu.root.folders.size() == 2
    taskMenu.root.folders.find { it.name == 'good1' }
    taskMenu.root.folders.find { it.name == 'good2' }

    and: 'an error message is logged'
    mockAppender.assertMessageIsValid(['TaskMenuItem', 'missing', 'name', 'SampleParentController'])

    cleanup:
    MockAppender.cleanup()
  }

}
