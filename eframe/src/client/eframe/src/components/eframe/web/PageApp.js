/**
 * Standard App startup services for a normal page-oriented app.
 * Includes all standard features needed by most GUIs: Toast, PrimeVueConfig
 *
 * Basic use:
 *  import PageApp from '@/components/eframe/web/PageApp';
 *  const app = PageApp.createApp('appID')
 *  app.use(...)
 *  app.mount()
 */
'use strict';

import Locales from "@/locales/Locales";

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
  createApp: function (component) {
    const app = createApp(component)
    const i18n = createI18n({
      locale: navigator.language,
      fallbackLocale: 'en',
      legacy: true,
      messages: Locales.getLocales(),
      silentTranslationWarn: true, silentFallbackWarn: true
    });

    app.use(ToastService);
    app.use(PrimeVueConfig);
    app.use(i18n)
    app.use(VueAxios, axios)

    return app
  }
}
