package org.simplemes.eframe.custom.service


import org.simplemes.eframe.custom.ExtensibleFieldHelper
import org.simplemes.eframe.custom.domain.FieldExtension
import org.simplemes.eframe.custom.domain.FieldGUIExtension
import org.simplemes.eframe.custom.gui.FieldInsertAdjustment
import org.simplemes.eframe.data.format.BigDecimalFieldFormat
import org.simplemes.eframe.data.format.BooleanFieldFormat
import org.simplemes.eframe.data.format.DateFieldFormat
import org.simplemes.eframe.data.format.DateOnlyFieldFormat
import org.simplemes.eframe.data.format.DomainReferenceFieldFormat
import org.simplemes.eframe.data.format.EnumFieldFormat
import org.simplemes.eframe.data.format.FieldFormatInterface
import org.simplemes.eframe.data.format.StringFieldFormat
import org.simplemes.eframe.domain.DomainUtils
import org.simplemes.eframe.test.BaseSpecification
import sample.domain.AllFieldsDomain
import sample.domain.SampleParent

/*
 * Copyright Michael Houston 2019. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Tests.
 */
class ExtensionServiceSpec extends BaseSpecification {

  @SuppressWarnings("unused")
  static specNeeds = [SERVER, JSON]

  @SuppressWarnings("unused")
  static dirtyDomains = [FieldExtension, FieldGUIExtension]

  //TODO: Find alternative to @Rollback
  def "verify that getExtensionConfiguration works with normal domain and no custom fields"() {
    when: 'the configuration is found'
    def (List available, List configured) = new ExtensionService().getExtensionConfiguration(SampleParent)

    then: 'the configured list is correct'
    configured.size() == SampleParent.fieldOrder.size()
    configured[0] == [name: 'name', type: 'textField', label: lookup('name.label'), custom: false]
    configured[1] == [name: 'title', type: 'textField', label: lookup('title.label'), custom: false]

    and: 'the available list is correct'
    available.size() > 0
    def f = available.find() { it.name == 'notDisplayed' }
    f == [name: 'notDisplayed', type: 'textField', label: lookup('notDisplayed.label'), custom: false]
  }

  //TODO: Find alternative to @Rollback
  def "verify that getExtensionConfiguration handles panels in the fieldOrder"() {
    when: 'the configuration is found'
    //noinspection GroovyUnusedAssignment
    def (List available, List configured) = new ExtensionService().getExtensionConfiguration(AllFieldsDomain)

    then: 'the configured list has a panel field'
    def f1 = configured.find() { it.name == 'group:details' }
    f1.type == 'tabbedPanels'
    f1.label == lookup('details.panel.label')
    !f1.custom
  }

  //TODO: Find alternative to @Rollback
  def "verify that getExtensionConfiguration handles custom panel in the fieldOrder"() {
    given: 'a custom panel added to the list'
    def fg = new FieldGUIExtension(domainName: SampleParent.name)
    fg.adjustments = [new FieldInsertAdjustment(fieldName: 'group:dummy', afterFieldName: 'title')]
    fg.save()

    when: 'the configuration is found'
    //noinspection GroovyUnusedAssignment
    def (List available, List configured) = new ExtensionService().getExtensionConfiguration(SampleParent)

    then: 'the configured list has a panel field'
    def f1 = configured.find() { it.name == 'group:dummy' }
    f1.type == 'tabbedPanels'
    f1.label == 'dummy'
    f1.custom
  }

  //TODO: Find alternative to @Rollback
  def "verify that getExtensionConfiguration works with custom fields - not configured for display"() {
    given: 'a custom field for the domain'
    def fieldExtension = new FieldExtension(fieldName: 'custom1', domainClassName: SampleParent.name,
                                            fieldFormat: BigDecimalFieldFormat.instance).save()

    when: 'the configuration is found'
    def (List available, List configured) = new ExtensionService().getExtensionConfiguration(SampleParent)

    then: 'the configured list is correct'
    configured.size() == SampleParent.fieldOrder.size()

    and: 'the custom field is not in the current displayed field list'
    def f1 = configured.find() { it.name == 'custom1' }
    !f1

    and: 'the custom field is in the available list'
    def f2 = available.find() { it.name == 'custom1' }
    f2 == [name: 'custom1', type: 'textField', label: 'custom1', custom: true, recordID: fieldExtension.id]
  }

  //TODO: Find alternative to @Rollback
  def "verify that getExtensionConfiguration works with custom fields - configured for display"() {
    given: 'a custom field for the domain'
    def fieldExtension = new FieldExtension(fieldName: 'custom1', domainClassName: SampleParent.name,
                                            fieldFormat: BigDecimalFieldFormat.instance).save()
    def fg = new FieldGUIExtension(domainName: SampleParent.name)
    fg.adjustments = [new FieldInsertAdjustment(fieldName: 'custom1', afterFieldName: 'title')]
    fg.save()

    when: 'the configuration is found'
    def (List available, List configured) = new ExtensionService().getExtensionConfiguration(SampleParent)

    then: 'the configured list is correct'
    configured.size() == (SampleParent.fieldOrder.size() + 1)
    configured[0].name == 'name'
    configured[1].name == 'title'
    configured[2].name == 'custom1'

    and: 'the record ID for the custom field extension is in the result'
    configured[2].recordID == fieldExtension.id

    and: 'the custom field is not in the available field list'
    def f1 = available.find() { it.name == 'custom1' }
    !f1
  }

