/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.custom.controller

import groovy.json.JsonSlurper
import io.micronaut.http.HttpStatus
import org.simplemes.eframe.application.Holders
import org.simplemes.eframe.controller.ControllerUtils
import org.simplemes.eframe.custom.ExtensibleFieldHelper
import org.simplemes.eframe.custom.domain.FieldExtension
import org.simplemes.eframe.custom.domain.FieldGUIExtension
import org.simplemes.eframe.data.format.EncodedTypeFieldFormat
import org.simplemes.eframe.data.format.IntegerFieldFormat
import org.simplemes.eframe.data.format.StringFieldFormat
import org.simplemes.eframe.exception.BusinessException
import org.simplemes.eframe.i18n.GlobalUtils
import org.simplemes.eframe.test.BaseSpecification
import org.simplemes.eframe.test.MockPrincipal
import org.simplemes.eframe.test.UnitTestUtils
import org.simplemes.eframe.test.annotation.Rollback
import sample.domain.SampleParent
import spock.lang.Shared

/**
 * Tests.
 */
class ExtensionControllerSpec extends BaseSpecification {

  @SuppressWarnings("unused")
  static dirtyDomains = [FieldExtension, FieldGUIExtension]

  /**
   * The controller under test.
   */
  @Shared
  ExtensionController controller

  @Override
  def setup() {
    controller = Holders.getBean(ExtensionController)
  }

  @Rollback
  def "verify that configDialog defines the available and configured fields in the model for the page - no custom fields"() {
    when: 'a request with params is made'
    def modelAndView = controller.configDialog(mockRequest([domainURL: '/sampleParent/show']), new MockPrincipal('joe', 'ADMIN'))

    then: 'the model returned contains available and configured fields'
    def available = modelAndView.model.get().get('availableFields')
    available.contains('"name":"notDisplayed"')
    def configured = modelAndView.model.get().get('configuredFields')
    configured.contains('"name":"name"')
  }

  @Rollback
  def "verify that saveFieldOrder can save a custom field order"() {
    given: 'the desired field order'
    def desiredFieldOrder = ['name', 'title', 'custom1', 'notes', 'moreNotes', 'allFieldsDomain', 'allFieldsDomains',
                             'sampleChildren']

    when: 'a request with params is made'
    def body = [domainURL: '/sampleParent/show', fields: desiredFieldOrder]
    def response = controller.saveFieldOrder(mockRequest([body: Holders.objectMapper.writeValueAsString(body)]),
                                             new MockPrincipal('joe', 'ADMIN'))

    then: 'the effective field order is saved in the DB correctly'
    def adjustedFieldOrder = ExtensibleFieldHelper.instance.getEffectiveFieldOrder(SampleParent)
    adjustedFieldOrder == desiredFieldOrder

    and: 'the HTTP response is correct'
    response.status == HttpStatus.OK
    def json = new JsonSlurper().parseText((String) response.body.get())
    json.message.text == GlobalUtils.lookup('definitionEditor.saved.message')
  }

  @Rollback
  def "verify that saveFieldOrder detects missing URL"() {
    given: 'the desired field order'
    def desiredFieldOrder = ['name', 'title']

    when: 'a request with params is made'
    def body = [fields: desiredFieldOrder]
    controller.saveFieldOrder(mockRequest([body: Holders.objectMapper.writeValueAsString(body)]),
                              new MockPrincipal('joe', 'ADMIN'))

    then: 'the right exception is thrown'
    def ex = thrown(Exception)
    UnitTestUtils.assertExceptionIsValid(ex, ['domainURL', 'not'])
  }

  @Rollback
  def "verify that saveFieldOrder detects missing fields"() {
    given: 'the fields are missing'
    def body = [domainURL: '/sampleParent/show']

    when: 'a request with params is made'
    controller.saveFieldOrder(mockRequest([body: Holders.objectMapper.writeValueAsString(body)]),
                              new MockPrincipal('joe', 'ADMIN'))

    then: 'the right exception is thrown'
    def ex = thrown(Exception)
    UnitTestUtils.assertExceptionIsValid(ex, ['fields', 'not'])
  }

  @Rollback
  def "verify that fieldDialog defines the default values for the field - add mode"() {
    when: 'a request with params is made'
    def modelAndView = controller.fieldDialog(mockRequest([domainURL: '/sampleParent/show']), new MockPrincipal('joe', 'ADMIN'))

    then: 'the model returned contains the default field extension for the display'
    FieldExtension fieldExtension = modelAndView.model.get().get(ControllerUtils.MODEL_KEY_DOMAIN_OBJECT)
    fieldExtension.fieldFormat == StringFieldFormat.instance
    fieldExtension.maxLength == 10
  }

  @SuppressWarnings("GrEqualsBetweenInconvertibleTypes")
  @Rollback
  def "verify that saveField can create a custom field"() {
    when: 'a request with params is made'
    def body = [fieldName     : 'custom1', fieldLabel: 'abc',
                fieldFormat   : IntegerFieldFormat.instance.id, maxLength: 237,
                valueClassName: String.name,
                domainURL     : '/sampleParent/show']
    def response = controller.saveField(mockRequest([body: Holders.objectMapper.writeValueAsString(body)]),
                                        new MockPrincipal('joe', 'ADMIN'))

    then: 'the response is Ok'
    response.status == HttpStatus.OK

    and: 'the custom field is saved in the DB'
    FieldExtension fieldExtension = FieldExtension.findAllByDomainClassName(SampleParent.name).find { it.fieldName == 'custom1' }
    fieldExtension.fieldFormat == IntegerFieldFormat.instance
    fieldExtension.fieldLabel == 'abc'
    fieldExtension.valueClassName == String.name
    fieldExtension.maxLength == 237
  }

