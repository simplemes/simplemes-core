package org.simplemes.eframe.web.ui.webix.freemarker

import ch.qos.logback.classic.Level
import org.simplemes.eframe.exception.BusinessException
import org.simplemes.eframe.misc.TextUtils
import org.simplemes.eframe.preference.PreferenceHolder
import org.simplemes.eframe.preference.SplitterPreference
import org.simplemes.eframe.security.SecurityUtils
import org.simplemes.eframe.test.BaseMarkerSpecification
import org.simplemes.eframe.test.DashboardUnitTestUtils
import org.simplemes.eframe.test.JavascriptTestUtils
import org.simplemes.eframe.test.MockAppender

/*
 * Copyright Michael Houston 2019. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Tests.
 */
class DashboardMarkerSpec extends BaseMarkerSpecification {

  @SuppressWarnings("unused")
  static specNeeds = [JSON, SERVER]


  //TODO: Find alternative to @Rollback
  def "verify that the tag can handle a two panel vertical dashboard"() {
    given: 'a dashboard config'
    DashboardUnitTestUtils.buildDashboardConfig('TEST', ['vertical0', '/page0', '/page1'])

    when: 'the HTML is generated'
    def page = execute(source: '<@efDashboard category="NONE"/>', uri: '/dashboard?arg=value')

    then: 'the HTML is valid'
    checkPage(page)

    and: 'the constructor works'
    new DashboardMarker().toString()

    and: 'the webix setup is correct'
    page.contains('<div id="TEST"></div>')
    def uiText = JavascriptTestUtils.extractBlock(page, 'webix.ui({')
    uiText.contains("container: 'TEST'")

    and: 'contains the two panels'
    def colText = JavascriptTestUtils.extractBlock(uiText, 'cols: [')
    colText.contains('id: "PanelA"')
    colText.contains('id: "PanelB"')

    and: 'the definePanels block defines the default pages correctly'
    def panelsText = JavascriptTestUtils.extractBlock(page, 'dashboard._definePanelsAndLoad({')
    panelsText.contains('defaultURL: "/page0"')
    panelsText.contains('defaultURL: "/page1"')

    and: 'the communication variables are generated correctly'
    page.contains('var _A={};')
    page.contains('var _B={};')

    and: 'the dialog preferences are loaded'
    page.contains('eframe.loadDialogPreferences();')

    and: 'the current dashboard/category are set'
    page.contains('dashboard.currentDashboard="TEST";')
    page.contains('dashboard.currentCategory="NONE";')
  }

  //TODO: Find alternative to @Rollback
  def "verify that the tag can handle a two panel horizontal dashboard"() {
    given: 'a dashboard config'
    DashboardUnitTestUtils.buildDashboardConfig('TEST', ['horizontal0', 'page0', 'page1'])

    when: 'the HTML is generated'
    def page = execute(source: '<@efDashboard category="NONE"/>', uri: '/dashboard?arg=value')

    then: 'the HTML is valid'
    checkPage(page)

    and: 'contains the two panels'
    def uiText = JavascriptTestUtils.extractBlock(page, 'webix.ui({')
    def rowText = JavascriptTestUtils.extractBlock(uiText, 'rows: [')
    rowText.contains('id: "PanelA"')
    rowText.contains('id: "PanelB"')
  }

  //TODO: Find alternative to @Rollback
  def "verify that the tag detects no configuration correctly - by category"() {
    when: 'the HTML is generated'
    def page = execute(source: '<@efDashboard category="TEST"/>', uri: '/dashboard?arg=value')

    then: 'the HTML is valid'
    checkPage(page)

    and: 'the error message is displayed'
    page.contains('<script>')
    page.contains('ef.displayMessage({error:')
    page.contains('</script>')
    def errorText = JavascriptTestUtils.extractBlock(page, 'ef.displayMessage({')
    errorText.contains(new BusinessException(112, ['TEST']).toString())
  }

  //TODO: Find alternative to @Rollback
  def "verify that the tag detects no configuration correctly - dashboard name"() {
    when: 'the HTML is generated'
    def page = execute(source: '<@efDashboard dashboard="TEST"/>', uri: '/dashboard?arg=value')

    then: 'the HTML is valid'
    checkPage(page)

    and: 'the error message is displayed'
    page.contains('<script>')
    page.contains('ef.displayMessage({error:')
    page.contains('</script>')
    def errorText = JavascriptTestUtils.extractBlock(page, 'ef.displayMessage({')
    errorText.contains(new BusinessException(121, ['TEST']).toString())
  }

  //TODO: Find alternative to @Rollback
  def "verify that the tag can handle a one dashboard panel"() {
    given: 'a dashboard config'
    DashboardUnitTestUtils.buildDashboardConfig('TEST', ['/page0'])

    when: 'the HTML is generated'
    def page = execute(source: '<@efDashboard category="NONE"/>', uri: '/dashboard?arg=value')

    then: 'the HTML is valid'
    checkPage(page)

    and: 'contains the panel'
    def uiText = JavascriptTestUtils.extractBlock(page, 'webix.ui({')
    def rowText = JavascriptTestUtils.extractBlock(uiText, 'rows: [')
    rowText.contains('id: "PanelA"')

    and: 'there is no splitter'
    !uiText.contains('resizer')

    and: 'the definePanels block defines the default page correctly'
    def panelsText = JavascriptTestUtils.extractBlock(page, 'dashboard._definePanelsAndLoad({')
    panelsText.contains('defaultURL: "/page0"')
  }

