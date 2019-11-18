package org.simplemes.eframe.web.task


import org.simplemes.eframe.i18n.GlobalUtils
import org.simplemes.eframe.test.BaseSpecification
import org.simplemes.eframe.test.UnitTestUtils
import sample.controller.SampleParentController

/*
 * Copyright (c) 2018 Simple MES, LLC.  All rights reserved.  See license.txt for license terms.
 */
/**
 * Tests.
 */
class TaskMenuItemSpec extends BaseSpecification {

  def "verify that the name is localized"() {
    when: 'the name is localized using the locale'
    def s = new TaskMenuItem(name: name).getLocalizedName(locale)

    then: 'the localized value is return from the message.properties'
    s == value

    where:
    name        | locale        | value
    'flexType'  | Locale.US     | GlobalUtils.lookup("taskMenu.${name}.label", null, locale)
    'flexType'  | Locale.GERMAN | GlobalUtils.lookup("taskMenu.${name}.label", null, locale)
    'flexType'  | null          | GlobalUtils.lookup("taskMenu.${name}.label", null, locale)
    'gibberish' | null          | 'gibberish'
    'gibb"Er'   | null          | 'gibbEr'
    "gibb'Er"   | null          | 'gibbEr'
  }

  def "verify that the tooltip is localized"() {
    when: 'the tooltip is localized using the locale'
    def s = new TaskMenuItem(name: name).getLocalizedTooltip(locale)

    then: 'the localized value is return from the message.properties'
    s == value

    where:
    name        | locale        | value
    'flexType'  | Locale.US     | GlobalUtils.lookup("taskMenu.${name}.tooltip", null, locale)
    'flexType'  | Locale.GERMAN | GlobalUtils.lookup("taskMenu.${name}.tooltip", null, locale)
    'flexType'  | null          | GlobalUtils.lookup("taskMenu.${name}.tooltip", null, locale)
    'gibberish' | null          | 'gibberish'
    'gibb"Er'   | null          | 'gibbEr'
    "gibb'Er"   | null          | 'gibbEr'
  }

  def "verify that getName cleans up the name - removes quotes"() {
    when: 'the tooltip is localized using the locale'
    def s = new TaskMenuItem(name: name).name

    then: 'the localized value is return from the message.properties'
    s == value

    where:
    name        | value
    'gibberish' | 'gibberish'
    'gibb"Er'   | 'gibbEr'
    "gibb'Er"   | 'gibbEr'
  }

  def "verify that check detects missing name"() {
    when: 'the item is checked'
    def s = new TaskMenuItem(controllerClass: SampleParentController).check()

    then: 'the problem is correctly detected'
    UnitTestUtils.assertContainsAllIgnoreCase(s, ['missing', 'name', 'source', 'ParentController'])
  }

  def "verify that check detects missing uri"() {
    when: 'the item is checked'

    def s = new TaskMenuItem(name: 'ABC', controllerClass: SampleParentController).check()

    then: 'the problem is correctly detected'
    UnitTestUtils.assertContainsAllIgnoreCase(s, ['missing', 'uri', 'ABC', 'source', 'ParentController'])
  }

}
