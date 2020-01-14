package org.simplemes.eframe.i18n

import org.simplemes.eframe.test.BaseSpecification

/*
 * Copyright Michael Houston 2018. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Tests.
 */
class MessageSourceSpec extends BaseSpecification {

  //static specNeeds = [SERVER]

  def "verify that getMessage works for default locale"() {
    when: 'the lookup is made'
    def s = new MessageSource().getMessage('home.label', Locale.US, null)

    then: 'it is correct'
    s == 'Home'
  }

  def "verify that getMessage works for a specific locale"() {
    when: 'the lookup is made'
    def s = new MessageSource().getMessage('searchStatus.green.label', Locale.GERMAN)

    then: 'it is correct'
    s == GlobalUtils.lookup('searchStatus.green.label', Locale.GERMAN)
  }

  def "verify that lookup works with UTF-8 encoding for German"() {
    expect: 'the correct string encoding is returned'
    def s = new MessageSource().getMessage('searchStatus.green.label', Locale.GERMAN)
    s.contains('Grün')
    s == 'Grün'
  }

  def "verify that lookup for German falls back to default bundle"() {
    expect: 'the lookup works'
    new MessageSource().getMessage('error.0.message', Locale.GERMAN) == 'Missing Error Code.'
  }

  def "verify that the sample bundle is found"() {
    expect: 'the sample string is found'
    new MessageSource().getMessage('allFieldsDomain.label', null) == 'All Fields'
  }

  def "verify that a jar file bundle can be found"() {
    // Uses the .properties file from the Report Engine .jar file.  Any .properties in any .jar file can work if this
    // .jar file changes.
    expect: 'the sample string is found'
    def s = new MessageSource(['metadata_messages']).getMessage('property.label.net.sf.jasperreports.javabean.field.property', null)
    s == "Property name"
  }

  def "verify that the getMessage supports arguments and message formatting options"() {
    expect: 'the sample string is found'
    new MessageSource().getMessage('_fields.summary.label', null, 12) == '12 Fields'
    //_fields.summary.label={0,number} {0,choice,0#Fields|1#Field|1<Fields}
  }

  def "verify that getMessage detects file changes in dev or test mode and clears the cache"() {
    given: 'a message source'
    def messageSource = new MessageSource()

    when: 'the resource is loaded'
    messageSource.getMessage('home.label', null) != null

    and: 'the last modified time is changed to simulate a touched file'
    messageSource.messagesFileLastChanged--

    and: 'the test for changed file is made'
    messageSource.devModeCheckForChanges()

    then: 'the cache was cleared'
    messageSource.cachedResourceBundles.keySet().size() == 0
  }


}
