package org.simplemes.eframe.test

import ch.qos.logback.classic.Level
import com.fasterxml.jackson.databind.ObjectMapper
import geb.spock.GebSpec
import groovy.util.logging.Slf4j
import io.micronaut.context.ApplicationContext
import io.micronaut.context.env.DefaultEnvironment
import io.micronaut.core.convert.ConversionService
import io.micronaut.http.HttpRequest
import io.micronaut.http.simple.SimpleHttpHeaders
import io.micronaut.http.simple.SimpleHttpParameters
import io.micronaut.runtime.server.EmbeddedServer
import org.junit.Rule
import org.junit.rules.TestName
import org.simplemes.eframe.application.Holders
import org.simplemes.eframe.application.InitialDataLoader
import org.simplemes.eframe.application.StartupHandler
import org.simplemes.eframe.controller.ControllerUtils
import org.simplemes.eframe.custom.AdditionHelper
import org.simplemes.eframe.custom.domain.FieldExtension
import org.simplemes.eframe.custom.domain.FieldGUIExtension
import org.simplemes.eframe.custom.gui.FieldInsertAdjustment
import org.simplemes.eframe.data.DateOnlyType
import org.simplemes.eframe.data.EFrameHibernatePersistenceInterceptor
import org.simplemes.eframe.data.EncodedType
import org.simplemes.eframe.data.format.BasicFieldFormat
import org.simplemes.eframe.data.format.StringFieldFormat
import org.simplemes.eframe.date.DateOnly
import org.simplemes.eframe.domain.DomainFinder
import org.simplemes.eframe.domain.DomainUtils
import org.simplemes.eframe.i18n.GlobalUtils
import org.simplemes.eframe.json.HibernateAwareJacksonModule
import org.simplemes.eframe.misc.ArgumentUtils
import org.simplemes.eframe.misc.LogUtils
import org.simplemes.eframe.misc.TypeUtils
import org.simplemes.eframe.preference.domain.UserPreference
import org.simplemes.eframe.security.SecurityUtils
import org.simplemes.eframe.security.domain.User
import spock.lang.Shared

/*
 * Copyright Michael Houston 2018. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * The base class for most non-GUI tests.  This supports starting an embedded server and the hibernate data store,
 * when needed.  These needs are flagged in your test classes with several static variables:
 * <pre>
 *   static specNeeds = [EMBEDDED]
 *   static dirtyDomains = [User]
 * </pre>
 *
 * If the EMBEDDED server is needed, then it will be started once for the session.  This server will be shared with all
 * tests run during the session.<p>
 * The <code>dirtyDomains</code> value indicates that your test will be using GORM/Hibernate.  That sub-system will be started
 * for you and all records in the given domains will be deleted on cleanup.
 * <p>
 * <b>Note:</b> This base class extends GebSpec to make running GUI and non-GUI tests together easier.
 * <p>
 * This class will reset any mocked versions of the common utility classes (e.g. {@link DomainUtils} and {@link ControllerUtils}).
 * This means you do not need to reset these .instance values during your cleanup.  This BaseSpecification does that for you.
 * See the list {@link BaseSpecification#utilityClassesToCheck} below.
 *
 * <h3>Standard Cleanups</h3>
 * This base class resets the following elements after each test:
 * <ul>
 *   <li><b>GlobalUtils.defaultLocale</b> - Reset to the JVM's default locale. </li>
 * </ul>
 *
 *
 * <h3>Logging</h3>
 * The logging for this class that can be enabled:
 * <ul>
 *   <li><b>debug</b> - Prints all beans found on startup. </li>
 * </ul>
 */
@Slf4j
class BaseSpecification extends GebSpec {

  /**
   * Declares that the spec needs the EmbeddedServer started.  
   * Possible value for the <code>specNeeds</code> list.        Use BaseAPISpecification for most embedded server tests.
   */
  static final String EMBEDDED = "EMBEDDED"

  /**
   * Declares that the spec will be testing GUI features.  This forces EMBEDDED.
   * Possible value for the <code>specNeeds</code> list.
   */
  static final String GUI = "GUI"

  /**
   * Declares that the spec needs GORM/Hibernate data store started.  If the <code>dirtyDomains</code>
   * is set, then you don't need to add this to the normal <code>specNeeds</code> list.
   * Possible value for the <code>specNeeds</code> list.
   */
  static final String HIBERNATE = "HIBERNATE"

