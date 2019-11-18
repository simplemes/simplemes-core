package org.simplemes.eframe.test

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import groovy.transform.ToString
import org.simplemes.eframe.application.Holders
import org.simplemes.eframe.date.EFrameDateFormat
import org.simplemes.eframe.json.HibernateAwareJacksonModule

/*
 * Copyright Michael Houston 2018. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Mocks the Jackson ObjectMapper instance in the Holders for use in tests.
 * This is a real ObjectMapper.
 * This class restores the original Holders.applicationContext during BaseSpecification.cleanup().
 * <pre>
 *   new MockObjectMapper(this).install()
 * </pre>
 * <p>
 * This will create a new instance of AssetPipelineService and return it when the getBean(AssetPipelineService)
 * method is called.  You can provide your own instance of the bean if needed.
 */
@ToString(includePackage = false, includeNames = true)
class MockObjectMapper extends ObjectMapper implements AutoCleanupMockInterface {

  /**
   * Holds the original ObjectMapper in use before the value was mocked in the Holders for unit tests.
   * Will be auto-restored by the BaseSpecification.cleanup() method.
   */
  ObjectMapper originalObjectMapper

  /**
   * The test specification that needs the mock.
   */
  BaseSpecification baseSpec

  /**
   * Basic constructor.
   * @param baseSpec The test specification that needs the mock (usually this).
   */
  MockObjectMapper(BaseSpecification baseSpec) {
    this.baseSpec = baseSpec

    disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
    def format = new EFrameDateFormat()
    format.setTimeZone(Holders.globals.timeZone)
    setDateFormat(format)
    enable(SerializationFeature.INDENT_OUTPUT)
    registerModule(new HibernateAwareJacksonModule())
  }

  /**
   * Installs the object mapper in the Holders for use by tests.
   * Also registers a cleanup method.
   */
  void install() {
    originalObjectMapper = Holders.objectMapper
    Holders.objectMapper = this
    baseSpec.registerAutoCleanup(this)
  }

  /**
   * Performs the cleanup action.
   * @param testSpec The test that requests the cleanup.
   */
  @Override
  void doCleanup(BaseSpecification testSpec) {
    Holders.objectMapper = originalObjectMapper
  }

}
