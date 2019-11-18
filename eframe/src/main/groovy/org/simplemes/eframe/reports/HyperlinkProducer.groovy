package org.simplemes.eframe.reports

import net.sf.jasperreports.engine.JRPrintHyperlink
import net.sf.jasperreports.engine.export.JRHyperlinkProducer

/*
 * Copyright (c) 2018 Simple MES, LLC.  All rights reserved.  See license.txt for license terms.
 */

/**
 * Creates a hyperlink for the report engine.  This class creates the hyper link href by adding the
 * controller to the href (e.g. adds '/report?').<p>
 *   <b>Note:</b> The limited API provided by the report engine forces us to use hard-coded report 'loc'
 *    values.  This means the reports must be placed in the correct directory to work with hyperlinks.
 */
class HyperlinkProducer implements JRHyperlinkProducer {
  /**
   * Generates the String hyperlink for a hyperlink instance.
   *
   * @param hyperlink the hyperlink instance
   * @return the generated hyperlink href.
   */
  @Override
  String getHyperlink(JRPrintHyperlink hyperlink) {
    def href = hyperlink.hyperlinkReference
    return "/report?" + href
  }
}
