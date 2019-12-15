package org.simplemes.eframe.custom.gui


import org.simplemes.eframe.custom.domain.FieldGUIExtension
import org.simplemes.eframe.domain.DomainUtils
import org.simplemes.eframe.test.BaseSpecification
import org.simplemes.eframe.test.CompilerTestUtils
import sample.domain.SampleParent
import sample.domain.SampleSubClass

/*
 * Copyright Michael Houston. All rights reserved.
 * Original Author: mph
 *
*/

class FieldAdjusterSpec extends BaseSpecification {

  @SuppressWarnings("unused")
  static specNeeds = [JSON, SERVER]

  //TODO: Find alternative to @Rollback
  def "verify that applyUserAdjustments works for simple case of adjustments on a field order"() {
    given: 'a field order and adjustment'
    def originalFieldOrder = DomainUtils.instance.getStaticFieldOrder(SampleParent)
    def originalSize = originalFieldOrder.size()
    def adj1 = new FieldInsertAdjustment(fieldName: 'custom1', afterFieldName: 'title')
    def extension = new FieldGUIExtension(domainName: SampleParent.name, adjustments: [adj1])
    extension.save()

    when: "field order is calculated"
    def fieldOrder = FieldAdjuster.applyUserAdjustments(SampleParent, originalFieldOrder)

    then: "field order contains the new field in the right place"
    fieldOrder.size() == originalSize + 1
    (fieldOrder.indexOf('title') + 1) == fieldOrder.indexOf('custom1')

    and: 'the original list is not modified'
    DomainUtils.instance.getStaticFieldOrder(SampleParent).size() == originalSize
  }

  //TODO: Find alternative to @Rollback
  def "verify that applyUserAdjustments works when no adjustments are found"() {
    given: 'a field order with not adjustments'
    def originalFieldOrder = DomainUtils.instance.getStaticFieldOrder(SampleParent)

    when: "field order is calculated"
    def fieldOrder = FieldAdjuster.applyUserAdjustments(SampleParent, originalFieldOrder)

    then: 'the original list is returned without cloning'
    originalFieldOrder.is(fieldOrder)
  }

  //TODO: Find alternative to @Rollback
  def "verify that applyUserAdjustments works with multiple adjustments"() {
    given: 'a field order and some adjustments'
    def originalFieldOrder = DomainUtils.instance.getStaticFieldOrder(SampleParent)
    def originalSize = originalFieldOrder.size()
    def adj1 = new FieldInsertAdjustment(fieldName: 'custom1', afterFieldName: 'title')
    def adj2 = new FieldInsertAdjustment(fieldName: 'custom2', afterFieldName: '-')
    def adj3 = new FieldInsertAdjustment(fieldName: 'custom3', afterFieldName: 'gibberish')
    def extension = new FieldGUIExtension(domainName: SampleParent.name, adjustments: [adj1, adj2, adj3])
    extension.save()

    when: "field order is calculated"
    def fieldOrder = FieldAdjuster.applyUserAdjustments(SampleParent, originalFieldOrder)

    then: "field order contains the new fields in the right place"
    fieldOrder.size() == originalSize + 3
    fieldOrder.findIndexOf { it == 'custom1' } == fieldOrder.findIndexOf { it == 'title' } + 1
    fieldOrder.findIndexOf { it == 'custom2' } == 0
    fieldOrder.findIndexOf { it == 'custom3' } == fieldOrder.size() - 1
  }

  //TODO: Find alternative to @Rollback
  def "verify that applyUserAdjustments works with no fieldOrder in domain"() {
    given: 'a class with no fieldOrder'
    def src = """
    package sample
    class Test { }
    """
    def clazz = CompilerTestUtils.compileSource(src)

    and: 'a field order and adjustment'
    def originalFieldOrder = DomainUtils.instance.getStaticFieldOrder(clazz)
    assert originalFieldOrder.size() == 0
    def adj1 = new FieldInsertAdjustment(fieldName: 'custom1', afterFieldName: 'title')
    def extension = new FieldGUIExtension(domainName: clazz.name, adjustments: [adj1])
    extension.save()

    when: "field order is calculated"
    def fieldOrder = FieldAdjuster.applyUserAdjustments(clazz, originalFieldOrder)

    then: "field order contains the new field in the right place"
    fieldOrder.size() == 1
    fieldOrder.findIndexOf { it == 'custom1' } >= 0
  }

