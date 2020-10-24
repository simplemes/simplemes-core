/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.web.ui.webix.freemarker


import org.simplemes.eframe.controller.StandardModelAndView
import org.simplemes.eframe.exception.MessageHolder
import org.simplemes.eframe.test.BaseMarkerSpecification
import org.simplemes.eframe.test.HTMLTestUtils

/**
 * Tests.
 */
class MessagesMarkerSpec extends BaseMarkerSpecification {

  def "verify that the marker creates the div correctly"() {
    when: 'the marker is applied'
    def page = execute(source: '<@efMessages/>')

    then: 'the correct div is created'
    HTMLTestUtils.checkHTML(page)
    page.contains('<div id="messages"></div>')
  }

  def "verify that the marker creates the div with an error message"() {
    given: 'the data model with an error message'
    def msg = new MessageHolder(text: 'an error message')
    def params = [(StandardModelAndView.MESSAGES): msg]

    when: 'the marker is applied'
    def page = execute(source: '<@efMessages/>', dataModel: params, uri: '/logging/dummy?arg=value')

    then: 'the correct div is created'
    HTMLTestUtils.checkHTML(page)
    page.contains('<div id="messages">')
    page.contains('<div class="message error-message">an error message</div>')
  }

  def "verify that the marker creates the div with multiple message of different types"() {
    given: 'the options with an error message'
    def msg = new MessageHolder(text: 'an error message')
    msg.addError([text: 'another error message'])
    msg.addWarn([text: 'a warning message'])
    msg.addInfo([text: 'an info message'])
    def params = [(StandardModelAndView.MESSAGES): msg]

    when: 'the marker is applied'
    def page = execute(source: '<@efMessages/>', dataModel: params, uri: '/logging/dummy?arg=value')

    then: 'the correct div is created'
    HTMLTestUtils.checkHTML(page)
    page.contains('<div class="message error-message">an error message</div>')
    page.contains('<div class="message error-message">another error message</div>')
    page.contains('<div class="message warning-message">a warning message</div>')
    page.contains('<div class="message info-message">an info message</div>')
  }

  def "verify that the marker escapes any embedded HTML in the message"() {
    given: 'the options with an error message'
    def msg = new MessageHolder(text: 'an error message<script></script>')
    def params = [(StandardModelAndView.MESSAGES): msg]

    when: 'the marker is applied'
    def page = execute(source: '<@efMessages/>', dataModel: params, uri: '/logging/dummy?arg=value')

    then: 'the correct div is created'
    HTMLTestUtils.checkHTML(page)
    page.contains('<div class="message error-message">an error message&lt;script&gt;&lt;/script&gt;</div>')
    !page.contains('<script>')
  }

  def "verify that the marker handles a message on the URL"() {
    given: 'the data model with a message as a parameter'
    def params = [:]
    def paramName = "_$type"
    params[paramName] = message
    def dataModel = [params: params]

    when: 'the marker is applied'
    def page = execute(source: '<@efMessages/>', dataModel: dataModel)

    then: 'the correct div is created'
    HTMLTestUtils.checkHTML(page)
    page.contains('<div id="messages">')
    page.contains("""<div class="message $type-message">$message</div>""")

    where:
    type      | message
    'info'    | 'ABC Message'
    'error'   | 'ABC Message'
    'warning' | 'ABC Message'
  }

}