  //TODO: Find alternative to @Rollback
  def "verify that the tag can handle loading a specific dashboard"() {
    given: 'a non-default dashboard config'
    def dashboardConfig = DashboardUnitTestUtils.buildDashboardConfig('TEST', ['/page0'])
    dashboardConfig.defaultConfig = false
    dashboardConfig.save()

    when: 'the HTML is generated'
    def page = execute(source: '<@efDashboard dashboard="TEST" category="NONE"/>', uri: '/dashboard?arg=value')

    then: 'the HTML is valid'
    checkPage(page)

    and: 'the right dashboard is used'
    TextUtils.findLine(page, 'dashboard._definePanelsAndLoad(').contains('defaultURL: "/page0"')
    page.contains('dashboard.currentDashboard="TEST";')
  }

  //TODO: Find alternative to @Rollback
  def "verify that the tag can handle three panel vertical"() {
    given: 'a dashboard config'
    DashboardUnitTestUtils.buildDashboardConfig('TEST', ['vertical0', 'page0', 'horizontal1', 'page1', 'page2'])

    when: 'the HTML is generated'
    def page = execute(source: '<@efDashboard category="NONE"/>', uri: '/dashboard?arg=value')

    then: 'the HTML is valid'
    checkPage(page)

    and: 'the right panels are generated'
    page.contains('id: "PanelA"')
    page.contains('id: "PanelB"')
    page.contains('id: "PanelC"')

    and: 'the panels are inside of the correct splitters'
    // resizer0
    //  panelA
    //  resizer2
    //    panelB
    //    panelC
    page.indexOf('PanelA') < page.indexOf('resizer0')
    page.indexOf('PanelA') < page.indexOf('resizer2')
    page.indexOf('PanelB') < page.indexOf('resizer2')
    page.indexOf('PanelB') < page.indexOf('PanelC')
  }

  //TODO: Find alternative to @Rollback
  def "verify that the tag can use splitter sizes from the user preferences"() {
    given: 'a dashboard config'
    DashboardUnitTestUtils.buildDashboardConfig('DASHBOARD', ['vertical0', 'page0', 'horizontal1', 'page1', 'page2'])

    and: 'a test user'
    setCurrentUser()

    and: 'the splitter preference is set for the dashboard'
    PreferenceHolder preferenceHolder = PreferenceHolder.find {
      page '/dashboard'
      user SecurityUtils.TEST_USER
      element '_dDASHBOARD'
    }
    preferenceHolder.setPreference(new SplitterPreference(resizer: 'resizer0', size: 23.4))
    preferenceHolder.setPreference(new SplitterPreference(resizer: 'resizer2', size: 24.5))
    preferenceHolder.save()

    when: 'the HTML is generated'
    def page = execute(source: '<@efDashboard category="NONE"/>', uri: '/dashboard?arg=value')

    then: 'the HTML is valid'
    checkPage(page)

    and: 'right splitter size is used'
    def panelAText = TextUtils.findLine(page, 'id: "PanelA"')
    JavascriptTestUtils.extractProperty(panelAText, 'width') == 'tk.pw("23.4%")'

    def panelBText = TextUtils.findLine(page, 'id: "PanelB"')
    JavascriptTestUtils.extractProperty(panelBText, 'height') == 'tk.ph("24.5%")'
  }

  //TODO: Find alternative to @Rollback
  def "verify that the tag can handle all of the supported button fields"() {
    given: 'a dashboard config'
    def buttons = [[label: 'b10', url: '/page10', panel: 'A', title: 'title10', css: 'caution-button', size: 1.2, buttonID: 'B10']]
    DashboardUnitTestUtils.buildDashboardConfig('DASHBOARD', ['vertical0', 'page0', 'page1'], buttons)

    when: 'the HTML is generated'
    def page = execute(source: '<@efDashboard category="NONE"/>', uri: '/dashboard?arg=value')

    then: 'the HTML is valid'
    checkPage(page)

    and: 'the defineButtons method is called correctly'
    def buttonsText = JavascriptTestUtils.extractBlock(page, 'dashboard._defineButtons([')
    def buttonText = TextUtils.findLine(buttonsText, 'id: "B10"')
    JavascriptTestUtils.extractProperty(buttonText, 'label') == 'b10'
    JavascriptTestUtils.extractProperty(buttonText, 'title') == 'title10'
    JavascriptTestUtils.extractProperty(buttonText, 'size').contains('1.2')
    JavascriptTestUtils.extractProperty(buttonText, 'css') == 'caution-button'
    JavascriptTestUtils.extractProperty(buttonText, 'url') == '/page10'
    buttonText.contains('panel: "A"')
  }

