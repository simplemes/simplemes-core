package org.simplemes.eframe.exception

import com.fasterxml.jackson.databind.ObjectMapper
import groovy.json.JsonSlurper
import org.simplemes.eframe.test.BaseSpecification

/*
 * Copyright Michael Houston 2018. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Tests.
 */
class MessageHolderSerializerSpec extends BaseSpecification {

  def "verify that the Message Holder serialization works"() {
    given: 'a message holder'
    def messageHolder = new MessageHolder(text: 'a bad message', code: 103)

    when: 'the message is serialized to JSON'
    def s = new ObjectMapper().writeValueAsString(messageHolder)
    //println "s = $s"

    then: 'the JSON is valid'
    def json = new JsonSlurper().parseText(s)
    json.message.text == 'a bad message'
    json.message.level == MessageHolder.LEVEL_ERROR_STRING
    json.message.code == 103
  }

  def "verify that the Message Holder serialization works with multiple messages"() {
    given: 'a message holder'
    def messageHolder = new MessageHolder(level: MessageHolder.LEVEL_INFO, text: 'an info message')
    messageHolder.addError(text: 'an error message', code: 104)
    messageHolder.addWarn(text: 'an warning message')

    when: 'the message is serialized to JSON'
    def s = new ObjectMapper().writeValueAsString(messageHolder)
    //println "s = $s"

    then: 'the JSON is valid'
    def json = new JsonSlurper().parseText(s)
    json.message.text == 'an error message'
    json.message.level == MessageHolder.LEVEL_ERROR_STRING
    json.message.code == 104

    and: 'the other messages are correct'
    List otherMessages = json.message.otherMessages
    otherMessages.size() == 2

    def warnMsg = otherMessages.find { it.level == MessageHolder.LEVEL_WARN_STRING }
    warnMsg.text == 'an warning message'

    def infoMsg = otherMessages.find { it.level == MessageHolder.LEVEL_INFO_STRING }
    infoMsg.text == 'an info message'
  }

  def "verify that an empty Message Holder can be serialized without error"() {
    given: 'a message holder'
    def messageHolder = new MessageHolder()

    when: 'the message is serialized to JSON'
    def s = new ObjectMapper().writeValueAsString(messageHolder)
    //println "s = $s"

    then: 'the JSON is valid'
    new JsonSlurper().parseText(s)
  }

  def "verify that support methods work"() {
    expect: ''
    new MessageHolderSerializer() != null
  }
}
