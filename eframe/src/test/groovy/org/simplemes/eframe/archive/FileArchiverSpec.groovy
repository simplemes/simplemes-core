/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.archive

import ch.qos.logback.classic.Level
import groovy.json.JsonSlurper
import org.simplemes.eframe.application.EFrameConfiguration
import org.simplemes.eframe.application.Holders
import org.simplemes.eframe.archive.domain.ArchiveLog
import org.simplemes.eframe.date.DateOnly
import org.simplemes.eframe.misc.FileFactory
import org.simplemes.eframe.misc.FileUtils
import org.simplemes.eframe.misc.TypeUtils
import org.simplemes.eframe.test.BaseSpecification
import org.simplemes.eframe.test.MockAppender
import org.simplemes.eframe.test.MockFileFactory
import org.simplemes.eframe.test.UnitTestUtils
import org.simplemes.eframe.test.annotation.Rollback
import sample.domain.AllFieldsDomain
import sample.domain.SampleChild
import sample.domain.SampleParent

/**
 * Tests.
 */
class FileArchiverSpec extends BaseSpecification {

  @SuppressWarnings("unused")
  def dirtyDomains = [SampleParent, AllFieldsDomain, ArchiveLog]

  /**
   * The string writer to contain the mocked file contents.
   */
  StringWriter stringWriter


  def setup() {
    // Most tests will use the Mock file system
    stringWriter = new StringWriter()
    FileFactory.instance = new MockFileFactory(stringWriter)

    // The Jackson mapper is setup in the mocked application context
    //new MockObjectMapper(this).install()
  }

  void cleanup() {
    // reset the archive options to their defaults
    Holders.configuration.archive = new EFrameConfiguration.Archive()
    FileFactory.instance = new FileFactory()
  }

  def "verify that default file name is used"() {
    given: 'a domain object'
    def o1 = new AllFieldsDomain(name: 'ABC', title: 'abc')

    when: 'the file name base is created'
    def ref = new FileArchiver().makeArchiveRefBase(o1)

    then: 'it matches the date'
    def c = new GregorianCalendar()
    def year = c.get(Calendar.YEAR)
    def month = String.format('%02d', c.get(Calendar.MONTH) + 1)
    def day = String.format('%02d', c.get(Calendar.DAY_OF_MONTH))
    ref == "${year}-${month}-${day}/ABC"
  }

  @SuppressWarnings("GStringExpressionWithinString")
  def "verify that the file name is configurable"() {
    given: 'a domain object'
    def o1 = new AllFieldsDomain(name: 'ABC', title: 'abc', count: 237)

    and: 'use current hour in file path and use property from the domain class'
    Holders.configuration.archive.folderName = '#{year}-${month}-${day}-${hour}-#{allFieldsDomain.title}'
    Holders.configuration.archive.fileName = '${allFieldsDomain?.count ?: object.toString()}'

    when: 'the file name base is created'
    def ref = new FileArchiver().makeArchiveRefBase(o1)

    then: 'it matches the date with hour and the sequence'
    def c = new GregorianCalendar()
    def year = c.get(Calendar.YEAR)
    def month = String.format('%02d', c.get(Calendar.MONTH) + 1)
    def day = String.format('%02d', c.get(Calendar.DAY_OF_MONTH))
    def hour = String.format('%02d', c.get(Calendar.HOUR_OF_DAY))
    ref == "${year}-${month}-${day}-${hour}-abc/${o1.count}"
  }


