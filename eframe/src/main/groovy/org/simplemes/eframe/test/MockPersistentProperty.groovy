/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.test

import groovy.transform.ToString
import org.simplemes.eframe.domain.PersistentProperty
import org.spockframework.mock.IDefaultResponse
import org.spockframework.mock.IMockInvocation
import org.spockframework.mock.ZeroOrNullResponse
import spock.mock.DetachedMockFactory

/**
 * A very simple mock/stub of a PersistentProperty for mocked domain use.
 * This is used mainly to provide a list of persistent fields.
 */
@ToString(includePackage = false, includeNames = true)
class MockPersistentProperty implements IDefaultResponse {

  /**
   * The actual mock entity.
   */
  Object mock

  /**
   * The name of the persistent entity.
   */
  String name

  /**
   * The property's class (type).
   */
  Class type

  /**
   * Basic constructor.
   *
   * @param name The name of the persistent entity/
   * @param clazz The class to pretend is a domain class.
   */
  MockPersistentProperty(String name, Class clazz) {
    this.name = name
    this.type = clazz
    def mockFactory = new DetachedMockFactory()
    mock = mockFactory.Mock([defaultResponse: this], PersistentProperty)
  }

  @Override
  Object respond(IMockInvocation invocation) {
    // No need to implement a response since the real fields above will
    // generate a good getName() and getType() methods.
/*
    //println "invocation.method.name = $invocation.method.name"
    def name = invocation.method.name
    if (name == 'getName') {
      return this.name
    } else if (name == 'getType') {
      return type
    }
*/
    return ZeroOrNullResponse.INSTANCE.respond(invocation)
  }
}