  //TODO: Find alternative to @Rollback
  def "verify that applyUserAdjustments works with adjustments to sub-class domain"() {
    given: 'a field order and adjustment'
    def originalFieldOrder = DomainUtils.instance.getStaticFieldOrder(SampleSubClass)
    def originalSize = originalFieldOrder.size()
    def adj1 = new FieldInsertAdjustment(fieldName: 'custom1', afterFieldName: 'subTitle')
    def extension = new FieldGUIExtension(domainName: SampleSubClass.name, adjustments: [adj1])
    extension.save()

    when: "field order is calculated"
    def fieldOrder = FieldAdjuster.applyUserAdjustments(SampleSubClass, originalFieldOrder)

    then: "field order contains the new field in the right place"
    fieldOrder.size() == originalSize + 1
    fieldOrder.findIndexOf { it == 'custom1' } == fieldOrder.findIndexOf { it == 'subTitle' } + 1
  }

  //TODO: Find alternative to @Rollback
  def "verify that applyUserAdjustments works with adjustments to parent class using a sub-class field"() {
    given: 'a field order and adjustment'
    def originalFieldOrder = DomainUtils.instance.getStaticFieldOrder(SampleSubClass)
    def originalSize = originalFieldOrder.size()
    def adj1 = new FieldInsertAdjustment(fieldName: 'custom1', afterFieldName: 'subTitle')
    def extension = new FieldGUIExtension(domainName: SampleParent.name, adjustments: [adj1])
    extension.save()

    when: "field order is calculated"
    def fieldOrder = FieldAdjuster.applyUserAdjustments(SampleSubClass, originalFieldOrder)

    then: "field order contains the new field in the right place"
    fieldOrder.size() == originalSize + 1
    fieldOrder.findIndexOf { it == 'custom1' } == fieldOrder.findIndexOf { it == 'subTitle' } + 1
  }

  // Some constants to make the tests shorter
  static def A = 'AAA'
  static def B = 'BBB'
  static def C = 'CC'
  static def D = 'DDD'
  static def E = 'EE'


  /**
   * Builds a single adjust using a short-hand to aid in clarity of the tests below.
   * For example, 'Remove BBB' for FieldRemoveAdjustment(fieldName: 'BBB').
   * <h3>Values</h3>
   * The values supported and their arguments:
   * <ul>
   *   <li><b>Remove</b> - FieldRemoveAdjustment(fieldName: arg1). </li>
   *   <li><b>Move</b> - FieldMoveAdjustment(fieldName: arg1, afterField: arg2). </li>
   *   <li><b>Insert</b> - FieldInsertAdjustment(fieldName: arg1, afterField: arg2). </li>
   * </ul>
   * @param s The spec.  See values above.
   * @return The adjustment.
   */
  static FieldAdjustmentInterface adj(String s) {
    def tokens = s.tokenize()
    switch (tokens[0]) {
      case 'Remove':
        return new FieldRemoveAdjustment(fieldName: tokens[1])
      case 'Move':
        return new FieldMoveAdjustment(fieldName: tokens[1], afterFieldName: tokens[2])
      case 'Insert':
        return new FieldInsertAdjustment(fieldName: tokens[1], afterFieldName: tokens[2])
    }

    return null
  }