  def "verify that basic archiving works with multiple top-level domain objects and children - round trip - real file system"() {
    given: 'the archiver will use the real file system'
    FileFactory.instance = new FileFactory()

    and: 'a domain with nested children'
    def sampleParent1 = null
    SampleParent.withTransaction {
      def afd2 = new AllFieldsDomain(name: 'ABC-02').save()
      sampleParent1 = new SampleParent(name: 'SAMPLE', title: 'Sample')
      sampleParent1.sampleChildren << new SampleChild(key: '123')
      sampleParent1.allFieldsDomains << afd2
      sampleParent1.save()
    }

    and: 'related records'
    def sampleParent2 = null
    SampleParent.withTransaction {
      sampleParent2 = new SampleParent(name: 'XYZ', title: 'xyz')
      sampleParent2.sampleChildren << new SampleChild(key: '456')
      sampleParent2.save()
    }

    and: 'the archiver is configured to write a simple file to the build directory'
    Holders.configuration.archive.topFolder = 'build/archives'
    Holders.configuration.archive.folderName = 'unit'
    Holders.configuration.archive.fileName = 'one'

    when: 'the domains are archived using a real file system archiver'
    def ref = null
    SampleParent.withTransaction {
      def archiver = new FileArchiver()
      archiver.archive(sampleParent1)
      archiver.archive(sampleParent2)
      ref = archiver.close()
    }
    and: 'the archive filename is determined'
    def path = FileArchiver.makePathFromReference(ref)
    def file = new File(path)

    then: 'the records are deleted and foreign objects are left unchanged'
    SampleParent.withTransaction {
      assert SampleParent.count() == 0
      assert SampleChild.count() == 0
      assert AllFieldsDomain.count() == 1
      assert ArchiveLog.count() == 1
      true
    }

    and: 'the archive file is created'
    file.exists()

    cleanup:
    file?.delete()
  }

  @SuppressWarnings("GroovyAssignabilityCheck")
  def "verify that archive works with related objects"() {
    given: 'a domain with some related files - an AllFieldsDomain with the same key field'
    def sampleParent1 = null
    SampleParent.withTransaction {
      new AllFieldsDomain(name: 'SAMPLE').save()
      sampleParent1 = new SampleParent(name: 'SAMPLE', title: 'Sample')
      sampleParent1.save()
    }

    when: 'the top-level is archived, along with related objects'
    SampleParent.withTransaction {
      def archiver = new FileArchiver()
      archiver.archive(sampleParent1)
      archiver.close()
    }

    then: 'the file content is created with the correct values'
    def s = stringWriter.toString()
    //println "JSON = ${groovy.json.JsonOutput.prettyPrint(s)}"
    def json = new JsonSlurper().parse(s.bytes)

    json.size() == 4

    and: 'the related records are in the JSON file'
    json[2] == AllFieldsDomain.name
    json[3].name == sampleParent1.name

    and: 'the related records are deleted'
    SampleParent.withTransaction {
      assert SampleParent.count() == 0
      assert AllFieldsDomain.count() == 0
      true
    }
  }

  def "verify that the archiver can unarchive using the original formatting logic"() {
    given: 'JSON in the original framework 1.0 format'
    def s = """ [
      "sample.domain.SampleParent",
      {
          "name": "SAMPLE",
          "title": "Sample",
          "notes": null,
          "notDisplayed": null,
          "dateCreated": "2019-01-02T09:48:16.408-0500",
          "lastUpdated": "2019-01-02T09:48:16.408-0500",
          "allFieldsDomain": null,
          "allFieldsDomains": null,
          "sampleChildren": [
              
          ],
          "id": 3
      },
      "sample.domain.AllFieldsDomain",
      {
          "name": "A-SAMPLE",
          "title": "a-sample",
          "qty": 12.2,
          "count": 237,
          "enabled": true,
          "dateTime": "${UnitTestUtils.SAMPLE_ISO_TIME_STRING}",
          "dueDate": "${UnitTestUtils.SAMPLE_ISO_DATE_ONLY_STRING}",
          "lastUpdated": "2019-01-02T09:48:16.406-0500",
          "id": 247
      }
    ]
    """

    and: 'the text is available to the MockFile reader'
    stringWriter.write(s)

    when: 'the file is unarchived'
    def objects = null
    SampleParent.withTransaction {
      objects = new FileArchiver().unarchive('dummy.arc')
    }

    then: 'the data is unarchived'
    objects.size() == 2

    and: 'the related records are created'
    SampleParent.withTransaction {
      assert SampleParent.count() == 1
      assert AllFieldsDomain.count() == 1

      def p = SampleParent.findByName('SAMPLE')
      p.title == 'Sample'

      def afd = AllFieldsDomain.findByName('A-SAMPLE')
      afd.title == 'a-sample'
      afd.qty == 12.2
      afd.count == 237
      afd.dateTime == new Date(UnitTestUtils.SAMPLE_TIME_MS)
      afd.dueDate == new DateOnly(UnitTestUtils.SAMPLE_DATE_ONLY_MS)
      true
    }
  }