  /**
   * Declares that the spec needs the Jackson object mapper in the application context.
   * If not running in an embedded server, then this will mock the application context with a real ObjectMapper.
   * Possible value for the <code>specNeeds</code> list.
   */
  static final String JSON = "JSON"

  /**
   * Declares that the spec will need a mocked extension handler.
   * Possible value for the <code>specNeeds</code> list.
   * <p>
   * This option mocks a dummy ExtensibleFieldHelper with empty FieldExtension and FieldGUIExtension
   * mocks.
   * Use {@link #mockFieldExtension} to mock an extension for a specific domain.
   */
  static final String EXTENSION_MOCK = "EXTENSION_MOCK"

  /**
   * The list of cleanups needed by mocked elements.
   *
   */
  List<AutoCleanupMockInterface> autoCleanupsNeeded = []

  /**
   * A list of other domain classes that need cleanup.
   */
  List<Class> otherDirtyDomains = []

  static EmbeddedServer embeddedServer
  static hibernateDatastore
  static boolean threadFinished
  static boolean headless = false
  static boolean slowHibernateCheckCompleted = false
  /**
   * The number of ms to delay the startup of the hibernate or server.  Used for testing.
   */
  static long hibernateStartDelay = 0
  static long serverStartDelay = 0

  /**
   * Used to track if a new time is needed for the tests.txt output.
   */
  static boolean wroteTime = false

  /**
   * The mock extension (if in specNeeds).
   */
  static MockFieldExtension _mockFieldExtension = null

  /**
   * The setup method.  This method writes the test in IDEA runner format to allow us to re-produce the
   * order of execution.  Writes each test suite to 'tmp/tests.txt'.
   */
  @SuppressWarnings("Println")
  def setupSpec() {
    // Uses -D writeTests to write the tests as they are executed to a text file for sequence testing.
    if (Boolean.valueOf(System.getProperty('writeTests'))) {
      if (!wroteTime) {
        new File('tmp/tests.txt') << "\nTests ${new Date()}\n"
        wroteTime = true
      }
      new File('tmp/tests.txt') << "${this.class.name}||"
    }
  }

