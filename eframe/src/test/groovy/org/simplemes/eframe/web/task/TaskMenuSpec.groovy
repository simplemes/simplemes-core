package org.simplemes.eframe.web.task

import org.simplemes.eframe.test.BaseSpecification

/*
 * Copyright (c) 2018 Simple MES, LLC.  All rights reserved.  See license.txt for license terms.
 */
/**
 * Tests.
 */
class TaskMenuSpec extends BaseSpecification {

  /**
   * Utility method to flatten the folder hierarchy for simple unit tests.
   * Produces a readable string format of the folder hierarchy.
   * <p>
   * Will produce 'core{perf+search{sub}}+admin' to indicate the children at each level.
   * @param folders The folders are this level.
   * @param builder The string builder to add this level to.
   * @return The flattened hierarchy.
   */
  String flattenFolders(List<TaskMenuFolder> folders) {
    def builder = new StringBuilder()
    for (folder in folders) {
      if (builder.size()) {
        builder << '+'
      }
      builder << folder.name
      if (folder.folders) {
        builder << "{${flattenFolders(folder.folders)}}"
      }
    }

    return builder.toString()
  }

  def "verify that basic insert - leftShift - works on a single item in a single folder"() {
    given: 'a task menu and a menu item'
    def taskMenu = new TaskMenu()
    def menuItem = new TaskMenuItem(folder: 'core:101', name: 'flexType')

    and: 'the task menu starts off in the sorted state'
    assert taskMenu.sorted

    when: 'the menu item is inserted'
    taskMenu << menuItem

    then: 'the right folder structure is created'
    taskMenu.root.folders.size() == 1
    taskMenu.root.folders[0].name == 'core'
    taskMenu.root.folders[0].displayOrder == 101
    taskMenu.root.folders[0].menuItems.size() == 1
    taskMenu.root.folders[0].menuItems[0] == menuItem

    and: 'the task menu now requires sorting'
    !taskMenu.sorted
  }

  def "verify that insert - leftShift - works on empty case"() {
    given: 'an existing folder structure'
    def taskMenu = new TaskMenu()

    when: 'the menu item with the given folder definition is added'
    taskMenu << new TaskMenuItem(folder: 'core', name: 'flexType')

    then: 'the resulting folder structure is correct'
    flattenFolders(taskMenu.root.folders) == 'core'
  }

  def "verify that insert - leftShift - works on empty case - sub-folder created"() {
    given: 'an existing folder structure'
    def taskMenu = new TaskMenu()

    when: 'the menu item with the given folder definition is added'
    taskMenu << new TaskMenuItem(folder: 'core.perf', name: 'flexType')

    then: 'the resulting folder structure is correct'
    flattenFolders(taskMenu.root.folders) == 'core{perf}'
  }

  def "verify that insert - leftShift - works on simple case"() {
    given: 'an existing folder structure'
    def taskMenu = new TaskMenu()
    taskMenu.root.folders << new TaskMenuFolder(name: 'core')

    when: 'the menu item with the given folder definition is added'
    taskMenu << new TaskMenuItem(folder: 'core.perf', name: 'flexType')

    then: 'the resulting folder structure is correct'
    flattenFolders(taskMenu.root.folders) == 'core{perf}'
  }

  def "verify that insert - leftShift - works for adding a sub-folder"() {
    given: 'an existing folder structure'
    def taskMenu = new TaskMenu()
    taskMenu.root.folders << new TaskMenuFolder(name: 'core', folders: [new TaskMenuFolder(name: 'search')])
    taskMenu.root.folders << new TaskMenuFolder(name: 'admin')

    when: 'the menu item with the given folder definition is added'
    taskMenu << new TaskMenuItem(folder: 'core.perf', name: 'flexType')

    then: 'the resulting folder structure is correct'
    flattenFolders(taskMenu.root.folders) == 'core{search+perf}+admin'
  }

  def "verify that insert - leftShift - works for adding to existing sub-folder"() {
    given: 'an existing folder structure'
    def taskMenu = new TaskMenu()
    taskMenu.root.folders << new TaskMenuFolder(name: 'core', folders: [new TaskMenuFolder(name: 'search')])

    when: 'the menu item with the given folder definition is added'
    taskMenu << new TaskMenuItem(folder: 'core.search', name: 'flexType')

    then: 'the resulting folder structure is correct'
    flattenFolders(taskMenu.root.folders) == 'core{search}'
  }

  def "verify that insert - leftShift - works for creating sub-sub-folder"() {
    given: 'an existing folder structure'
    def taskMenu = new TaskMenu()
    taskMenu.root.folders << new TaskMenuFolder(name: 'core', folders: [new TaskMenuFolder(name: 'search')])

    when: 'the menu item with the given folder definition is added'
    taskMenu << new TaskMenuItem(folder: 'core.search.admin', name: 'flexType')

    then: 'the resulting folder structure is correct'
    flattenFolders(taskMenu.root.folders) == 'core{search{admin}}'
  }

