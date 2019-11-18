package org.simplemes.eframe.web.ui.webix.freemarker


import org.simplemes.eframe.application.Holders
import org.simplemes.eframe.dashboard.domain.DashboardConfig
import org.simplemes.eframe.i18n.GlobalUtils
import org.simplemes.eframe.misc.TypeUtils

/*
 * Copyright Michael Houston 2018. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * Provides the implementation of the efTitle/&gt; marker.
 * This marker is used to display a standardized title for a given page.  This is common in almost
 * all pages, so a central way to define it is provided.  You can use your own titles as you see fit,
 * but this can help reduce the boiler-plate code in your application. Also, the prevalent use of tabs in most browsers mean
 * title display space is limited.  Most types of this standard title will try to place the most important information first
 * in the title.  For example, to display the title for a show page for a product should be something like:
 *
 */
@SuppressWarnings("unused")
class TitleMarker extends BaseMarker {
  /**
   * The valid title types.
   */
  protected static final validTypes = ['main', 'list', 'create', 'show', 'edit', 'dashboard']

  /**
   * Executes the directive, with the values passed by the setValues() method.
   */
  @Override
  void execute() {
    def type = (parameters?.type ?: 'main').toLowerCase()
    if (!validTypes.contains(type)) {
      throw new MarkerException("efTitle: Invalid type ${type}.  Valid types: $validTypes", this)
    }
    def s = null
    def appTitle = Holders.configuration.appName

    if (type == 'main') {
      if (parameters.label) {
        def label = GlobalUtils.lookup((String) parameters.label, null)
        // main.app.title={0} - {1}
        s = GlobalUtils.lookup('main.app.title', null, [label, appTitle] as Object[])
      } else {
        s = appTitle
      }
    } else if (type == 'list' || type == 'create') {
      // Needs a domain name only.
      def domainLabel = getDomainLabel()
      s = GlobalUtils.lookup("${type}.title", null, [domainLabel, appTitle] as Object[])
    } else if (type == 'edit' || type == 'show') {
      // Needs a domain name and a domain object
      def domainLabel = getDomainLabel()
      if (!domainObject) {
        domainObject = '-unknown-'
      }
      def objectString = escape(TypeUtils.toShortString(domainObject))
      s = GlobalUtils.lookup("${type}.title", null, [objectString, domainLabel, appTitle] as Object[])
    } else if (type == 'dashboard') {
      def dashboardCategory = parameters.dashboardCategory
      def dashboard = parameters.dashboard
      if (!dashboardCategory && !dashboard) {
        throw new MarkerException("Tag <@efTitle> for dashboard requires dashboardCategory or dashboard value.", this)
      }
      def t = ''
      DashboardConfig.withTransaction {
        def dashboardConfig
        if (dashboardCategory) {
          dashboardConfig = DashboardConfig.findByCategoryAndDefaultConfig((String) dashboardCategory, true)
        } else {
          dashboardConfig = DashboardConfig.findByDashboard(dashboard)
        }
        t = dashboardConfig?.title ?: dashboard ?: dashboardCategory
      }
      s = "$t - ${lookup('dashboard.label')} - $appTitle"
    }

    write(s)
  }

  /**
   * Returns the desired domain object's display label.
   * @return The label.
   */
  protected String getDomainLabel() {
    def label = (String) parameters?.label ?: domainObjectName
    if (label) {
      label += '.label'
    }
    if (!label) {
      throw new MarkerException("efTitle: No controller or label found. Perhaps use label='' option.", this)
    }
    return GlobalUtils.lookup(label)
  }

}

