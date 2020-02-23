/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.test

import io.micronaut.views.freemarker.FreemarkerViewsRenderer
import org.simplemes.eframe.application.Holders
import sample.controller.SampleController

/**
 * Tests. Use of the MockBean.
 */
class MockBeanSpec extends BaseSpecification {

  @SuppressWarnings("unused")
  def specNeeds = SERVER

  def "verify that a mocked bean will find the real beans for other non-mocked beans"() {
    given: 'a mocked bean'
    new MockBean(this, FreemarkerViewsRenderer, this).install()

    when: 'another bean is requested from the application context'
    def bean = Holders.applicationContext.getBean(SampleController)

    then: 'the bean is found'
    bean instanceof SampleController
  }
}