  //TODO: Find alternative to @Rollback
  def "verify that determineDifferences works for supported scenarios"() {
    expect:
    // Build some pretty message in case of failures for complex cases.
    def found = FieldAdjuster.determineDifferences(before, after)
    def msgString = "Failed Case '$msg'.  before = $before, after = $after, expected = $result, found = $found"
    assert FieldAdjuster.determineDifferences(before, after) == result, msgString

    and: 'round-trip works too'
    roundTrip(before, after)

    where:
    before    | after           | result                                                                           | msg
    [A, B, C] | [A, C]          | [adj("Remove $B"), adj("Move $C $A")]                                            | 'Delete middle entry'
    [A, B, C] | [B, C]          | [adj("Remove $A"), adj("Move $B -")]                                             | 'Delete first entry'
    [A, B, C] | [A, B, D, C]    | [adj("Insert $D $B"), adj("Move $C $D")]                                         | 'Add new one in the middle'
    [A, B, C] | [D, A, B, C]    | [adj("Insert $D -"), adj("Move $A $D")]                                          | 'Add new one at beginning'
    [A, B, C] | [A, B, D, E, C] | [adj("Insert $D $B"), adj("Insert $E $D"), adj("Move $C $E")]                    | 'Add 2 new ones in middle'
    [A, B, C] | [A, B, C, D, E] | [adj("Insert $D $C"), adj("Insert $E $D")]                                       | 'Add 2 new ones at end'
    [A, B, C] | [A, C, B]       | [adj("Move $C $A"), adj("Move $B $C")]                                           | 'Move end to middle'
    [A, B, C] | [C, A, B]       | [adj("Move $C -"), adj("Move $A $C")]                                            | 'Move existing to beginning'
    [A, B, C] | [A, D, C, B]    | [adj("Insert $D $A"), adj("Move $C $D"), adj("Move $B $C")]                      | 'Move existing to after new'
    [A, B, C] | [A, D, E, C, B] | [adj("Insert $D $A"), adj("Insert $E $D"), adj("Move $C $E"), adj("Move $B $C")] | 'Move 2 existing to after existing'
    [A, B, C] | [A, C, B]       | [adj("Move $C $A"), adj("Move $B $C")]                                           | 'Move core eto end'
  }

  /**
   * Convenience method to test the round trip diff/adjustment cycle for two lists.
   * @param original
   * @param desired
   */
  boolean roundTrip(List original, List desired) {
    //println "original = $original"
    //println " desired = $desired"
    def adjustments = FieldAdjuster.determineDifferences(original, desired)
    for (adj in adjustments) {
      adj.apply(original)
    }
    //println "adjustments = $adjustments"
    assert original == desired
    return original == desired
  }

  def "verify that determineDifferences handles extensive changes to order are handled correctly"() {
    expect: 'rearrange and insert a new value'
    roundTrip(['eee', 'yyy', '888', '111', 'qqq'],
              ['rrr', '111', 'qqq', '888', 'eee', 'yyy'])

    and: 'a complete mix-up with inserting new value'
    roundTrip(['eee', 'yyy', 'sss', 'bbb', '888', 'iii', '111', 'qqq', 'lll', 'nnn', 'ooo', '444', 'ccc', 'ddd', '999'],
              ['rrr', '111', 'qqq', '888', '777', 'ggg', 'eee', 'yyy', '555', '666', 'jjj', 'sss', 'bbb', '333', 'ppp', 'mmm'])
  }

  def "verify that determineDifferences and apply have acceptable performance"() {
    given: 'many random lists of strings to test'
    def allStringsSize = 30
    def maxListSize = 20
    def allStrings = []
    // Generate the dummy strings
    for (int i = 0; i < allStringsSize; i++) {
      allStrings[i] = (Integer.toString(i, 36)) * (1 + new Random().nextInt(30))
    }

    and: 'the number of iterations to test'
    def maxRuns = 500

    when: 'the iterations are run'
    def start = System.currentTimeMillis()
    for (int run = 0; run < maxRuns; run++) {
      //println "iteration = $iteration"
      // generate a dummy list for each run
      def original = []
      def desired = []
      for (int i = 0; i < maxListSize; i++) {
        def d = allStrings[new Random().nextInt(allStringsSize)]
        if (!desired.contains(d)) {
          desired << d
        }
        def o = allStrings[new Random().nextInt(allStringsSize)]
        if (!original.contains(o)) {
          original << o
        }
      }
      roundTrip(original, desired)
    }

    then: 'the elapsed time per iteration is Ok'
    def elapsed = System.currentTimeMillis() - start
    //println "elapsed per run = ${elapsed/maxRuns}"
    // Takes about 3ms on development box.
    // Since this method is not called a lot, 5ms per run is a reasonable target.
    assert elapsed / maxRuns < 5
  }
}
