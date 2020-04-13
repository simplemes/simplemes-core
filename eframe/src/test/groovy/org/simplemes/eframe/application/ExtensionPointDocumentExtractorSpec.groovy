/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.application

import org.simplemes.eframe.test.BaseSpecification
import org.simplemes.eframe.test.MockFile

/**
 * Tests.
 */
class ExtensionPointDocumentExtractorSpec extends BaseSpecification {

  def "verify that the utility process class files and writes the output"() {
    given: 'a mock folder with some classes'
    def mockFiles = ['sample/service/OrderService.class', 'sample/SampleBasicStatus.class']
    def folder = new MockFile(path: ExtensionPointDocumentExtractor.CLASSES_DIR, files: mockFiles)

    and: 'a mocked output file'
    def stringWriter = new StringWriter()
    def outputFile = new MockFile(path: 'extensions.adoc', writer: stringWriter)

    when: 'the folder is processed'
    ExtensionPointDocumentExtractor.processClasses(folder, outputFile)

    then: 'output file is correct'
    def s = stringWriter.toString()
    s.contains('OrderService.release(')
  }
}
