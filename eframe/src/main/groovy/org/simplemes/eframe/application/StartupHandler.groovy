package org.simplemes.eframe.application

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import groovy.util.logging.Slf4j
import io.micronaut.discovery.event.ServiceStartedEvent
import io.micronaut.runtime.event.annotation.EventListener
import io.micronaut.scheduling.annotation.Async
import org.simplemes.eframe.date.EFrameDateFormat
import org.simplemes.eframe.json.HibernateAwareJacksonModule

import javax.inject.Singleton

/*
 * Copyright Michael Houston 2018. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * This bean is executed on startup.  This is used to load initial data and handle similar actions.
 *
 * <h3>Logging</h3>
 * The logging for this class that can be enabled:
 * <ul>
 *   <li><b>debug</b> - Prints all beans found on startup. </li>
 * </ul>
 */
@Slf4j
@Singleton
class StartupHandler {


  /**
   * Executed on Startup.  Triggers the initial data load process.
   * @param event
   */
  @EventListener
  @Async
  void onStartup(ServiceStartedEvent event) {
    log.debug('Server Started with configuration {}', Holders.configuration)

    // Start Initial data load.
    def loader = Holders.applicationContext.getBean(InitialDataLoader)
    //println "loader = $loader"
    loader.dataLoad()

    if (log.debugEnabled) {
      log.debug('All Beans: {}', Holders.applicationContext.allBeanDefinitions*.name)
    }
    //def ds = Holders.applicationContext.getBean(javax.sql.DataSource)
    //println "ds = $ds, ${ds.getClass()}"


    // Modify the Object mapper
    def mapper = Holders.applicationContext.getBean(ObjectMapper)
    configureJacksonObjectMapper(mapper)

  }

  /**
   * This method should be called from your Application class before the   Micronaut.run() method is called.
   * It initializes some settings that must be in place before Hibernate and Micronaut startup.
   */
  static void preStart() {
    TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
  }

  /**
   * Configures the Jackson object mapper with the settings we need.  Includes adding the hibernate 5 module to
   * avoid infinite recursion.
   * @param mapper The mapper.
   */
  static void configureJacksonObjectMapper(ObjectMapper mapper) {
    mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
    def format = new EFrameDateFormat()
    format.setTimeZone(Holders.globals.timeZone)
    mapper.setDateFormat(format)
    mapper.registerModule(new HibernateAwareJacksonModule())

/*  // Don't use Hibernate5Module for Jackson.  Seems to not solve the infinite recursion problem when serializing parent/child.
    // Also, doesn't help with child creation/update for REST POST cases.
    def hibernate5Module = new Hibernate5Module()
    hibernate5Module.disable(Hibernate5Module.Feature.FORCE_LAZY_LOADING)
    hibernate5Module.enable(Hibernate5Module.Feature.SERIALIZE_IDENTIFIER_FOR_LAZY_NOT_LOADED_OBJECTS)
    //println "hibernate5Module1 = ${hibernate5Module.isEnabled(Hibernate5Module.Feature.FORCE_LAZY_LOADING)}"
    //println "hibernate5Module1 = ${hibernate5Module.isEnabled(Hibernate5Module.Feature.SERIALIZE_IDENTIFIER_FOR_LAZY_NOT_LOADED_OBJECTS)}"
    mapper.registerModule(hibernate5Module)
    //println "startup mapper = $mapper"
*/

  }

}