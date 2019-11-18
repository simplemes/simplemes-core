package org.simplemes.eframe.web

import org.simplemes.eframe.test.BaseSpecification

/*
 * Copyright Michael Houston 2018. All rights reserved.
 * Original Author: mph
 *
*/

/**
 *
 */
class PanelUtilsSpec extends BaseSpecification {
  def "verify that isPanel handles the supported cases"() {
    expect:
    PanelUtils.isPanel(group) == result

    where:
    group       | result
    'group:abc' | true
    'groupAbc'  | false
    'abc'       | false
    'group:'    | false
  }

  def "verify that determinePanelName handles the supported cases"() {
    expect:
    PanelUtils.getPanelName(group) == panelName

    where:
    group        | panelName
    'group:abc'  | 'abc'
    null         | null
    'short'      | null
    '-group:abc' | null
    'group:'     | null
    'notgroup:'  | null
  }

  def "verify that organizeFieldsIntoPanels handles the supported cases"() {
    when: 'the panels are organized'
    def res = PanelUtils.organizeFieldsIntoPanels(list)

    then: 'the result is the same as expected'
    res == expected

    and: 'the keySets are the same - in the right order'
    def set = res.keySet()
    def expectedSet = expected.keySet()
    set == expectedSet
    // Need to check the order since Groovy comparison of keySet() is order independent
    for (int i = 0; i < res.size(); i++) {
      assert set[i] == expectedSet[i]
    }

    where:
    list                                                          | expected
    null                                                          | [:]
    []                                                            | [:]
    ['a', 'b']                                                    | [:]
    ['a', 'group:abc', 'b', 'c']                                  | [main: ['a'], abc: ['b', 'c']]
    ['group:main', 'a', 'group:abc', 'b', 'c']                    | [main: ['a'], abc: ['b', 'c']]
    ['group:abc', 'a', 'group:main', 'b', 'c']                    | [abc: ['a'], main: ['b', 'c']]
    ['group:abc', 'x', 'group:main', 'a', 'group:main', 'b', 'c'] | [abc: ['x'], main: ['a', 'b', 'c']]
  }

}
