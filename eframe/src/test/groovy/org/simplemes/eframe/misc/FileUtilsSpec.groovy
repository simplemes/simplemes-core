package org.simplemes.eframe.misc

import groovy.mock.interceptor.StubFor
import org.simplemes.eframe.test.BaseSpecification
import org.simplemes.eframe.test.UnitTestUtils
import spock.util.mop.ConfineMetaClassChanges

/*
 * Copyright (c) 2018 Simple MES, LLC.  All rights reserved.  See license.txt for license terms.
 */

/**
 * Tests.
 */
class FileUtilsSpec extends BaseSpecification {

  def "verify that convertToOSPath works correctly"() {
    expect: 'the path is converted'
    FileUtils.convertToOSPath("abc/def") == "abc${File.separator}def"
  }

  def "verify that resolveRelativePath works correctly"() {
    expect: 'the path is resolved correctly'
    FileUtils.resolveRelativePath(path) == result

    where:
    path                | result
    'abc/def'           | 'abc/def'
    'abc/def/../ghi'    | 'abc/ghi'
    'abc/def/../../Ghi' | 'abcGhi'
    '../../ghi'         | '../../ghi'
    null                | null
    ''                  | ''
  }

  @SuppressWarnings("JavaIoPackageAccess")
  @ConfineMetaClassChanges([File])
  def "verify that createArchiveDirsIfNeeded creates the correct directories"() {
    given: 'a mock for the File methods to avoid local file system changes'
    def mock = new StubFor(File)
    mock.demand.exists(1..1) { return false }
    mock.demand.mkdirs(1..1) { return true }

    when: 'the method is called'
    mock.use {
      FileUtils.createArchiveDirsIfNeeded('../archives/unit/xyz/object.arc/')
    }

    then: 'the correct File methods were called'
    mock.expect.verify()

    and: 'there are no local file system changes'
    !new File('../archives/unit/xyz').exists()
  }

  @SuppressWarnings("JavaIoPackageAccess")
  @ConfineMetaClassChanges([File])
  def "verify that createArchiveDirsIfNeeded gracefully fails if the directory could not be created"() {
    given: 'a mock for the File methods to avoid local file system changes - mk dirs will fail'
    def mock = new StubFor(File)
    mock.demand.exists(1..1) { return false }
    mock.demand.mkdirs(1..1) { return false }

    when: 'the method is called'
    mock.use {
      FileUtils.createArchiveDirsIfNeeded('../archives/unit/xyz/object.arc/')
    }

    then: 'the a valid exception is triggered'
    def ex = thrown(Exception)
    UnitTestUtils.assertExceptionIsValid(ex, ['103', '../archives/unit/xyz/object.arc'])
  }

  def "verify that createArchiveDirsIfNeeded detects missing argument"() {
    when: 'the method is called'
    FileUtils.createArchiveDirsIfNeeded(null)

    then: 'the a valid exception is triggered'
    def ex = thrown(Exception)
    UnitTestUtils.assertExceptionIsValid(ex, ['dir'])
  }

}