  /**
   * Sets up the Spock test.<p>
   * <b>Note:</b> This method indirectly sets the JVM's default timezone to UTC for proper date handling in Hibernate.
   *
   */
  @SuppressWarnings(["Println", "GroovyAssignabilityCheck", "SystemOutPrint"])
  def setup() {
    // This method has some ugly logic to avoid a race condition between the hibernate startup and the embedded server
    // startup.  Without it, the initial data load might not happen under all cases.
    // This usually only applies to when test Spec is run by itself.
    StartupHandler.preStart()
    if (needsHibernate() && hibernateDatastore == null) {
      // Start hibernate in a separate thread.
      threadFinished = false
      def t = new Thread(
        {
          if (!hibernateDatastore) {
            try {
              def start = System.currentTimeMillis()
              if (hibernateStartDelay) {
                log.info('setup() delay hibernate startup by {}ms', hibernateStartDelay)
                sleep(hibernateStartDelay)
              }

              // Build the std mappings for the encoded type and date only type from the additions
              // (internal and others found in the bootstrap).
              def typeMap = [:]
              for (addition in AdditionHelper.instance.additions) {
                for (clazz in addition.encodedTypes) {
                  typeMap[clazz] = EncodedType
                }
              }
              def mappings = {
                typeMap.each { k, v ->
                  mapping.userTypes[k] = v
                }
                // Add the standard DateOnly
                mapping.userTypes[DateOnly] = DateOnlyType
              }

              Map configuration = [
                'hibernate.cache.use_second_level_cache': false,
                'hibernate.hbm2ddl.auto'                : 'update',
                'hibernate.session_factory'             : EFrameHibernatePersistenceInterceptor,
                'grails.gorm.failOnError'               : true,
                'grails.gorm.failOnErrorPackages'       : ['org.simplemes', 'sample'],
                'grails.gorm.default.mapping'           : mappings,
              ]
              def classes = DomainFinder.instance.topLevelDomainClasses
              def packageList = classes*.package
              hibernateDatastore = new EFrameHibernateDatastore(configuration, packageList as Package[])
              Holders.hibernateDatastore = hibernateDatastore
              def end = System.currentTimeMillis()
              def s = hibernateStartDelay ? "- delayed ${hibernateStartDelay} for testing" : ""
              System.out.println("  Hibernate Startup ${end - start} ms $s")
              log.info("Hibernate startup {}ms", end - start)
              if (!embeddedServer) {
                // If hibernate beat the embedded server, then disable the work-around.
                slowHibernateCheckCompleted = true
              }
            } catch (Exception e) {
              log.error('Hibernate Startup {}', e)
            }
            threadFinished = true
          }
        } as Runnable)
      t.start()
    }

    //def x = new DefaultEnvironment('test')
    //println "x = $x"

    if (needsServer() && embeddedServer == null) {
      if (hibernateDatastore) {
        // Handle the case when hibernate was already running before this test (e.g. all test case).
        slowHibernateCheckCompleted = true
      }
      //println "starting embedded"
      def start = System.currentTimeMillis()
      if (serverStartDelay) {
        log.info('setup() delay server startup by {}ms', serverStartDelay)
        sleep(serverStartDelay)
      }
      embeddedServer = ApplicationContext.run(EmbeddedServer)


      def end = System.currentTimeMillis()
      if (log.debugEnabled) {
        log.debug('All Beans: {}', Holders.applicationContext.allBeanDefinitions*.name)
      }

      def s = serverStartDelay ? "- delayed ${serverStartDelay} for testing" : ""

      // Wait for the initial data loader to finish (up to 1 second).  Will check the count of rows in the User table.
      // The committed data is not visible to other processes until about 100ms after the commit.
      def waitStartTime = System.currentTimeMillis()
      boolean done = false
      while (!done) {
        User.withTransaction {
          done = User.count() > 0
        }
        // Make sure the wait does not go on too long.
        def waitTime = (System.currentTimeMillis() - waitStartTime)
        if (waitTime > 5000) {
          System.out.println("  Waited too long for initial data load ($waitTime) ms.")
          done = true
        }
        sleep(100)
      }
      System.out.println("  Server Startup ${end - start} ms $s")
      log.info("startup {}ms", end - start)
    }

    if (needsHibernate()) {
      // Wait for hibernate to finish, but only if needed.
      //sleep(100) // Disabled since this was executed for every hibernate test case.
      while (!threadFinished) {
        sleep(100)
      }
      // If the server finished first, then re-run the initial data load.
      if (hibernateDatastore && embeddedServer && needsServer() && !slowHibernateCheckCompleted) {
        slowHibernateCheckCompleted = true
        def start = System.currentTimeMillis()
        def loader = Holders.applicationContext.getBean(InitialDataLoader)
        loader.dataLoad()
        def end = System.currentTimeMillis()
        System.out.println("Loaded Initial Data for slow Hibernate case ${end - start}ms")
      }
    }

    // Finally, set the environment to test it not set already
    if (!needsServer()) {
      // Store a dummy environment for non-server tests.
      Holders.fallbackEnvironment = Holders.fallbackEnvironment ?: new DefaultEnvironment('test')
    }

    // See if a mock Jackson ObjectMapper is needed and not already in an embedded server.
    if ((!needsServer()) && needs(JSON) && embeddedServer == null) {
      def objectMapper = new ObjectMapper()
      objectMapper.registerModule(new HibernateAwareJacksonModule())
      StartupHandler.configureJacksonObjectMapper(objectMapper)
      new MockBean(this, ObjectMapper, objectMapper).install()  // Auto cleaned up
    }

    // See if a mock extensible field helper is needed.
    if (needs(EXTENSION_MOCK)) {
      _mockFieldExtension = new MockFieldExtension(this).install()
    }
  }

  /**
   * Restores any globals that were mocked.
   */
  void cleanup() {
    doAutoCleanups()
    cleanupDomainRecords()
    checkForLeftoverRecords()
    cleanupMockedUtilityClasses()
    MockAppender.cleanup()
    doOtherCleanups()
  }

  /**
   * Checks for any left-over records that may have been created by this test.
   * Checks all domains.
   */
  void checkForLeftoverRecords() {
    if (!hibernateDatastore) {
      // No db, so nothing to check
      return
    }
    def start = System.currentTimeMillis()
    def allDomains = DomainUtils.instance.allDomains
    for (clazz in allDomains) {
      User.withTransaction {
        def allowed = InitialDataRecords.instance.records[clazz.simpleName]
        def list = clazz.list()
        list = list.findAll() { !allowed?.contains(TypeUtils.toShortString(it)) }
        if (list) {
          def s = "${list.size()} records leftover in domain ${clazz.simpleName} for test ${this.class.simpleName}.  List: $list"
          throw new IllegalStateException(s)
        }
      }
    }
    def elapsed = System.currentTimeMillis() - start
    log.info("Elapsed time for left over record check: {} ms", elapsed)
  }


