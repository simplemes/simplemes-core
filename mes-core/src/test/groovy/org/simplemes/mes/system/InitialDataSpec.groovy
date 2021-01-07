package org.simplemes.mes.system

import org.simplemes.eframe.application.Holders
import org.simplemes.eframe.application.InitialDataLoader
import org.simplemes.eframe.security.domain.Role
import org.simplemes.eframe.security.domain.User
import org.simplemes.eframe.test.BaseSpecification
import org.simplemes.eframe.test.annotation.Rollback

/*
 * Copyright Michael Houston 2017. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Test.
 *
 */
class InitialDataSpec extends BaseSpecification {

  @SuppressWarnings("unused")
  static specNeeds = SERVER

  /**
   * The list of roles expected for a new MES DB.
   */
  static final List<String> mesRoles = ['SUPERVISOR', 'ENGINEER', 'LEAD', 'OPERATOR']

  @Rollback
  def "verify that the mes roles are created"() {
    when: 'initial data is loaded'
    def res = InitialData.initialDataLoad()

    then: 'the roles exist'
    for (role in mesRoles) {
      assert Role.findByAuthority(role)
    }

    and: 'the records listed are correct'
    res.Role.containsAll(mesRoles)
  }

  def "verify that the admin user is created after the mes roles so the admin will have those roles"() {
    given: 'any user or roles are removed'
    User.withTransaction {
      User.list()*.delete()
      Role.list()*.delete()
      assert User.count() == 0
      assert Role.count() == 0
    }

    when: 'initial data is loaded'
    User.withTransaction {
      def loader = Holders.applicationContext.getBean(InitialDataLoader)
      loader.dataLoad()
    }

    then: 'the new roles exist'
    for (role in mesRoles) {
      assert Role.findByAuthority(role)
    }

    and: 'the admin user is assigned all roles including the mes-core roles'
    def user = User.findByUserName('admin')
    for (role in mesRoles) {
      assert user.userRoles.find { it.authority == role }
    }
  }

}
