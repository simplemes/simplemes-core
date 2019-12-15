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
class MessageHolderSpec extends BaseSpecification {

  def "verify that the Message Holder serialization works"() {
    given: 'a message holder'
    def messageHolder = new MessageHolder(text: 'a bad message')

    when: 'the message is serialized to JSON'
    def s = new ObjectMapper().writeValueAsString(messageHolder)
    //println "s = $s"

    then: 'the JSON is valid'
    def json = new JsonSlurper().parseText(s)
    json.message.text == 'a bad message'
    json.message.level == 'error'
  }

  def "verify that the Message Holder add methods work"() {
    given: 'a message holder'
    def messageHolder = new MessageHolder(level: MessageHolder.LEVEL_INFO, text: 'an info message', code: 103)

    when: 'more messages are added'
    messageHolder.addError(text: 'an error message', code: 101)
    messageHolder.addWarn(text: 'a warning message')
    messageHolder.addWarning(text: 'another warning message')
    messageHolder.addInfo(text: 'another info message')

    then: 'the main message is the highest priority - error'
    messageHolder.text == 'an error message'
    messageHolder.code == 101
    messageHolder.level == MessageHolder.LEVEL_ERROR

    and: 'the secondary messages have the rest of the messages'
    messageHolder.otherMessages.size() == 4

    def infoMsg = messageHolder.otherMessages.find { it.text == 'an info message' }
    infoMsg.code == 103
    infoMsg.level == MessageHolder.LEVEL_INFO

    def anotherInfoMsg = messageHolder.otherMessages.find { it.text == 'another info message' }
    anotherInfoMsg.level == MessageHolder.LEVEL_INFO

    def warnMsg = messageHolder.otherMessages.find { it.text == 'a warning message' }
    warnMsg.level == MessageHolder.LEVEL_WARN

    def warningMsg = messageHolder.otherMessages.find { it.text == 'another warning message' }
    warningMsg.level == MessageHolder.LEVEL_WARN
  }

  def "verify that empty Message Holder can have the add methods called"() {
    given: 'a message holder'
    def messageHolder = new MessageHolder()

    when: 'a message is added'
    messageHolder.addError(text: 'an error message', code: 101)

    then: 'the main message is added message'
    messageHolder.text == 'an error message'
    messageHolder.code == 101
    messageHolder.level == MessageHolder.LEVEL_ERROR
  }

  def "verify that validation error map constructor works - multiple errors"() {
    given: 'the validation errors'
    def error1 = Mock(FieldError)
    error1.getField() >> 'name'
    error1.getCodes() >> ['someMessage.message']
    error1.getArguments() >> ['name']
    error1.getDefaultMessage() >> 'someMessage.message'
    0 * _

    def error2 = Mock(FieldError)
    error2.getField() >> 'zTitle'
    error2.getCodes() >> ['anotherMessage.message']
    error2.getArguments() >> ['zTitle']
    error2.getDefaultMessage() >> 'anotherMessage.message'
    0 * _

    def errors = Mock(ValidationErrors)
    errors.getAllErrors() >> [error1, error2]
    0 * _

    when: 'a message is added'
    def messageHolder = new MessageHolder(errors)

    then: 'the main message is in the holder'
    messageHolder.text == 'someMessage.message'

    and: 'the second message is in the holder'
    messageHolder.otherMessages[0].text == 'anotherMessage.message'
  }

}
