/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.test

import ch.qos.logback.classic.Level
import geb.spock.GebSpec
import groovy.util.logging.Slf4j
import io.micronaut.context.ApplicationContext
import io.micronaut.core.convert.ConversionService
import io.micronaut.http.HttpHeaders
import io.micronaut.http.HttpRequest
import io.micronaut.http.cookie.Cookies
import io.micronaut.http.netty.cookies.NettyCookie
import io.micronaut.http.simple.SimpleHttpHeaders
import io.micronaut.http.simple.SimpleHttpParameters
import io.micronaut.runtime.server.EmbeddedServer
import io.micronaut.transaction.jdbc.DataSourceUtils
import org.openqa.selenium.TimeoutException
import org.simplemes.eframe.application.Holders
import org.simplemes.eframe.application.StartupHandler
import org.simplemes.eframe.controller.ControllerUtils
import org.simplemes.eframe.custom.annotation.ExtensionPointHelper
import org.simplemes.eframe.custom.domain.FieldExtension
import org.simplemes.eframe.custom.domain.FieldGUIExtension
import org.simplemes.eframe.domain.DomainUtils
import org.simplemes.eframe.domain.annotation.DomainEntityHelper
import org.simplemes.eframe.i18n.GlobalUtils
import org.simplemes.eframe.misc.ArgumentUtils
import org.simplemes.eframe.misc.LogUtils
import org.simplemes.eframe.misc.TypeUtils
import org.simplemes.eframe.preference.domain.UserPreference
import org.simplemes.eframe.search.SearchHelper
import org.simplemes.eframe.security.SecurityUtils
import org.simplemes.eframe.security.domain.RefreshToken

