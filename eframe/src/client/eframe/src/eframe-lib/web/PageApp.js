/*
 * Copyright (c) Michael Houston 2021. All rights reserved.
 */

/**
 * Standard App startup services for a normal page-oriented app.
 * Includes all standard features needed by most GUIs: Toast, PrimeVueConfig
 *
 * Basic use:
 *  import PageApp from 'eframe-lib/components/web/PageApp';
 *  const app = PageApp.createApp('appID')
 *  app.use(...)
 *  app.mount()
 */
'use strict';

import libLocales from "../locales/Locales"
import MultiLocales from "./MultiLocales"

import {createApp} from 'vue'
import ToastService from "primevue/toastservice";
import PrimeVueConfig from "primevue/config";
import VueAxios from "vue-axios";
import axios from "axios";

import 'primevue/resources/themes/saga-blue/theme.css';
import 'primevue/resources/primevue.min.css';
import 'primeicons/primeicons.css';
import 'primeflex/primeflex.css';

import {createI18n} from 'vue-i18n'


export default {
  createApp: function (component, moduleLocales) {
    MultiLocales.addLocale(libLocales)
    MultiLocales.addLocale(moduleLocales)
    const app = createApp(component)
    const i18n = createI18n({
      locale: navigator.language,
      fallbackLocale: 'en',
      legacy: true,
      messages: MultiLocales.getLocales(),
      silentTranslationWarn: true, silentFallbackWarn: true
    });

    app.use(ToastService);
    app.use(PrimeVueConfig);
    console.log('ABC_6')

    app.component('router-link', i18n) // Use a dummy router-link component to avoid missing component warning.
    app.use(i18n)
    app.use(VueAxios, axios)


    // Define some
    var numberOfAjaxCallsPending = 0;

    // Add a request interceptor
    axios.interceptors.request.use(function (config) {
      numberOfAjaxCallsPending++;
      //console.log("numberOfAjaxCAllPending1: "+numberOfAjaxCallsPending);

      // show loader
      return config;
    }, function (error) {
      return Promise.reject(error);
    });

    // Add a response interceptor
    axios.interceptors.response.use(function (response) {
      numberOfAjaxCallsPending--;
      //console.log("------------  Ajax pending", numberOfAjaxCallsPending);

      if (numberOfAjaxCallsPending == 0) {
        //hide loader
      }
      return response;
    }, function (error) {
      numberOfAjaxCallsPending--;
      if (numberOfAjaxCallsPending == 0) {
        //hide loader
      }
      return Promise.reject(error);
    });

    // Create a global check function available to the GUI testing.
    // eslint-disable-next-line no-unused-vars
    function ajaxPending() {
      return numberOfAjaxCallsPending > 0
    }

    window._ajaxPending = ajaxPending

    return app
  }
}