  def "verify that the bad input syntax fails gracefully"() {
    given: 'bad JSON in the mock file'
    def s = "[{]"
    stringWriter.write(s)

    when: 'the file is unarchived'
    def objects = null
    SampleParent.withTransaction {
      objects = new FileArchiver().unarchive('dummy.arc')
    }

    then: 'the right exception is thrown'
    def ex = thrown(Exception)
    UnitTestUtils.assertExceptionIsValid(ex, ['dummy.arc', 'JsonMappingException'])
  }

  def "verify that unarchive can create the objects with saving them"() {
    given: 'JSON in the original framework 1.0 format'
    def s = """ [
      "sample.domain.SampleParent",
      {
          "name": "SAMPLE",
          "title": "Sample"
      }
    ]
    """

    and: 'the text is available to the MockFile reader'
    stringWriter.write(s)

    when: 'the file is unarchived'
    List objects = null
    SampleParent.withTransaction {
      objects = new FileArchiver().unarchive('dummy.arc', false)
    }

    then: 'the data is note saved'
    objects.size() == 1
    objects[0].id == null

    and: 'no records are created'
    SampleParent.withTransaction {
      assert SampleParent.count() == 0
      true
    }
  }

  def "verify that unarchive can gracefully detect validation errors"() {
    given: 'JSON in the original framework 1.0 format - missing required field'
    def s = """ [
      "sample.domain.SampleParent",
      {
          "title": "Sample"
      }
    ]
    """

    and: 'the text is available to the MockFile reader'
    stringWriter.write(s)

    when: 'the file is unarchived'
    SampleParent.withTransaction {
      new FileArchiver().unarchive('dummy.arc')
    }

    then: 'the right exception is thrown'
    def ex = thrown(Exception)
    UnitTestUtils.assertExceptionIsValid(ex, ['name', 'missing', 'dummy.arc'])
  }

  def "verify that archive fails with missing domain object ID"() {
    given: 'a domain that is not archivable'
    def sampleParent = new SampleParent(name: 'TYPO')

    when: 'the object is archived'
    def archiver = new FileArchiver()
    archiver.archive(sampleParent)
    archiver.close()

    then: 'an exception is thrown'
    def e = thrown(Exception)
    //error.128.message=The domain {0} {1} has no ID.  This record must be saved before it can be processed.
    UnitTestUtils.assertExceptionIsValid(e, ['TYPO', 'saved', SampleParent.simpleName], 128)
  }

  def "verify that archive fails with non-domain object"() {
    when: 'the object is archived'
    def archiver = new FileArchiver()
    archiver.archive('XYZ')
    archiver.close()

    then: 'an exception is thrown'
    def e = thrown(Exception)
    //      throw new IllegalArgumentException("Cannot archive a ${domainObject.class}.  It is not a Hibernate domain class.")
    UnitTestUtils.assertExceptionIsValid(e, ['string', 'not', 'domain'])
  }

  @Rollback
  def "verify that attempts to save same object twice generates a different file name each time"() {
    given: 'a domain object is created'
    def sample = new SampleParent(name: 'ABC').save()

    when: 'the object is archived with the exists method returning true once'
    def archiver = new FileArchiver()
    archiver.archive(sample)
    def reference1 = archiver.close()

    and: 'the same domain object is created and archived again to trigger the same base file name'
    def otherStringWriter = new StringWriter()
    FileFactory.instance = new MockFileFactory(otherStringWriter, 1)
    //def sample2 = new SampleParent(name: 'ABC').save()
    def archiver2 = new FileArchiver()
    archiver2.archive(sample)
    def reference2 = archiver2.close()

    then: 'the two file names generated are different'
    reference1 != reference2
  }

  @Rollback
  def "verify that the verification of the archive can be disabled"() {
    given: 'a domain object is created'
    def sample = new SampleParent(name: 'ABC').save()

    and: 'the verify option is disabled in the configuration options'
    Holders.configuration.archive.verify = false

    when: 'the object is archived'
    def archiver = new FileArchiver()
    archiver.archive(sample)
    archiver.close()

    then: 'the verification did not take place'
    !archiver.verified
  }