import javax.sql.DataSource
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
 * The <code>dirtyDomains</code> value indicates that your test will be using a real database.  That sub-system will be started
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
  static final String SERVER = "EMBEDDED"

  /**
   * Declares that the spec will be testing GUI features.  This forces SERVER.
   * Possible value for the <code>specNeeds</code> list.
   */
  static final String GUI = "GUI"

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
  @SuppressWarnings(["SystemOutPrint", 'unused'])
  def setupSpec() {
    StartupHandler.preStart()
    // Uses -DwriteTests=true to write the tests as they are executed to a text file for sequence testing.
    // Supports -DwriteTests=echo To write the test name to the output.
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
   * <b>Note:</b> This method indirectly sets the JVM's default timezone to UTC for proper date handling in the database.
   *
   */
  @SuppressWarnings(["Println", "GroovyAssignabilityCheck", "SystemOutPrint"])
  def setup() {
    log.debug("setup(): Starting Specification: ", this)
    startServerIfNeeded()


    //def x = new DefaultEnvironment('test')
    //println "x = $x"

    if (needsServer() && embeddedServer == null) {


      //def end = System.currentTimeMillis()
      if (log.debugEnabled) {
        log.debug('All Beans: {}', Holders.applicationContext.allBeanDefinitions*.name)
      }

    } else {
      // No server needed.
      // Finally, set the environment to test it not set already
      if (!needsServer()) {
        // Store a dummy environment for non-server tests.
        if (!Holders.fallbackEnvironmentNames) {
          Holders.fallbackEnvironmentNames = ['test']
        }
      }

      // See if a mock extensible field helper is needed.
      if (needs(EXTENSION_MOCK)) {
        _mockFieldExtension = new MockFieldExtension(this).install()
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

      log.debug("All Beans = ${Holders.applicationContext.getAllBeanDefinitions()*.name}")
    }
  }

  /**
   * Restores any globals that were mocked.
   */
  void cleanup() {
    doAutoCleanups()
    // Make sure any cached applicationContext is re-read from the real Holder, if needed.
    DomainEntityHelper.instance.clearCaches()
    if (embeddedServer) {
      FieldExtension.withTransaction {
        cleanupDomainRecords()
      }
      FieldExtension.withTransaction {
        checkForLeftoverRecords()
      }
    }
    cleanupMockedUtilityClasses()
    MockAppender.cleanup()
    doOtherCleanups()
  }

  /**
   * Checks for any left-over records that may have been created by this test.
   * Checks all domains.
   */
  void checkForLeftoverRecords() {
    if (!embeddedServer) {
      // No db, so nothing to check
      return
    }
    waitForInitialDataLoad()  // Make sure the data is loaded before exiting.
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
  def utilityClassesToCheck = [DomainUtils, ControllerUtils, DomainEntityHelper, ExtensionPointHelper, SearchHelper]

  /**
   * Cleans up any mocked .instance values in the common utility classes.
   */
  def cleanupMockedUtilityClasses() {
    for (clazz in utilityClassesToCheck) {
      def s = clazz.instance.toString()
      if (s.contains('Mock')) {
        //throw new IllegalStateException("Mock instance left in $clazz.simpleName for test ${this.class.simpleName}.  Class = $s")
        clazz.instance = clazz.getConstructor().newInstance()
      }
    }
  }

  /**
   * Do other, misc cleanups.
   */
  def doOtherCleanups() {
    // Reset the default locale
    GlobalUtils.defaultLocale = Locale.default

    // Reset the current UI timezone and the simulated environment flag
    Holders.globals.timeZone = TimeZone.getTimeZone("America/New_York")
    Holders.simulateProductionEnvironment = false

    // Clear the user override for later tests.
    SecurityUtils.currentUserOverride = null

    // Make sure nothing is cached in the helper
    ExtensionPointHelper.clearCaches()
  }

  /**
   * This method is called when the report fails.   It is sent by global Spock extension BaseFailureListener.
   */
  void reportFailure(String methodName = null) {
    if (needs(GUI)) {
      report(methodName ?: 'unknown-method-name')
    }
  }

  /**
   * A running counter to make sure we don't have screen shot name collisions.
   */
  static int reportScreenShotCount = 1

  /**
   * Writes the screen shot to the reports directory.
   * @param methodName The method that failed.
   */
  void report(String methodName) {
    def s = createReportLabel(methodName)
    log.debug('Logging failure screen shot to {}', s)
    browser.report(s)
    reportScreenShotCount++
  }

  /**
   * Builds a unique file name.
   * @param methodName The core label.
   * @return The unique file name.
   */
  String createReportLabel(String methodName = "") {
    def className = this.class.simpleName
    def numberFormat = "%03d"
    return "$className-$methodName-${String.format(numberFormat, reportScreenShotCount)}"
  }

  /**
   * Determines if this test class needs a given feature.  Checks the 'specNeeds' static value.
   * @param need The specific need (e.g. BaseSpecification.SERVER).
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
   * This is true if the <code>specNeeds</code> static values contain SERVER or GUI.
   * @return True if needed.
   */
  boolean needsServer() {
    if (this.hasProperty('specNeeds')) {
      return (this.specNeeds.contains(SERVER) || this.specNeeds.contains(GUI))
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
   *   <li><b>remoteAddress</b> - The InetSocketAddress simulated for this mock request. </li>
   *   <li><b>cookies</b> - A list of cookies simulated for this mock request.  Array of strings in format: ['JWT=abc...','JWT_REFRESH=abc...'] </li>
   *   <li><b>headers</b> - A list of headers simulated for this mock request.  Map with key = header name. </li>
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

    def remoteAddress = params?.remoteAddress
    if (remoteAddress) {
      //noinspection GroovyAssignabilityCheck
      request.getRemoteAddress() >> remoteAddress
      params.remove('remoteAddress')
    }

    params?.each { k, v -> parameters.add(k as String, v as String) }
    //def parameters = Mock(HttpParameters)
    //parameters.get(_) >> null
    request.parameters >> parameters

    def uri = params?.uri
    if (uri) {
      request.getUri() >> new URI((String) uri)
      request.getPath() >> uri
    }

    def accept = params?.accept
    if (accept) {
      request.getHeaders() >> new SimpleHttpHeaders((Map) [Accept: accept], null)
    }

    if (params?.cookies) {
      def cookies = Mock(Cookies)
      for (String s in params.cookies) {
        def start = s.indexOf('=')
        if (start < 0) {
          throw new IllegalArgumentException("Mock cookie string must be in the form: NAME=value.  Missing '='.")
        }
        def value = s[(start + 1)..-1]
        def name = s[0..(start - 1)]
        cookies.get(name) >> new NettyCookie(name, value)
      }
      request.getCookies() >> cookies
    }

    if (params?.headers) {
      def headers = Mock(HttpHeaders)
      params.headers.each {String k,v ->
        headers.get(k) >> v
      }
      request.getHeaders() >> headers
    }

    Holders.mockRequest = request

    registerAutoCleanup({ testSpec -> Holders.mockRequest = null })

    return request
  }

  /**
   * Waits for the initial data load finishes.  Mainly used to wait for things like admin user or roles to be loaded.
   * Waits up to 5 seconds.  Will throw an exception if it never finishes.
   */
  void waitForInitialDataLoad() {
    if (InitialDataRecords.instance.loadFinished) {
      return
    }

    def start = System.currentTimeMillis()
    def elapsed = 0
    while (elapsed < 5000) {
      elapsed = System.currentTimeMillis() - start
      if (InitialDataRecords.instance.loadFinished) {
        log.debug("waitForInitialDataLoad(): Waited {}ms", elapsed)
        return
      }
    }

    throw new IllegalStateException("waitForInitialDataLoad(): Initial data load did not finish in ${elapsed}ms.  Is server started?")

  }

  /**
   * Cleans up any records before and after each test, based on the values in the <code>dirtyDomains</code>
   * array. Always cleans up UserPreference records.
   * @param tester The tester class <b>Required</b>.  Used to access the GEB features.
   */
  void cleanupDomainRecords() {
    def list = TypeUtils.getStaticPropertyInSuperClasses(this.getClass(), 'dirtyDomains')
    for (l in list) {
      for (domainClass in l) {
        deleteAllRecords((Class) domainClass)
      }
    }
    for (domainClass in otherDirtyDomains) {
      deleteAllRecords(domainClass)
    }
    if (embeddedServer) {
      deleteAllRecords(UserPreference)
      deleteAllRecords(RefreshToken)
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
   * <p>
   * <b>Note:</b> This lookup will flag missing .properties entries.
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
    return prefix + lookupFlagMissing(key, locale, args)
  }

  /**
   * Convenience method for general message.properties lookup for fields marked as required.
   * Delegates to {@link GlobalUtils#lookup(java.lang.String, java.lang.Object [ ])}.
   * <p>
   * <b>Note:</b> This lookup will flag missing .properties entries.
   * @param key The key to lookup.
   * @param locale The locale to use for the message. (<b>Default</b>: Request Locale)
   * @param args The replaceable arguments used by the message (if any).
   * @return The looked up message.
   */
  String lookupRequired(String key, Locale locale = null, Object... args) {
    return '*' + lookupFlagMissing(key, locale, args)
  }

  /**
   * Lookup with a check for missing .properties entries.  Will alter the result with a '-missing.in.properties' to
   * help find non-looked up text on GUIs and such.
   * @param key The key to lookup.
   * @param locale The locale to use for the message. (<b>Default</b>: Request Locale)
   * @param args The replaceable arguments used by the message (if any).
   * @return The looked up message.
   */
  private String lookupFlagMissing(String key, Locale locale = null, Object... args) {
    def s = GlobalUtils.lookup(key, locale, args)
    if (s == key) {
      s = s + '-missing.in.properties'
    }
    return s
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
  @SuppressWarnings('unused')
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

  /**
   * Wait for a record to change in the database.  Checks the version until is changes.  Waits up to 5 seconds.
   * @param record The record to wait for a change.
   */
  void waitForRecordChange(Object record) {
    def originalVersion = record.version
    def domainClass = record.getClass()
    def start = System.currentTimeMillis()
    def elapsed = System.currentTimeMillis() - start
    while (elapsed < 5000) {
      def record2 = domainClass.findByUuid(record.uuid)
      if (record2.version != originalVersion) {
        return
      }
      sleep(100)
      elapsed = System.currentTimeMillis() - start
    }
    throw new TimeoutException("${record.getClass().simpleName} record '${TypeUtils.toShortString(record)}' did not change within ${elapsed}ms.")
  }

  /**
   * Checks the HTML and Javascript for basic format errors.  If the page contains no 'script' tag, the whole page
   * will be tested as Javascript (not HTML).
   * @param page The page content to check.
   * @return True if Ok.  Exception if not.
   */
  boolean checkPage(String page) {
    HTMLTestUtils.checkHTML(page)

    if (page.contains('<')) {  // Some HTML (probably).
      if (page.contains('<script')) {
        return JavascriptTestUtils.checkScriptsOnPage(page)
      }
    } else {
      // Just Javascript (probably).
      return JavascriptTestUtils.checkScriptFragment(page)
    }
    log.warn("Script check disabled probably due to presence of < on page. Try checkJavascript() or checkJavascriptFragment().  Test = {}", this)
    return true
  }

  /**
   * Checks the page for JS errors.  Adds some JS to make the page fragment legal JS.
   * @param page The page.
   * @return true
   */
  boolean checkJavascript(String page) {
    return JavascriptTestUtils.checkScript(page)
  }

  /**
   * Checks the page for JS errors.  Adds some JS to make the page fragment legal JS.
   * @param page The page.
   * @return true
   */
  boolean checkJavascriptFragment(String page) {
    return JavascriptTestUtils.checkScript("var x = [ $page ];")
  }

}