  /**
   * The common utility classes to check for a mocked instance.  Will reset them to their default (non-mocked) instance.
   * This typically includes {@link DomainUtils} and {@link ControllerUtils}.
   */
  def utilityClassesToCheck = [DomainUtils, ControllerUtils]

  /**
   * Cleans up any mocked .instance values in the common utility classes.
   */
  def cleanupMockedUtilityClasses() {
    for (clazz in utilityClassesToCheck) {
      def s = clazz.instance.toString()
      if (s.contains('Mock')) {
        //throw new IllegalStateException("Mock instance left in $clazz.simpleName for test ${this.class.simpleName}.  Class = $s")
        clazz.instance = clazz.newInstance()
      }
    }
  }

  /**
   * Do other, misc cleanups.
   */
  def doOtherCleanups() {
    // Reset the default locale
    GlobalUtils.defaultLocale = Locale.default

    // Reset the current UI timezone
    Holders.globals.timeZone = TimeZone.getTimeZone("America/New_York")

    // Clear the user override for later tests.
    SecurityUtils.currentUserOverride = null
  }

  /**
   * This method is called when the report fails.   It is sent by global Spock extension BaseFailureListener.
   */
  void reportFailure(String title = null) {
    if (needs(GUI)) {
      report(title ?: 'failure')
    }
  }

  /**
   * A running counter to make sure we don't have screen shot name collisions.
   */
  @Shared
  int reportScreenShotCount = 1

  /**
   * The test name.
   */
  @Rule
  TestName gebReportingSpecTestName

  /**
   * Writes the screen shot to the reports directory.
   * @param label The label to add to the end of the file names generated.
   */
  void report(String label) {
    def s = createReportLabel(label)
    log.debug('Logging failure screen shot to {}', s)
    browser.report(s)
    reportScreenShotCount++
  }

  /**
   * Builds a unique file name.
   * @param label The core label.
   * @return The unique file name.
   */
  String createReportLabel(String label = "") {
    def methodName = gebReportingSpecTestName?.methodName ?: 'fixture'
    def numberFormat = "%03d"
    return "${String.format(numberFormat, reportScreenShotCount)}-$methodName-$label"
  }

  /**
   * Determines if this test class needs a given feature.  Checks the 'specNeeds' static value.
   * @param need The specific need (e.g. BaseSpecification.EMBEDDED).
   * @return True if needed.
   */
  boolean needs(String need) {
    if (this.hasProperty('specNeeds')) {
      return this.specNeeds.contains(need)
    }
    return false
  }

  /**
   * Determines if this test class needs GORM/Hibernate.  Checks the <code>dirtyDomains</code> and
   * <code>specNeeds</code> static values.
   * @return True if needed.
   */
  boolean needsHibernate() {
    if (needsServer()) {
      // Make sure the server has hibernate for login and other DB needs.
      // This won't slow startup by much.
      return true
    }
    if (this.hasProperty('specNeeds')) {
      if (this.specNeeds.contains(HIBERNATE)) {
        return true
      }
    }
    return this.hasProperty('dirtyDomains')
  }

  /**
   * Determines if this test class needs the servers started.
   * This is true if the <code>specNeeds</code> static values contain EMBEDDED or GUI.
   * @return True if needed.
   */
  boolean needsServer() {
    if (this.hasProperty('specNeeds')) {
      return (this.specNeeds.contains(EMBEDDED) || this.specNeeds.contains(GUI))
    }
    return false
  }

