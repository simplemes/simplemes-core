package org.simplemes.eframe.web.task


import org.simplemes.eframe.i18n.GlobalUtils
import org.simplemes.eframe.test.BaseSpecification
import org.simplemes.eframe.test.UnitTestUtils

/*
 * Copyright (c) 2018 Simple MES, LLC.  All rights reserved.  See license.txt for license terms.
 */
/**
 * Tests.
 */
class TaskMenuFolderSpec extends BaseSpecification {

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

  def "verify that insert - leftShift - adds the menu item"() {
    given: 'folder and a menu item'
    def folder = new TaskMenuFolder()
    def menuItem = new TaskMenuItem(name: 'flexType')

    when: 'the menu item is added'
    folder << menuItem

    then: 'item is in the list'
    folder.menuItems.size() == 1
    folder.menuItems[0] == menuItem
  }

  def "verify that buildFolders works with a simple one-level string"() {
    when: 'a single level folder is created'
    def folders = TaskMenuFolder.buildFolders('core')

    then: 'the right folder is created'
    folders[0].name == 'core'
    folders[0].displayOrder == null
  }

  def "verify that buildFolders works with a one-level string with display order"() {
    when: 'a single level folder is created with an order'
    def folders = TaskMenuFolder.buildFolders('core:237')

    then: 'the right folder is created'
    folders[0].name == 'core'
    folders[0].displayOrder == 237
  }

  def "verify that buildFolders works with a multiple levels and display orders"() {
    when: 'a multi level folder is created'
    def folders = TaskMenuFolder.buildFolders('core:237.system:437.search:337')

    then: 'the right top-level folder is created'
    folders.size() == 3
    folders[0].name == 'core'
    folders[0].displayOrder == 237

    and: 'the second level folder is created'
    folders[1].name == 'system'
    folders[1].displayOrder == 437

    and: 'the third level folder is created'
    folders[2].name == 'search'
    folders[2].displayOrder == 337
  }

  def "verify that the name is localized"() {
    given: 'a folder'
    def folders = TaskMenuFolder.buildFolders(folderName)

    when: 'the name is localized using the default locale'
    def s = folders[0].getLocalizedName(locale)

    then: 'the localized value is return from the message.properties'
    s == value

    where:
    folderName  | locale        | value
    'admin'     | Locale.US     | GlobalUtils.lookup("taskMenu.${folderName}.label", null, locale)
    'admin'     | Locale.GERMAN | GlobalUtils.lookup("taskMenu.${folderName}.label", null, locale)
    'admin'     | null          | GlobalUtils.lookup("taskMenu.${folderName}.label", null, locale)
    'gibberish' | null          | 'gibberish'
  }

  def "verify that the tooltip is localized"() {
    given: 'a folder'
    def folders = TaskMenuFolder.buildFolders(folderName)

    when: 'the name is localized using the default locale'
    def s = folders[0].getLocalizedTooltip(locale)

    then: 'the localized value is return from the message.properties'
    s == value

    where:
    folderName  | locale        | value
    'admin'     | Locale.US     | GlobalUtils.lookup("taskMenu.${folderName}.tooltip", null, locale)
    'admin'     | Locale.GERMAN | GlobalUtils.lookup("taskMenu.${folderName}.tooltip", null, locale)
    'admin'     | null          | GlobalUtils.lookup("taskMenu.${folderName}.tooltip", null, locale)
    'gibberish' | null          | 'gibberish'
  }

  def "verify that buildFolders fails with a non-numeric display order"() {
    when: 'a bad single level folder def is used'
    TaskMenuFolder.buildFolders('core:XYZ')

    then: 'the right exception is triggered'
    def ex = thrown(Exception)
    UnitTestUtils.assertExceptionIsValid(ex, ['XYZ'])
  }

  def "verify that buildFolders fails with a bad format for the folder def"() {
    when: 'a bad single level folder def is used'
    TaskMenuFolder.buildFolders(':')

    then: 'the right exception is triggered'
    def ex = thrown(Exception)
    UnitTestUtils.assertExceptionIsValid(ex, ['name', ':'])
  }

  def "verify that sortAll respects sorts for simple case - no displayOrder"() {
    given: 'an existing folder structure'
    def folder = new TaskMenuFolder()
    folder.folders << new TaskMenuFolder(name: 'perf')
    folder.folders << new TaskMenuFolder(name: 'core')
    folder.folders << new TaskMenuFolder(name: 'system')
    folder.folders << new TaskMenuFolder(name: 'search')

    when: 'the list is sorted'
    folder.sortAll()

    then: 'the resulting folder order is sorted alphabetically'
    flattenFolders(folder.folders) == 'core+perf+search+system'

    and: 'all displayOrder fields are assigned'
    def expectedDisplayOrder = 9000
    folder.folders.each {
      assert it.displayOrder == expectedDisplayOrder
      expectedDisplayOrder++
      true
    }
  }