  @Rollback
  def "verify that saveField can create a custom field with an empty label"() {
    when: 'a request with params is made'
    def body = [fieldName     : 'custom1',
                fieldFormat   : IntegerFieldFormat.instance.id, maxLength: 237,
                valueClassName: String.name,
                domainURL     : '/sampleParent/show']
    def response = controller.saveField(mockRequest([body: Holders.objectMapper.writeValueAsString(body)]),
                                        new MockPrincipal('joe', 'ADMIN'))

    then: 'the label in the response is not empty'
    def details = Holders.objectMapper.readValue(response.body().toString(), Map)
    details.label == 'custom1'
  }

  @SuppressWarnings("GrEqualsBetweenInconvertibleTypes")
  def "verify that saveField can update a custom field"() {
    given: 'an existing field extension record'
    def fe = null
    FieldExtension.withTransaction {
      fe = new FieldExtension(fieldName: 'custom1', fieldLabel: 'abc',
                              fieldFormat: EncodedTypeFieldFormat.instance, maxLength: 437,
                              valueClassName: String.name, domainClassName: SampleParent.name)
      fe.save()
    }

    when: 'a request with params is made'
    def body = [fieldName     : 'custom2', fieldLabel: 'xyz',
                fieldFormat   : IntegerFieldFormat.instance.id, maxLength: 237,
                valueClassName: Long.name,
                id            : fe.uuid.toString(),
                domainURL     : '/sampleParent/show']
    def response = controller.saveField(mockRequest([body: Holders.objectMapper.writeValueAsString(body)]),
                                        new MockPrincipal('joe', 'ADMIN'))

    then: 'the response is Ok'
    response.status == HttpStatus.OK

    and: 'the custom field is saved in the DB'
    FieldExtension fieldExtension = null
    FieldExtension.withTransaction {
      fieldExtension = FieldExtension.findAllByDomainClassName(SampleParent.name).find { it.fieldName == 'custom2' }
      assert fieldExtension.fieldFormat == IntegerFieldFormat.instance
      assert fieldExtension.fieldLabel == 'xyz'
      assert fieldExtension.valueClassName == Long.name
      assert fieldExtension.maxLength == 237
      true
    }

    and: 'the saved details are in the response'
    def details = Holders.objectMapper.readValue(response.body().toString(), Map)
    details.name == fieldExtension.fieldName
    details.label == GlobalUtils.lookup(fieldExtension.fieldLabel)
    details.type == 'textField'
    details.custom == true
  }

  def "verify that saveField gracefully detects invalid record ID"() {
    when: 'a request with params is made'
    def body = [fieldName     : 'custom2', fieldLabel: 'xyz',
                fieldFormat   : IntegerFieldFormat.instance.id, maxLength: 237,
                valueClassName: Long.name,
                id            : 'e91ab8d8-93ad-4a27-b1ed-adb6e4555690',
                domainURL     : '/sampleParent/show']
    controller.saveField(mockRequest([body: Holders.objectMapper.writeValueAsString(body)]),
                         new MockPrincipal('joe', 'ADMIN'))

    then: 'the right exception is thrown'
    //error.134.message=The record (id={0}) for domain {1} could not be found.
    def ex = thrown(BusinessException)
    UnitTestUtils.assertExceptionIsValid(ex, ['e91ab8d8-93ad-4a27-b1ed-adb6e4555690', '134', SampleParent.simpleName])
  }

  def "verify that saveField gracefully detects domain validation errors"() {
    when: 'a request with params is made'
    def body = [domainURL: '/sampleParent/show']
    def response = controller.saveField(mockRequest([body: Holders.objectMapper.writeValueAsString(body)]),
                                        new MockPrincipal('joe', 'ADMIN'))

    then: 'the response is correct'
    response.status == HttpStatus.BAD_REQUEST

    def json = new JsonSlurper().parseText((String) response.body())
    UnitTestUtils.assertContainsAllIgnoreCase(json.message.text, ['fieldName', 'missing'])
  }

  def "verify that deleteField can create delete a custom field"() {
    given: 'an existing field extension record'
    def fe = buildCustomField(fieldName: 'custom1', fieldLabel: 'abc',
                              fieldFormat: EncodedTypeFieldFormat.instance, maxLength: 437,
                              valueClassName: String.name, domainClass: SampleParent)

    when: 'a request with params is made'
    def body = [id: fe.uuid.toString(), domainURL: '/sampleParent/show']
    def response = controller.deleteField(mockRequest([body: Holders.objectMapper.writeValueAsString(body)]),
                                          new MockPrincipal('joe', 'ADMIN'))

    then: 'the response is Ok'
    response.status == HttpStatus.OK

    and: 'the custom field is removed from the DB'
    FieldExtension.withTransaction {
      assert FieldExtension.findAllByDomainClassName(SampleParent.name).size() == 0
      true
    }
  }

  def "verify that deleteField gracefully handles record not found"() {
    when: 'a request with params is made'
    def body = [id: 'e91ab8d8-93ad-4a27-b1ed-adb6e4555690']
    controller.deleteField(mockRequest([body: Holders.objectMapper.writeValueAsString(body)]),
                           new MockPrincipal('joe', 'ADMIN'))

    then: 'the right exception is thrown'
    def ex = thrown(Exception)
    UnitTestUtils.assertExceptionIsValid(ex, ['105', 'e91ab8d8-93ad-4a27-b1ed-adb6e4555690'])

  }

}
