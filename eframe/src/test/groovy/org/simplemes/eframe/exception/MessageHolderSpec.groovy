/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.exception

import com.fasterxml.jackson.databind.ObjectMapper
import groovy.json.JsonSlurper
import org.simplemes.eframe.domain.validate.ValidationError
import org.simplemes.eframe.test.BaseSpecification

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

  def "verify that List constructor handles validation errors correctly"() {
    given: 'some errors'
    def error1 = new ValidationError(1, 'badField')
    def error2 = new ValidationError(0, 'otherBadField')

    when: 'the constructor is called'
    def messageHolder = new MessageHolder([error1, error2])

    then: 'the messages are in the list correctly'
    messageHolder.code == error1.code
    messageHolder.text == error1.toString()
    messageHolder.level == MessageHolder.LEVEL_ERROR

    def msg2 = messageHolder.otherMessages[0]
    messageHolder.otherMessages.size() == 1
    msg2.code == error2.code
    msg2.text == error2.toString()
    msg2.level == MessageHolder.LEVEL_ERROR
  }

}