  @Rollback
  def "verify that the verification of the archive can be enabled"() {
    given: 'a domain object is created'
    def sample = new SampleParent(name: 'ABC').save()

    and: 'the verify option is disabled in the configuration options'
    Holders.configuration.archive.verify = true

    when: 'the object is archived'
    def archiver = new FileArchiver()
    archiver.archive(sample)
    archiver.close()

    then: 'the verification did take place'
    archiver.verified
  }

  @Rollback
  def "verify that the ArchiveLog record creation can be disabled"() {
    given: 'a domain object is created'
    def sample = new SampleParent(name: 'ABC').save()

    and: 'the log option is disabled in the configuration options'
    Holders.configuration.archive.log = false

    when: 'the object is archived'
    def archiver = new FileArchiver()
    archiver.archive(sample)
    archiver.close()

    then: 'the ArchiveLog record is not written'
    ArchiveLog.list().size() == 0
  }

  @Rollback
  def "verify that cancel preserves records and does not leave file partially created"() {
    given: 'a domain object is created'
    def sample = new SampleParent(name: 'XYZ').save()

    when: 'the object is archived'
    def archiver = new FileArchiver()
    archiver.archive(sample)

    and: 'the archive is cancelled'
    archiver.cancel()

    then: 'the record is not deleted'
    SampleParent.list().size() == 1

    and: 'the ArchiveLog record is not written'
    ArchiveLog.list().size() == 0

    and: 'the mock file created is deleted'
    FileFactory.instance.lastFileCreated.deleted
  }

  @Rollback
  def "verify that the debug logging works"() {
    given: 'a mock appender for Info level only'
    def mockAppender = MockAppender.mock(FileArchiver, Level.DEBUG)

    and: 'a domain to be archived'
    def sample = new SampleParent(name: 'XYZ').save()

    when: 'the object is archived'
    def archiver = new FileArchiver()
    archiver.archive(sample)
    archiver.close()
    sample.delete()  // Force the DB to remove the record

    then: 'the debug message is written'
    UnitTestUtils.assertContainsAllIgnoreCase(mockAppender.message, ['DEBUG', 'archived', TypeUtils.toShortString(sample)])

    when: 'the object is unarchived'
    mockAppender.clearMessages()
    archiver.unarchive('dummy.arc')

    then: 'the debug message is written'
    UnitTestUtils.assertContainsAllIgnoreCase(mockAppender.message, ['DEBUG', 'unarchived', TypeUtils.toShortString(sample)])
  }

  @Rollback
  def "verify that a write error preserves records and does not leave file partially created"() {
    given: 'a domain object is created'
    def sample = new SampleParent(name: 'XYZ').save()

    and: 'a mock file that will fail'
    FileFactory.instance.writesShouldFail = true

    when: 'the object is archived'
    new FileArchiver().archive(sample)

    then: 'an exception is thrown'
    thrown(Exception)

    and: 'the mock file created is deleted'
    FileFactory.instance.lastFileCreated.deleted
  }

  def "verify that makePathFromReference works"() {
    given: 'a specific top folder in the config'
    Holders.configuration.archive.topFolder = '../archives'

    expect: 'the right default is used'
    FileArchiver.makeReferenceFromPath(FileUtils.convertToOSPath(path)) == result

    where:
    path                              | result
    '../archives/2018-17-12/arc1.arc' | '2018-17-12/arc1.arc'
    '2018-17-12/arc1.arc'             | '2018-17-12/arc1.arc'
    'arc1.arc'                        | 'arc1.arc'
  }

  def "verify that findAllArchives uses the correct default directory"() {
    given: 'a top folder configuration entry'
    Holders.configuration.archive.topFolder = '../arch'

    and: 'a simulate file system directory structure - two-level folders with archive files in both levels'
    FileFactory.instance.simulatedFiles = ['../arch'                : ['../arch/2018-03-23'],
                                           '../arch/2018-03-23'     : ['ref1.arc', 'ref2.arc', '../arch/2018-03-23/unit'],
                                           '../arch/2018-03-23/unit': ['ref1a.arc', 'ref2a.arc']]

    expect: 'the right default is used'
    def files = new FileArchiver().findAllArchives()
    files.size() == 4
    files.contains('2018-03-23/ref1.arc')
    files.contains('2018-03-23/ref2.arc')
    files.contains('2018-03-23/unit/ref1a.arc')
    files.contains('2018-03-23/unit/ref2a.arc')
  }

}