  def "verify that determineFieldType works with the supported field types"() {
    expect: 'the right type is returned'
    new ExtensionService().determineFieldType(name, (FieldFormatInterface) format?.instance) == type

    where:
    format                     | name         | type
    StringFieldFormat          | 'a'          | 'textField'
    DateFieldFormat            | 'a'          | 'dateField'
    DateOnlyFieldFormat        | 'a'          | 'dateField'
    EnumFieldFormat            | 'a'          | 'dropDown'
    DomainReferenceFieldFormat | 'a'          | 'dropDown'
    BooleanFieldFormat         | 'a'          | 'checkBox'
    null                       | 'a'          | 'textField'
    null                       | 'group:main' | 'tabbedPanels'
  }

  //TODO: Find alternative to @Rollback
  def "verify that saveFieldOrder can save the new field order - create case"() {
    given: 'a new fieldOrder with the field added after the title'
    def newFieldOrder = DomainUtils.instance.getStaticFieldOrder(SampleParent)
    newFieldOrder.add(newFieldOrder.indexOf('title') + 1, 'custom1')

    when: 'the configuration is saved'
    new ExtensionService().saveFieldOrder(SampleParent, newFieldOrder)

    then: 'the new field is in the new effective field order'
    def adjustedFieldOrder = ExtensibleFieldHelper.instance.getEffectiveFieldOrder(SampleParent)
    adjustedFieldOrder.indexOf('title') < adjustedFieldOrder.indexOf('custom1')
  }

  //TODO: Find alternative to @Rollback
  def "verify that saveFieldOrder can save the new field order - update case"() {
    given: 'an existing custom field added to the list'
    def fg = new FieldGUIExtension(domainName: SampleParent.name)
    fg.adjustments = [new FieldInsertAdjustment(fieldName: 'custom0', afterFieldName: 'name')]
    fg.save()

    and: 'a new fieldOrder with the field added after the title'
    def newFieldOrder = DomainUtils.instance.getStaticFieldOrder(SampleParent)
    newFieldOrder.add(newFieldOrder.indexOf('title') + 1, 'custom1')

    when: 'the configuration is saved'
    new ExtensionService().saveFieldOrder(SampleParent, newFieldOrder)

    then: 'the new field is in the new effective field order'
    def adjustedFieldOrder = ExtensibleFieldHelper.instance.getEffectiveFieldOrder(SampleParent)
    adjustedFieldOrder.indexOf('title') < adjustedFieldOrder.indexOf('custom1')

    and: 'the original field is no longer in the list'
    // This is because custom0 is not in the new field order desired by the caller.
    adjustedFieldOrder.indexOf('custom0') < 0
  }

  //TODO: Find alternative to @Rollback
  def "verify that saveFieldOrder can save the new field order when moving the location of a custom field"() {
    given: 'an existing custom field added to the list'
    def fg = new FieldGUIExtension(domainName: SampleParent.name)
    fg.adjustments = [new FieldInsertAdjustment(fieldName: 'custom0', afterFieldName: 'name')]
    fg.save()

    and: 'the field is moved to after the title'
    def newFieldOrder = DomainUtils.instance.getStaticFieldOrder(SampleParent)
    newFieldOrder.add(newFieldOrder.indexOf('title') + 1, 'custom0')

    when: 'the configuration is saved'
    new ExtensionService().saveFieldOrder(SampleParent, newFieldOrder)

    then: 'the existing custom field is in the new effective field order'
    def adjustedFieldOrder = ExtensibleFieldHelper.instance.getEffectiveFieldOrder(SampleParent)
    adjustedFieldOrder.indexOf('title') < adjustedFieldOrder.indexOf('custom0')
  }

  def "verify that deleteField can delete a custom field"() {
    given: 'an existing custom field'
    def fieldExtension = null
    FieldExtension.withTransaction {
      fieldExtension = new FieldExtension(fieldName: 'custom1', domainClassName: SampleParent.name,
                                          fieldFormat: BigDecimalFieldFormat.instance).save()
      def fg = new FieldGUIExtension(domainName: SampleParent.name)
      fg.adjustments = [new FieldInsertAdjustment(fieldName: 'custom1', afterFieldName: 'title')]
      fg.save()
    }

    when: 'the record is deleted'
    new ExtensionService().deleteField(fieldExtension.id.toString())

    then: 'the field is no longer in the new effective field order'
    def adjustedFieldOrder = ExtensibleFieldHelper.instance.getEffectiveFieldOrder(SampleParent)
    !adjustedFieldOrder.contains('custom1')

    and: 'the record is delete'
    FieldExtension.withTransaction {
      assert FieldExtension.findByDomainClassNameAndFieldName(SampleParent.name, 'custom1') == null
      true
    }
  }

  def "verify that deleteField can create delete a custom field and leave other custom fields as-is"() {
    given: 'two existing field extension records'
    def fe = buildCustomField([[fieldName: 'custom1', domainClass: SampleParent, afterFieldName: 'title'],
                               [fieldName: 'custom2', domainClass: SampleParent, afterFieldName: 'notes']])

    when: 'a request with params is made'
    new ExtensionService().deleteField(fe.id.toString())

    then: 'the field is not in the field order'
    def adjustedFieldOrder = ExtensibleFieldHelper.instance.getEffectiveFieldOrder(SampleParent)
    !adjustedFieldOrder.contains('custom1')

    and: 'the other field is still in the order'
    adjustedFieldOrder.contains('custom2')
  }

  def "verify that deleteField gracefully detects missing record"() {
    when: 'a request with params is made'
    def res = new ExtensionService().deleteField('878788797')

    then: 'the response is correct'
    res == 0
  }


}