  def "verify that sortAll respects displayOrder mixed with missing displayOrders "() {
    given: 'an existing folder structure'
    def folder = new TaskMenuFolder()
    folder.folders << new TaskMenuFolder(name: 'perf')
    folder.folders << new TaskMenuFolder(name: 'core', displayOrder: 10000)
    folder.folders << new TaskMenuFolder(name: 'system', displayOrder: 100)
    folder.folders << new TaskMenuFolder(name: 'search')

    when: 'the list is sorted'
    folder.sortAll()

    then: 'the resulting folder order is sorted correctly'
    flattenFolders(folder.folders) == 'system+perf+search+core'

    and: 'all displayOrder fields are assigned'
    folder.folders.each {
      assert it.displayOrder
      true
    }
  }

  def "verify that sortAll sorts menu items"() {
    given: 'an existing folder structure'
    def folder = new TaskMenuFolder()
    folder.menuItems << new TaskMenuItem(name: '/system/index', displayOrder: 100)
    folder.menuItems << new TaskMenuItem(name: '/core/index')
    folder.menuItems << new TaskMenuItem(name: '/flexType/index', displayOrder: 10000)
    folder.menuItems << new TaskMenuItem(name: '/perf/index')

    when: 'the list is sorted'
    folder.sortAll()

    then: 'the resulting menu items are sorted correctly'
    folder.menuItems[0].name == '/system/index'
    folder.menuItems[1].name == '/core/index'
    folder.menuItems[2].name == '/perf/index'
    folder.menuItems[3].name == '/flexType/index'

    and: 'all displayOrder fields are assigned'
    folder.menuItems.each {
      assert it.displayOrder
      true
    }
  }

  def "verify that sortAll sorts sub-folders"() {
    given: 'an existing folder structure'
    def topFolder = new TaskMenuFolder(name: 'core', displayOrder: 100)
    def childFolder1 = new TaskMenuFolder(name: 'system', displayOrder: 100)
    def childFolder2 = new TaskMenuFolder(name: 'perf', displayOrder: 200)

    topFolder.folders << childFolder1
    topFolder.folders << childFolder2

    def childFolder2a = new TaskMenuFolder(name: 'subB', displayOrder: 200)
    def childFolder2b = new TaskMenuFolder(name: 'subA', displayOrder: 100)
    childFolder2.folders << childFolder2a
    childFolder2.folders << childFolder2b

    and: 'some menu items in a sub-folder'
    def menuItem1 = new TaskMenuItem(name: '/perf/index', displayOrder: 200)
    def menuItem2 = new TaskMenuItem(name: '/core/index', displayOrder: 100)
    childFolder1.menuItems << menuItem1
    childFolder1.menuItems << menuItem2

    when: 'the list is sorted'
    topFolder.sortAll()

    then: 'the resulting folder order is sorted correctly'
    flattenFolders(topFolder.folders) == 'system+perf{subA+subB}'

    and: 'the menu items are sorted in a sub-folder'
    childFolder1.menuItems[0].name == '/core/index'
    childFolder1.menuItems[1].name == '/perf/index'
  }

  def "verify that toFullString works with folders and menu items"() {
    given: 'an existing folder structure'
    def topFolder = new TaskMenuFolder(name: 'core', displayOrder: 100)
    def childFolder1 = new TaskMenuFolder(name: 'system', displayOrder: 100)
    def childFolder2 = new TaskMenuFolder(name: 'perf', displayOrder: 200)

    topFolder.folders << childFolder1
    topFolder.folders << childFolder2

    def childFolder2a = new TaskMenuFolder(name: 'subB', displayOrder: 200)
    def childFolder2b = new TaskMenuFolder(name: 'subA', displayOrder: 100)
    childFolder2.folders << childFolder2a
    childFolder2.folders << childFolder2b

    and: 'some menu items in a sub-folder'
    def menuItem1 = new TaskMenuItem(name: 'parent', uri: '/perf/index', displayOrder: 200)
    def menuItem2 = new TaskMenuItem(name: 'flexType', uri: '/core/index', displayOrder: 100)
    childFolder1.menuItems << menuItem1
    childFolder1.menuItems << menuItem2

    when: 'the list is sorted'
    def s = topFolder.toFullString()

    then: 'the result contains the right elements'
    s.contains(topFolder.name)
    s.contains(childFolder1.name)
    s.contains(childFolder2.name)
    s.contains(childFolder2a.name)
    s.contains(childFolder2b.name)
    s.contains(menuItem1.name)
    s.contains(menuItem1.uri)
    s.contains(menuItem2.name)
    s.contains(menuItem2.uri)
  }
}