  /**
   * Mocks a request with the given parameters.  This also stores the mock request in the currentRequest for indirect
   * access.<p>
   * <h3>Support params</h3>
   * The logging for this class that can be enabled:
   * <ul>
   *   <li><b>body</b> - The request body text (string). </li>
   *   <li><b>uri</b> - The URI of this request (string). </li>
   *   <li><b>accept</b> - The Accept header for this request. </li>
   * </ul>
   *
   * @param params The parameters (optional).  Special parameters ('uri') are stored in the request itself in the special fields.
   *               Also supports the body (string)
   * @return The request.
   */
  HttpRequest mockRequest(Map params = null) {
    def request = Mock(HttpRequest)
    def parameters = new SimpleHttpParameters(ConversionService.SHARED)
    // See if a the body is provided.
    def body = params?.body
    if (body) {
      //noinspection GroovyAssignabilityCheck
      request.getBody(*_) >> Optional.of(body)
      params.remove('body')
    }

    params?.each { k, v -> parameters.add(k as String, v as String) }
    //def parameters = Mock(HttpParameters)
    //parameters.get(_) >> null
    request.parameters >> parameters

    def uri = params?.uri
    if (uri) {
      request.getUri() >> new URI((String) uri)
    }

    def accept = params?.accept
    if (accept) {
      request.getHeaders() >> new SimpleHttpHeaders([Accept: accept], null)
    }

    Holders.mockRequest = request

    registerAutoCleanup({ testSpec -> Holders.mockRequest = null })

    return request
  }

  /**
   * Loads the initial data for the given domain class(s).  Also marks these domains as dirty.
   * @param classes The class(s).
   */
  void loadInitialData(Class... classes) {
    for (clazz in classes) {
      clazz.withTransaction {
        clazz.initialDataLoad()
        otherDirtyDomains << clazz
      }
    }
  }

  /**
   * Cleans up any records before and after each test, based on the values in the <code>dirtyDomains</code>
   * array. Always cleans up UserPreference records.
   * @param tester The tester class <b>Required</b>.  Used to access the GEB features.
   */
  void cleanupDomainRecords() {
    if (this.hasProperty('dirtyDomains')) {
      for (domainClass in this.dirtyDomains) {
        deleteAllRecords((Class) domainClass)
      }
    }
    for (domainClass in otherDirtyDomains) {
      deleteAllRecords(domainClass)
    }
    if (needs(HIBERNATE) || needs(GUI)) {
      deleteAllRecords(UserPreference)
    }
  }

  /**
   * Utility method to clean up all records in a given domain.
   * @param domainClass The domain to clean up.
   * @param ignoreInitialRecords If true, then the allowed initial data load records will nto be deleted.
   */
  @SuppressWarnings("GroovyAssignabilityCheck")
  void deleteAllRecords(Class domainClass, Boolean ignoreInitialRecords = true) {
    domainClass.withTransaction {
      def list = domainClass.list()
      // Filter out any allowed records (usually from initial data load)
      if (ignoreInitialRecords) {
        def allowed = InitialDataRecords.instance.records[domainClass.simpleName]
        list = list.findAll() { !(allowed?.contains(TypeUtils.toShortString(it))) }
      }
      if (list) {
        log.debug("Deleting all({}) {} records", list.size(), domainClass.simpleName)
      }
      for (record in (list)) {
        log.trace("  Deleting record '{}'", record)
        //println "deleting record = $record"
        record.delete(flush: true)
      }
    }
  }


  /**
   * Registers an auto cleanup task with this test.
   * @param acm The auto cleanup mock.
   */
  void registerAutoCleanup(AutoCleanupMockInterface acm) {
    autoCleanupsNeeded << acm
  }

  /**
   * Cleanup up any registered mocks that need cleanup tasks.
   */
  void doAutoCleanups() {
    if (autoCleanupsNeeded) {
      for (acm in autoCleanupsNeeded) {
        try {
          //println "acm = $acm"
          log.trace('Performing Auto Cleanup {}', acm)
          acm.doCleanup(this)
        } catch (Exception e) {
          LogUtils.logStackTrace(log, e, acm)
        }
      }
      autoCleanupsNeeded.clear()
    }
  }

  /**
   * Convenience method for general message.properties lookup.
   * Delegates to {@link GlobalUtils#lookup(java.lang.String, java.lang.Object [ ])}.
   * @param key The key to lookup.  If it starts with '*', then the return value will start with a '*'.
   *            The lookup will take place without the '*'.  This is used to support required field labels.
   * @param locale The locale to use for the message. (<b>Default</b>: Request Locale)
   * @param args The replaceable arguments used by the message (if any).
   * @return The looked up message.
   */
  String lookup(String key, Locale locale = null, Object... args) {
    def prefix = ''
    if (key.startsWith('*')) {
      key = key - '*'
      prefix = '*'
    }
    return prefix + GlobalUtils.lookup(key, locale, args)
  }