  //TODO: Find alternative to @Rollback
  def "verify that the tag can handle multiple buttons"() {
    given: 'a dashboard config'
    def button1a = [label: 'b10', url: '/page11', panel: 'A', buttonID: 'ID10']
    def button1b = [label: 'b10', url: '/page12', panel: 'B', buttonID: 'ID102']
    def button1c = [label: 'b10', url: '/page13', panel: 'A', buttonID: 'ID103']
    def button2 = [label: 'b20', url: '/page14', panel: 'B', buttonID: 'ID20']
    def buttons = [button1a, button1b, button1c, button2]
    DashboardUnitTestUtils.buildDashboardConfig('DASHBOARD', ['vertical0', 'page0', 'page1'], buttons)

    when: 'the HTML is generated'
    def page = execute(source: '<@efDashboard category="NONE"/>', uri: '/dashboard?arg=value')

    then: 'the HTML is valid'
    checkPage(page)

    and: 'the first button has multiple pages on it correctly'
    def buttonsText = JavascriptTestUtils.extractBlock(page, 'dashboard._defineButtons([')
    def button1Text = TextUtils.findLine(buttonsText, 'label: "b10"')
    button1Text.contains('id: "ID10"')
    button1Text.contains('panel: "A"')
    button1Text.contains('url: "/page11"')
    button1Text.contains('panel: "B"')
    button1Text.contains('url: "/page12"')
    button1Text.contains('panel: "A"')
    button1Text.contains('url: "/page13"')

    and: 'the single page button is correct too'
    def button2Text = TextUtils.findLine(buttonsText, 'label: "b20"')
    button2Text.contains('panel: "B"')
    button2Text.contains('url: "/page14"')
    button2Text.contains('id: "ID20"')
  }

  //TODO: Find alternative to @Rollback
  def "verify that the tag can handle supported URLs formats"() {
    given: 'a dashboard config'
    DashboardUnitTestUtils.buildDashboardConfig('TEST', ['vertical0', '/page0', 'vertical1', 'Page1',
                                                         'vertical2', 'http://server:80/page2', 'https://server:81/page3'])

    when: 'the HTML is generated'
    def page = execute(source: '<@efDashboard category="NONE"/>', uri: '/dashboard?arg=value')

    then: 'the HTML is valid'
    checkPage(page)

    and: 'the definePanels block defines the default pages correctly'
    def panelsText = JavascriptTestUtils.extractBlock(page, 'dashboard._definePanelsAndLoad({')
    TextUtils.findLine(panelsText, '"A":').contains('defaultURL: "/page0"')
    TextUtils.findLine(panelsText, '"B":').contains('defaultURL: "Page1"')
    TextUtils.findLine(panelsText, '"C":').contains('defaultURL: "http://server:80/page2"')
    TextUtils.findLine(panelsText, '"D":').contains('defaultURL: "https://server:81/page3"')
  }

  //TODO: Find alternative to @Rollback
  def "verify that the marker can handle additional URL arguments added as activityParams"() {
    given: 'a dashboard config and model for the marker'
    def dataModel = [:]
    dataModel[DashboardMarker.ACTIVITY_PARAMETERS_NAME] = [workCenter: 'WC227', location: 'loc_ABC']
    DashboardUnitTestUtils.buildDashboardConfig('TEST', ['/page0'])

    when: 'the HTML is generated'
    def page = execute(source: '<@efDashboard category="NONE"/>', uri: '/dashboard?arg=value', dataModel: dataModel)

    then: 'the HTML is valid'
    checkPage(page)

    and: 'the activity params are passed to the javascript logic for later use in the panels.'
    page.contains("""dashboard._addActivityParameter("workCenter","WC227");""")
    page.contains("""dashboard._addActivityParameter("location","loc_ABC");""")
  }

  //TODO: Find alternative to @Rollback
  def "verify that the mark can handle quotes in the additional activity parameters"() {
    given: 'a dashboard config and model for the marker'
    def dataModel = [:]
    dataModel[DashboardMarker.ACTIVITY_PARAMETERS_NAME] = [workCenter: 'WC"227']
    DashboardUnitTestUtils.buildDashboardConfig('TEST', ['/page0'])

    when: 'the HTML is generated'
    def page = execute(source: '<@efDashboard category="NONE"/>', uri: '/dashboard?arg=value', dataModel: dataModel)

    then: 'the HTML is valid'
    checkPage(page)

    and: 'the activity params are passed to the javascript logic for later use in the panels.'
    page.contains("""dashboard._addActivityParameter("workCenter","WC\\\"227");""")
  }

  //TODO: Find alternative to @Rollback
  def "verify that the tag logs the hierarchy with trace logging"() {
    given: 'a dashboard config'
    DashboardUnitTestUtils.buildDashboardConfig('TEST', ['horizontal0', 'page0', 'page1'])

    and: 'a mock appender to catch the log messages'
    def mockAppender = MockAppender.mock(DashboardMarker, Level.TRACE)

    when: 'the HTML is generated'
    execute(source: '<@efDashboard category="NONE"/>', uri: '/dashboard?arg=value')

    then: 'the message is logged'
    mockAppender.assertMessageIsValid(['TEST', 'page0', 'page1'])

    cleanup:
    MockAppender.cleanup()
  }


}
