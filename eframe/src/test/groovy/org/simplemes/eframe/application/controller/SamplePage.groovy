package org.simplemes.eframe.application.controller

import geb.Page

/*
 * Copyright Michael Houston 2018. All rights reserved.
 * Original Author: mph
 *
*/

/**
 *
 */
class SamplePage extends Page {

  static url = '/test/dashboard'
  //static url = '/'

  static at = { true  /*title.contains 'Dashboard'*/ }

  static content = {
    order { $('#order') }
    passwordInput { $('#password') }
    submitInput { $('input', type: 'submit') }
    errorsLi(required: false) { $('li#errors') }
    startButton { $('div', view_id: 'start').find('button') }
    panelA { $('div', view_id: 'PanelA') }
    panelB { $('div', view_id: 'PanelB') }
    resizer { $('div', view_id: 'resizerA') }
    //startButton { $('div', view_id: 'start')}  // Works
    //startButton { $('button')}  // adding view_id takes extra 32ms to find element.
  }

  boolean hasErrors() {
    !errorsLi.empty
  }

}
