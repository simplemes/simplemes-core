/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

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
import io.micronaut.transaction.jdbc.DataSourceUtils
import org.junit.Rule
import org.junit.rules.TestName
import org.simplemes.eframe.application.Holders
import org.simplemes.eframe.application.StartupHandler
import org.simplemes.eframe.controller.ControllerUtils
import org.simplemes.eframe.custom.domain.FieldExtension
import org.simplemes.eframe.custom.domain.FieldGUIExtension
import org.simplemes.eframe.custom.gui.FieldInsertAdjustment
import org.simplemes.eframe.data.format.BasicFieldFormat
import org.simplemes.eframe.data.format.StringFieldFormat
import org.simplemes.eframe.domain.DomainUtils
import org.simplemes.eframe.domain.annotation.DomainEntityHelper
import org.simplemes.eframe.i18n.GlobalUtils
import org.simplemes.eframe.json.EFrameJacksonModule
import org.simplemes.eframe.misc.ArgumentUtils
import org.simplemes.eframe.misc.LogUtils
import org.simplemes.eframe.misc.TypeUtils
import org.simplemes.eframe.preference.domain.UserPreference
import org.simplemes.eframe.security.SecurityUtils
import spock.lang.Shared

import javax.sql.DataSource
import javax.transaction.Transactional
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.SQLException