  def "verify that insert - leftShift - works for multiple new menu items added to one level"() {
    given: 'an existing folder structure'
    def taskMenu = new TaskMenu()
    taskMenu.root.folders << new TaskMenuFolder(name: 'core', folders: [new TaskMenuFolder(name: 'perf'),
                                                                        new TaskMenuFolder(name: 'search')])

    when: 'the new menu items are added'
    taskMenu << new TaskMenuItem(folder: 'core.search.admin', name: 'flexTypeA')
    taskMenu << new TaskMenuItem(folder: 'core.search.admin', name: 'flexTypeB')
    taskMenu << new TaskMenuItem(folder: 'core.search.admin', name: 'flexTypeC')

    then: 'the resulting folder structure is correct'
    flattenFolders(taskMenu.root.folders) == 'core{perf+search{admin}}'
  }

  def "verify that insert - leftShift - works for multiple new menu items i multiple levels"() {
    given: 'an existing folder structure'
    def taskMenu = new TaskMenu()
    taskMenu.root.folders << new TaskMenuFolder(name: 'core', folders: [new TaskMenuFolder(name: 'perf'),
                                                                        new TaskMenuFolder(name: 'search')])

    when: 'the new menu items are added'
    taskMenu << new TaskMenuItem(folder: 'core.search.admin', name: 'flexType')
    taskMenu << new TaskMenuItem(folder: 'core.perf', name: 'sample')

    then: 'the resulting folder structure is correct'
    flattenFolders(taskMenu.root.folders) == 'core{perf+search{admin}}'
  }

  def "verify that sortAll assigns sort order fields and sort the folders - one level"() {
    given: 'an existing folder structure'
    def taskMenu = new TaskMenu()
    taskMenu << new TaskMenuItem(folder: 'perf', name: 'sample')
    taskMenu << new TaskMenuItem(folder: 'core', name: 'flexType')
    taskMenu << new TaskMenuItem(folder: 'system', name: 'sample')
    taskMenu << new TaskMenuItem(folder: 'search', name: 'sample')

    when: 'the list is sorted'
    taskMenu.sort()

    then: 'the resulting folder order is sorted correctly'
    flattenFolders(taskMenu.root.folders) == 'core+perf+search+system'

    and: 'all displayOrder fields are assigned'
    def expectedDisplayOrder = 9000
    taskMenu.root.folders.each {
      assert it.displayOrder == expectedDisplayOrder
      expectedDisplayOrder++
      true
    }
  }

  def "verify that folder definition string with display order is used"() {
    when: 'the menu item is inserted'
    def taskMenu = new TaskMenu()
    taskMenu << new TaskMenuItem(folder: 'core', name: 'flexType', displayOrder: 101)
    taskMenu << new TaskMenuItem(folder: 'core', name: 'sample', displayOrder: 100)

    and: 'the list is sorted'
    taskMenu.sort()

    then: 'the right folder structure is created'
    taskMenu.root.folders.size() == 1
    def coreFolder = taskMenu.root.folders[0]
    coreFolder.menuItems[0].name == 'sample'
    coreFolder.menuItems[1].name == 'flexType'
  }

  def "verify that sort skips the sort when called a second time"() {
    given: 'a task menu with a mock folder to detect sorting'
    def mockFolder = Mock(TaskMenuFolder)
    def taskMenu = new TaskMenu(root: mockFolder)

    when: 'the menu is sorted'
    taskMenu.sort()

    then: 'no sort is called'
    0 * mockFolder.sortAll()
  }

  def "verify that sort happens when a new element is added to the task menu"() {
    given: 'a task menu with a mock folder to detect sorting'
    def mockFolder = Mock(TaskMenuFolder)
    def taskMenu = new TaskMenu(root: mockFolder)
    taskMenu.sorted = false

    when: 'the menu is sorted twice'
    taskMenu.sort()
    taskMenu.sort()

    then: 'sort is called just once'
    1 * mockFolder.sortAll()
  }

  def "verify that insert - leftShift - works on a menu item with no folder"() {
    given: 'a task menu and a menu item'
    def taskMenu = new TaskMenu()
    def menuItem = new TaskMenuItem(name: 'flexType')

    when: 'the menu item is inserted'
    taskMenu << menuItem

    then: 'the menu item is in the root folder'
    taskMenu.root.folders.size() == 0
    taskMenu.root.menuItems.size() == 1
    taskMenu.root.menuItems[0].name == 'flexType'
  }

  def "verify that toFullString works correctly"() {
    given: 'an existing folder structure'
    def taskMenu = new TaskMenu()
    taskMenu << new TaskMenuItem(folder: 'core.perf', name: 'sample1')
    taskMenu << new TaskMenuItem(folder: 'core.system', name: 'flexType')
    taskMenu << new TaskMenuItem(folder: 'search', name: 'sample3')

    when: 'the string is created'
    def s = taskMenu.toFullString()

    then: 'it contains the key info'
    s.contains('perf')
    s.contains('core')
    s.contains('system')
    s.contains('search')
    s.contains('sample1')
    s.contains('sample3')
  }

  // test filtering by role?
}
