/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.application

import com.fasterxml.jackson.databind.ObjectMapper
import groovy.util.logging.Slf4j
import io.micronaut.context.ApplicationContext
import io.micronaut.context.env.Environment
import io.micronaut.context.event.StartupEvent
import io.micronaut.http.HttpRequest
import io.micronaut.http.context.ServerRequestContext
import io.micronaut.runtime.event.annotation.EventListener

import javax.inject.Singleton
import javax.sql.DataSource

/**
 * A place to hold specific global values.  Simplifies access to things like the ApplicationContext.
 */
@Slf4j
@Singleton
class Holders {

  /**
   * The current application context.
   */
  static ApplicationContext applicationContext

  /**
   * The current environment.  This field holds the real environment.  Null if not in a real server instance.
   */
  static Environment environment

  /**
   * A fallback environment for unit/hibernate only tests and such.
   */
  static Environment fallbackEnvironment

  /**
   * The configuration.
   */
  static EFrameConfiguration configuration = new EFrameConfiguration()

  /**
   * The global settings.  Mainly app-level defaults.
   */
  static Globals globals = new Globals()

  /**
   * Handles application startup events.
   * @param event The event.
   */
  @EventListener
  void onStartup(StartupEvent event) {
    applicationContext = event.source as ApplicationContext
    environment = applicationContext.environment
    configuration = applicationContext.getBean(EFrameConfiguration)
  }

  /**
   * Returns the current data source. If hibernate is not started, then this will return null.
   * @return The data store.
   */
  static DataSource getDataSource() {
    return applicationContext?.getBean(DataSource)
  }

  /**
   * Convenience method to get the bean from the Application Context.
   * @return The bean.
   */
  static <T> T getBean(Class<T> c) {
    return applicationContext?.getBean(c)
  }

  /**
   * Returns the current environment.  Supports non-server scenarios.
   * @return The environment.
   */
  static Environment getEnvironment() {
    if (!environment) {
      return fallbackEnvironment
    }
    return environment
  }

  /**
   * Returns true if the current environment is dev.
   * @return True if dev.
   */
  static boolean isEnvironmentDev() {
    return getEnvironment()?.activeNames?.contains('dev')
  }

  /**
   * Returns true if the current environment is test.
   * @return True if test.
   */
  static boolean isEnvironmentTest() {
    return getEnvironment()?.activeNames?.contains('test')
  }

  /**
   * A mock request, returned when the environment is test.
   */
  static HttpRequest mockRequest = null

  /**
   * Returns the current request for this thread.
   * @return The request.  Can be null.
   */
  static HttpRequest getCurrentRequest() {
    if (environmentTest && mockRequest) {
      return mockRequest
    }
    def request = ServerRequestContext.currentRequest()
    if (request) {
      return request.get()
    }
    return null
  }

  /**
   * A cached copy of the object mapper.
   */
  static ObjectMapper objectMapper

  /**
   * Returns the object mapper in use for this server.
   * @return The Jackson object mapper.
   */
  static ObjectMapper getObjectMapper() {
    if (!objectMapper) {
      objectMapper = applicationContext?.getBean(ObjectMapper)
    }
    return objectMapper
  }

}