  /**
   * Convenience method for general message.properties lookup for fields marked as required.
   * Delegates to {@link GlobalUtils#lookup(java.lang.String, java.lang.Object [ ])}.
   * @param key The key to lookup.
   * @param locale The locale to use for the message. (<b>Default</b>: Request Locale)
   * @param args The replaceable arguments used by the message (if any).
   * @return The looked up message.
   */
  String lookupRequired(String key, Locale locale = null, Object... args) {
    return '*' + GlobalUtils.lookup(key, locale, args)
  }

  /**
   * The missing property handler.   This avoids the GEB propertyMissing handler when possible.
   * @param name The missing property.
   * @return
   */
  def propertyMissing(String name) {
    if (needs(GUI)) {
      // Let GEB handle it.
      return super.propertyMissing(name)
    }
    throw new MissingPropertyException("Unable to resolve '$name' as variable for ${this.class.name}. If this is a GUI Test, did you forget to add 'static specNeeds = [GUI]'?")
  }

  /**
   * Temporarily sets the log level to trace for the given class.
   * Will be reset to null on test finish.
   * <p>
   * This is used for quick trace settings while developing a test.
   * It <b>should not</b> be left in the test code for check-in.
   * @param clazz The class to set the log level to.
   */
  void setTraceLogLevel(Class clazz) {
    LogUtils.getLogger(clazz).level = Level.TRACE
    def original = LogUtils.getLogger(clazz).level
    registerAutoCleanup({ testSpec -> LogUtils.getLogger(clazz).level = original })
  }


  /**
   * Temporarily sets the current user for this test.
   * Will be reset to null on test finish.
   * @param userName The user to set the current user to.  <b>Default: </b>'TEST'.
   */
  void setCurrentUser(String userName = SecurityUtils.TEST_USER) {
    SecurityUtils.currentUserOverride = userName
    registerAutoCleanup({ testSpec -> SecurityUtils.currentUserOverride = null })
  }

  /**
   * For the current test, disable the stack trace logging.
   */
  void disableStackTraceLogging() {
    LogUtils.disableStackTraceLogging = true
    registerAutoCleanup({ testSpec -> LogUtils.disableStackTraceLogging = false })

  }

  /**
   * Mocks a specific field extension.  Needs the {@link #EXTENSION_MOCK} in the specNeeds property.
   * @param options The options for the field to add.  See {@link MockFieldExtension} for details.
   */
  void mockFieldExtension(Map options) {
    ArgumentUtils.checkMissing(_mockFieldExtension, 'specNeeds must have EXTENSION_MOCK specified')
    if (_mockFieldExtension.options == null) {
      _mockFieldExtension.options = []
    }

    _mockFieldExtension.options << options
  }

  /**
   * Convenience method to build a custom field for the given domain class.
   * @param options Contains: domainClass, fieldName, fieldFormat,valueClassName , afterFieldName or a 'list' of these elements.
   * @return The first FieldExtension created.
   */
  FieldExtension buildCustomField(Map options) {
    return buildCustomField([options])
  }

  /**
   * Convenience method to build a custom field for the given domain class.
   * @param options Contains: domainClass, fieldName, fieldFormat,valueClassName, afterFieldName or a 'list' of these elements.
   * @return The first FieldExtension created.
   */
  FieldExtension buildCustomField(List<Map> list) {
    def res = null
    FieldExtension.withTransaction {
      def domainList = new HashSet()
      for (field in list) {
        def fieldName = field.fieldName ?: 'custom1'
        def fieldFormat = field.fieldFormat ?: StringFieldFormat.instance
        def fe = new FieldExtension(fieldName: fieldName, domainClassName: field.domainClass.name,
                                    fieldFormat: (BasicFieldFormat) fieldFormat,
                                    valueClassName: field.valueClassName).save()
        domainList << field.domainClass
        res = res ?: fe
      }
      // Now, build the field GUI record, one for each domain in the input.
      def adj = []
      for (domainClass in domainList) {
        def fieldList = list.findAll { it.domainClass == domainClass }
        def fg = new FieldGUIExtension(domainName: domainClass.name)

        for (field in fieldList) {
          adj << new FieldInsertAdjustment(fieldName: field.fieldName,
                                           afterFieldName: field.afterFieldName ?: 'title')
        }
        fg.adjustments = adj
        fg.save()
      }
    }
    return res
  }


}
