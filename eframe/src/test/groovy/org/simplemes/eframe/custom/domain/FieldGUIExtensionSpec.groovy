/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.custom.domain


import org.simplemes.eframe.custom.gui.FieldInsertAdjustment
import org.simplemes.eframe.misc.FieldSizes
import org.simplemes.eframe.test.BaseSpecification
import org.simplemes.eframe.test.DomainTester
import org.simplemes.eframe.test.annotation.Rollback

/**
 * Tests.
 */
class FieldGUIExtensionSpec extends BaseSpecification {

  @SuppressWarnings("unused")
  static specNeeds = [JSON, SERVER]

  @SuppressWarnings("unused")
  static dirtyDomains = [FieldGUIExtension]

  def "verify that domain enforces constraints"() {
    expect: 'the constraints are enforced'
    DomainTester.test {
      domain FieldGUIExtension
      requiredValues domainName: 'com.test.FlexType1'
      maxSize 'domainName', FieldSizes.MAX_CLASS_NAME_LENGTH

      notNullCheck 'domainName'
      fieldOrderCheck false
    }
  }

  def "verify that JSON conversions work - in a proper transaction"() {
    given: 'some adjustments in a saved record'
    FieldGUIExtension.withTransaction {
      def adj1 = new FieldInsertAdjustment(fieldName: 'c1', afterFieldName: 'title')
      def adj2 = new FieldInsertAdjustment(fieldName: 'c2', afterFieldName: 'title')
      def e = new FieldGUIExtension(domainName: 'com.test.FlexType')
      e.adjustments = [adj1, adj2]
      e.save()
    }

    when: 'the extension is forced to re-parse the JSON - simulates a a fresh record from the DB'
    List adjustments = null
    FieldGUIExtension.withTransaction {
      def extension = FieldGUIExtension.findByDomainName('com.test.FlexType')
      adjustments = extension.adjustments
    }

    then: 'the JSON is converted correctly to the field adjustment POGOs'
    adjustments.size() == 2
    adjustments[0] == new FieldInsertAdjustment(fieldName: 'c1', afterFieldName: 'title')
    adjustments[1] == new FieldInsertAdjustment(fieldName: 'c2', afterFieldName: 'title')
  }

  @Rollback
  def "verify that record with no adjustments works can be saved"() {
    given: 'a record with no adjustments'
    def e = new FieldGUIExtension(domainName: 'com.test.FlexType')
    e.adjustments = []
    e.save()

    when: 'the extension is forced to re-parse the JSON - simulates a a fresh record from the DB'
    def extension = FieldGUIExtension.findByDomainName('com.test.FlexType')
    extension.textHasBeenParsed = false

    then: 'the adjustments are correct'
    extension.adjustmentsText == null
    extension.adjustments == []
  }

  @Rollback
  def "verify that clearing all adjustments works"() {
    given: 'some adjustments in a saved record'
    def adj1 = new FieldInsertAdjustment(fieldName: 'c1', afterFieldName: 'title')
    def adj2 = new FieldInsertAdjustment(fieldName: 'c2', afterFieldName: 'title')
    def e = new FieldGUIExtension(domainName: 'com.test.FlexType')
    e.adjustments = [adj1, adj2]
    e.save()

    when: 'the extension is forced to re-parse the JSON - simulates a a fresh record from the DB'
    def e2 = FieldGUIExtension.findByDomainName('com.test.FlexType')
    e2.textHasBeenParsed = false

    and: 'the adjustments are cleared'
    e2.adjustments = []
    e2.save()

    then: 'the re-loaded record has no adjustments'
    def extension = FieldGUIExtension.findByDomainName('com.test.FlexType')
    extension.adjustmentsText == null
    extension.adjustments == []
  }

  @Rollback
  def "verify that updating some adjustments works"() {
    given: 'some adjustments in a saved record'
    def adj1 = new FieldInsertAdjustment(fieldName: 'c1', afterFieldName: 'title')
    def adj2 = new FieldInsertAdjustment(fieldName: 'c2', afterFieldName: 'title')
    def e = new FieldGUIExtension(domainName: 'com.test.FlexType')
    e.adjustments = [adj1, adj2]
    e.save()

    when: 'one adjustment is changed'
    adj2 = new FieldInsertAdjustment(fieldName: 'c2x', afterFieldName: 'titleX')
    e.adjustments = [adj1, adj2]
    e.save()

    and: 'the extension is forced to re-parse the JSON - simulates a a fresh record from the DB'
    def e2 = FieldGUIExtension.findByDomainName('com.test.FlexType')
    e2.textHasBeenParsed = false

    then: 'the re-loaded record has the correct adjustments'
    def extension = FieldGUIExtension.findByDomainName('com.test.FlexType')
    extension.adjustments[0] == new FieldInsertAdjustment(fieldName: 'c1', afterFieldName: 'title')
    extension.adjustments[1] == new FieldInsertAdjustment(fieldName: 'c2x', afterFieldName: 'titleX')
  }

  def "verify that removeReferencesToField deletes entire record when removing references"() {
    given: 'one adjustment in a saved record'
    FieldGUIExtension.withTransaction {
      def adj1 = new FieldInsertAdjustment(fieldName: 'custom1', afterFieldName: 'title')
      def e = new FieldGUIExtension(domainName: 'com.test.FlexType')
      e.adjustments = [adj1]
      e.save()
    }

    when: 'the method is called'
    FieldGUIExtension.removeReferencesToField('com.test.FlexType', 'custom1')

    then: 'the record is deleted'
    FieldGUIExtension.withTransaction {
      assert FieldGUIExtension.list().size() == 0
      true
    }
  }

  def "verify that removeReferencesToField leaves other fields in the list when deleting a single field"() {
    given: 'two adjustments in a saved record'
    FieldGUIExtension.withTransaction {
      def adj1 = new FieldInsertAdjustment(fieldName: 'custom1', afterFieldName: 'title')
      def adj2 = new FieldInsertAdjustment(fieldName: 'custom2', afterFieldName: 'count')
      def e = new FieldGUIExtension(domainName: 'com.test.FlexType')
      e.adjustments = [adj1, adj2]
      e.save()
    }

    when: 'the method is called'
    FieldGUIExtension.removeReferencesToField('com.test.FlexType', 'custom1')

    then: 'the record contains the other field'
    FieldGUIExtension.withTransaction {
      def fg = FieldGUIExtension.findByDomainName('com.test.FlexType')
      assert fg.adjustments.size() == 1
      assert fg.adjustments[0].fieldName == 'custom2'
      true
    }
  }

  def "verify that setAdjustmentsText works"() {
    when: 'the text is set'
    def fge = new FieldGUIExtension()
    fge.adjustmentsText = '''[
      "org.simplemes.eframe.custom.gui.FieldInsertAdjustment",
      {"fieldName":"custom1","afterFieldName":"title"},
      "org.simplemes.eframe.custom.gui.FieldInsertAdjustment",
      {"fieldName":"custom2","afterFieldName":"title"},
      "org.simplemes.eframe.custom.gui.FieldInsertAdjustment",
      {"fieldName":"custom3","afterFieldName":"title"}
    ]'''

    then: 'the values can be parsed'
    def adjustments = fge.adjustments
    adjustments.size() == 3
    adjustments[0] == new FieldInsertAdjustment(fieldName: 'custom1', afterFieldName: 'title')
    adjustments[1] == new FieldInsertAdjustment(fieldName: 'custom2', afterFieldName: 'title')
    adjustments[2] == new FieldInsertAdjustment(fieldName: 'custom3', afterFieldName: 'title')
  }

}