/**
 * The base class for most non-GUI tests.  This supports starting an embedded server and cleanup of the database tables.
 * These needs are flagged in your test classes with several static variables:
 * <pre>
 *   static specNeeds = [SERVER]
 *   static dirtyDomains = [User]
 * </pre>
 *
 * If the SERVER server is needed, then it will be started once for the session.  This server will be shared with all
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
  // TODO: Delete this constant and EXTENSION_MOCK
  static final String EMBEDDED = "EMBEDDED"

  /**
   * Declares that the spec needs the EmbeddedServer started.
   * Possible value for the <code>specNeeds</code> list.        Use BaseAPISpecification for most embedded server tests.
   */
  static final String SERVER = "EMBEDDED"

  /**
   * Declares that the spec will be testing GUI features.  This forces EMBEDDED.
   * Possible value for the <code>specNeeds</code> list.
   */
  static final String GUI = "GUI"

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
  @SuppressWarnings(["SystemOutPrint"])
  def setupSpec() {
    StartupHandler.preStart()
    // Uses -DwriteTests=true to write the tests as they are executed to a text file for sequence testing.
    // Supports DwriteTests=echo To write the test name to the output.
    def writeTests = System.getProperty('writeTests')
    if (writeTests) {
      if (writeTests.equalsIgnoreCase('true')) {
        if (!wroteTime) {
          new File('tmp/tests.txt') << "\nTests ${new Date()}\n"
          wroteTime = true
        }
        new File('tmp/tests.txt') << "${this.class.name}||"
      } else if (writeTests.equalsIgnoreCase('echo')) {
        System.out.println("${this.class.name}")
      }
    }
  }

  /**
   * Sets up the Spock test.<p>
   * <b>Note:</b> This method indirectly sets the JVM's default timezone to UTC for proper date handling in Hibernate.
   *
   */
  @SuppressWarnings(["Println", "GroovyAssignabilityCheck", "SystemOutPrint"])
  def setup() {
    startServerIfNeeded()


    //def x = new DefaultEnvironment('test')
    //println "x = $x"

    if (needsServer() && embeddedServer == null) {


      //def end = System.currentTimeMillis()
      if (log.debugEnabled) {
        log.debug('All Beans: {}', Holders.applicationContext.allBeanDefinitions*.name)
      }

      // See if a mock Jackson ObjectMapper is needed and not already in an embedded server.
      if ((!needsServer()) && needs(JSON) && embeddedServer == null) {
        def objectMapper = new ObjectMapper()
        objectMapper.registerModule(new EFrameJacksonModule())
        StartupHandler.configureJacksonObjectMapper(objectMapper)
        new MockBean(this, ObjectMapper, objectMapper).install()  // Auto cleaned up
      }

      // See if a mock extensible field helper is needed.
      if (needs(EXTENSION_MOCK)) {
        _mockFieldExtension = new MockFieldExtension(this).install()
      }
    } else {
      // No server needed.
      // Finally, set the environment to test it not set already
      if (!needsServer()) {
        // Store a dummy environment for non-server tests.
        Holders.fallbackEnvironment = Holders.fallbackEnvironment ?: new DefaultEnvironment('test')
      }

    }
  }

  /**
   * Starts an embedded server, if needed.
   */
  def startServerIfNeeded() {
    if (needsServer() && embeddedServer == null) {
      def start = System.currentTimeMillis()
      embeddedServer = ApplicationContext.run(EmbeddedServer)
      System.out.println("Started Server ${System.currentTimeMillis() - start}ms")
      executeTestDDLStatements()
/*    // Probably not needed in most cases.
      // Wait for the initial user record to be committed
      def adminUser = null
      start = System.currentTimeMillis()
      while(!adminUser) {
        adminUser = User.findByUserName('admin')
        //println "adminUser = $adminUser"
        if (!adminUser) {
          sleep(50)
        }
      }
      System.out.println("  Waited ${System.currentTimeMillis() - start}ms for IDAT to finish")
      //println "admin = ${User.list()*.userName} $adminUser "
*/

      log.debug("All Beans = ${Holders.applicationContext.getAllBeanDefinitions()*.name}")
    }
  }

  /**
   * Executes any DDL Statements needed for a test database (H2).
   * Checks all loaded modules (via classpath) for 'ddl/_testH2.sql' files.
   */
  void executeTestDDLStatements() {
    def resources = getClass().classLoader.getResources('ddl/_testH2.sql')
    for (url in resources) {
      def inputStream = null
      try {
        log.debug("executeTestDDLStatements(): Executing statements from {} ", url)
        inputStream = url.openStream()
        inputStream.eachLine { line ->
          if (line.contains(';')) {
            line = line - ';'
            log.debug("executeTestDDLStatements(): Executing {} ", line)
            DataSource dataSource = Holders.getApplicationContext().getBean(DataSource.class)
            Connection connection = DataSourceUtils.getConnection(dataSource)
            def ps = connection.prepareStatement(line)
            ps.execute()
          }
          false
        }
      } finally {
        inputStream?.close()
      }
    }

  }

  /**
   * Restores any globals that were mocked.
   */
  void cleanup() {
    doAutoCleanups()
    FieldExtension.withTransaction {
      cleanupDomainRecords()
    }
    FieldExtension.withTransaction {
      checkForLeftoverRecords()
    }
    cleanupMockedUtilityClasses()
    MockAppender.cleanup()
    doOtherCleanups()
  }

  /**
   * Checks for any left-over records that may have been created by this test.
   * Checks all domains.
   */
  @Transactional
  void checkForLeftoverRecords() {
    if (!embeddedServer) {
      // No db, so nothing to check
      return
    }
    def start = System.currentTimeMillis()
    def allDomains = DomainUtils.instance.allDomains
    for (clazz in allDomains) {
      def allowed = InitialDataRecords.instance.records[clazz.simpleName]
      def list = clazz.list()
      list = list.findAll() { !allowed?.contains(TypeUtils.toShortString(it)) }
      if (list) {
        def s = "${list.size()} records leftover in domain ${clazz.simpleName} for test ${this.class.simpleName}.  List: $list"
        log.error(s)
        throw new IllegalStateException(s)
      }
    }
    def elapsed = System.currentTimeMillis() - start
    log.info("Elapsed time for left over record check: {} ms", elapsed)
  }


  /**
   * The common utility classes to check for a mocked instance.  Will reset them to their default (non-mocked) instance.
   * This typically includes {@link DomainUtils} and {@link ControllerUtils}.
   */
  def utilityClassesToCheck = [DomainUtils, ControllerUtils, DomainEntityHelper]

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
   * Determines if this test class needs the servers started.
   * This is true if the <code>specNeeds</code> static values contain EMBEDDED or GUI.
   * @return True if needed.
   */
  boolean needsServer() {
    if (this.hasProperty('specNeeds')) {
      return (this.specNeeds.contains(EMBEDDED) || this.specNeeds.contains(SERVER) || this.specNeeds.contains(GUI))
    }

    // dirtyDomains implies SERVER is needed.
    return this.hasProperty('dirtyDomains')
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
  @Transactional
  void loadInitialData(Class... classes) {
    for (clazz in classes) {
      clazz.initialDataLoad()
      otherDirtyDomains << clazz
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
    if (embeddedServer) {
      deleteAllRecords(UserPreference)
      deleteAllRecords(FieldGUIExtension)
      deleteAllRecords(FieldExtension)
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
        record.delete()
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

  // TODO: Move to a Test Utils method?
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

  /**
   * Runs the validation on the given domain object and verifies that the given validation (code/fieldName)
   * are found.  Also checks the message for the presence of the expectedStrings.
   * @param object The domain object to validate.
   * @param code The expected error code.
   * @param fieldName The expected error field.
   * @param expectedStrings The expected strings the in the message.
   * @return True if passes.  Throws assertion exception if not.
   */
  boolean assertValidationFails(Object object, int code, String fieldName, List<String> expectedStrings) {
    //error.4.message=Invalid "{0}" class.  Class "{1}" not found.
    def errors = DomainUtils.instance.validate(object)
    assert errors.size() > 0, "Expected error on object $object"
    assert errors[0].fieldName == fieldName
    assert errors[0].code == code
    UnitTestUtils.assertContainsAllIgnoreCase(errors[0].toString(), expectedStrings)
    return true
  }

  /**
   * Enable SQL trace.  Used mainly in debugging of SQL without enabling it during startup.
   * @param includeParams If true, then parameters are logged too.  (<b>Default</b>: false).
   */
  void enableSQLTrace(boolean includeParams = false) {
    def logger = LogUtils.getLogger('io.micronaut.data')
    def oldLevel = LogUtils.getLogger('io.micronaut.data').level
    if (includeParams) {
      logger.level = Level.TRACE
    } else {
      logger.level = Level.DEBUG
    }
    // Automatically reset it at the end of the test.
    registerAutoCleanup({ testSpec -> logger.level = oldLevel })
  }


  /**
   * Disables the SQL trace level.  Resets to WARN.
   */
  void disableSQLTrace() {
    def logger = LogUtils.getLogger('io.micronaut.data')
    logger.level = Level.WARN
  }

  /**
   * Creates a prepared SQL statement.
   *
   * @param sql The SQL.
   * @return The statement.
   */
  protected PreparedStatement getPreparedStatement(String sql) throws SQLException {
    DataSource dataSource = Holders.applicationContext.getBean(DataSource.class)
    Connection connection = DataSourceUtils.getConnection(dataSource)
    return connection.prepareStatement(sql)
  }

}
