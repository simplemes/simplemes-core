/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.test

import groovy.transform.ToString
import groovy.util.logging.Slf4j
import org.simplemes.eframe.custom.domain.FlexField
import org.simplemes.eframe.custom.domain.FlexType
import org.simplemes.eframe.data.format.StringFieldFormat
import org.simplemes.eframe.date.DateOnly
import org.simplemes.eframe.date.DateUtils
import org.simplemes.eframe.domain.DomainUtils
import org.simplemes.eframe.misc.ArgumentUtils
import org.simplemes.eframe.misc.TextUtils
import org.simplemes.eframe.security.domain.Role
import org.simplemes.eframe.security.domain.User

/**
 * A simple data generator for test data.  Simplifies the creation of of test data for most
 * domain objects that follow a consistent pattern: few required fields, single key field and a title.
 * <p>
 * Also provides special purpose data generators for common types used in test (e.g. FexType,etc).
 */
@Slf4j
@ToString(includeNames = true, includePackage = false)
class DataGenerator {
  /**
   * The domain class to test.
   */
  Class _domain

  /**
   * The number of records to generate.
   */
  Integer _count = 1

  /**
   * Other fields to set in the domain objects.  Supports g-string like replacement parameters
   * such as: 'ABC${i}abc$r'.  <p>
   * <b>Note:</b> to use the g-string like parameters, you must use a normal string (e.g. single quotes ').
   */
  Map<String, Object> _values = [:]

  /**
   * Sets the domain class to generate record for.
   * @param domain The domain class
   */
  void domain(final Class domain) { this._domain = domain }

  /**
   * Sets the number of records to generate (<b>Default:</b> 1).
   * @param count The number of records to count.
   */
  void count(final Integer count) { this._count = count }

  /**
   * Other fields to set in the domain objects.  Supports g-string like replacement parameters
   * for string fields such as: 'ABC${i}abc$r'.  <p>
   * <b>Note:</b> to use the g-string like parameters, you must use a normal string (e.g. single quotes ').
   * <p>
   * Numeric/date/boolean fields can be specified.  They will be incremented by 1 in all cases for each new record.
   * Booleans will alternate between true and false.
   * @param values The values.
   */
  void values(final Map<String, Object> values) { this._values = values }

  /**
   * Triggers the generation of the data.
   * @param config The configuration.  Elements include: domain (Class), values (Map), count (integer).
   * @return The records generated.
   */
  static List generate(@DelegatesTo(DataGenerator) final Closure config) {
    DataGenerator dataGenerator = new DataGenerator()
    dataGenerator.with config

    ArgumentUtils.checkMissing(dataGenerator._domain, 'domain')
    //def start = System.currentTimeMillis()
    def list = dataGenerator.doGenerate()
    //println "elapsed = ${System.currentTimeMillis() - start}"
    return list
  }

  /**
   * Generates the data.
   * @return The record(s) created.
   */
  List doGenerate() {
    def res = []
    _domain.withTransaction {
      def keys = DomainUtils.instance.getKeyFields(_domain)
      def key = keys[0]
      def numberFormat = "%03d"
      for (iInt in 1.._count) {
        def rInt = _count - iInt + 1
        def i = String.format(numberFormat, iInt)
        def r = String.format(numberFormat, rInt)
        def gStringParams = [i: i, r: r]

        // Fill in the key field and a title
        def object = _domain.newInstance()
        object[key] = _values[key] ?: "ABC$i"
        if (object.hasProperty('title')) {
          object.title = "abc$r"
        }

        // Fill any other values passed in
        def incr = iInt - 1
        _values.each { k, v ->
          if (v instanceof String) {
            object[k] = TextUtils.evaluateGString(v, gStringParams)
          } else if (v instanceof Number) {
            object[k] = v + 1 * incr
          } else if (v instanceof Boolean) {
            object[k] = (incr % 2) == 1
            if (v) {
              // Started with true, so invert the flag
              object[k] = !object[k]
            }
          } else if (v instanceof DateOnly) {
            object[k] = new DateOnly(v.time + incr * DateUtils.MILLIS_PER_DAY)
          } else if (v instanceof Date) {
            object[k] = new Date(v.time + incr * DateUtils.MILLIS_PER_DAY)
          } else {
            // Fall back to just a simple object reference
            object[k] = v
          }
        }

        object.save()
        res << object
      }
    }
    return res
  }

  /**
   * Builds a flex type with all required fields filled in.
   * <h3>Options</h3>
   * <ul>
   *   <li><b>flexType</b> - The flexType (key field) for the record created (<b>Default</b>: 'FLEX1'). </li>
   *   <li><b>fieldName</b> - The field name for the first field in the flex type (<b>Default</b>: 'FIELD1'). </li>
   *   <li><b>fieldFormat</b> - The field format for the first field in the flex type (<b>Default</b>: StringFieldFormat.instance). </li>
   *   <li><b>fieldLabel</b> - The field label (<b>Default</b>: null). </li>
   *   <li><b>fieldCount</b> - The number of fields to generate (<b>Default</b>: 1). Do not use with fieldName</li>
   *   <li><b>defaultFlexType</b> - The default flex type is set (<b>Default</b>: false). </li>
   * </ul>
   *
   * @param options The options. See above.  Optional.
   * @return
   */
  @SuppressWarnings("GroovyAssignabilityCheck")
  static FlexType buildFlexType(Map options = null) {
    def flexType = null
    def fieldCount = options?.fieldCount ?: 1
    def dflt = options?.defaultFlexType ?: false
    FlexType.withTransaction {
      flexType = new FlexType(flexType: options?.flexType ?: 'FLEX1', defaultFlexType: dflt)
      def format = options?.format ?: options?.fieldFormat ?: StringFieldFormat.instance
      def label = options?.fieldLabel
      for (int i = 0; i < fieldCount; i++) {
        flexType.fields << new FlexField(fieldName: options?.fieldName ?: "FIELD${i + 1}",
                                         fieldFormat: format,
                                         fieldLabel: label,
                                         sequence: (10 + i * 10))
      }
      flexType.save()
    }

    return flexType
  }

  /**
   * Creates one user with one role.  The user name is used to determine the role.
   * 'none' has no roles.
   *
   * @param userName The user name (must be 'designer', 'customizer', 'manager', or 'none').
   */
  static void buildTestUser(String userName) {
    User.withTransaction {
      def role = null
      if (userName != 'none') {
        def desiredRole = userName.toUpperCase()
        role = Role.findByAuthority(desiredRole)
        assert role
      }

      def user = new User(userName: userName, password: userName, passwordExpired: false)
      if (role) {
        user.addToUserRoles(role)
      }
      user.save()
    }
  }
}
