import {createApp} from 'vue'
import FlexType from '@/components/eframe/custom/flexType/FlexTypeCrud.vue';
import '@/assets/styles/global.css';
import Locales from '@/locales/Locales.js';

import 'primevue/resources/themes/saga-blue/theme.css';
import 'primevue/resources/primevue.min.css';
import 'primeicons/primeicons.css';
import 'primeflex/primeflex.css';
import ToastService from 'primevue/toastservice';

import {createStore} from 'vuex'
import PrimeVueConfig from 'primevue/config';
import {createI18n} from 'vue-i18n'

import axios from 'axios'
import VueAxios from 'vue-axios'

//import {language} from '../../../src/locales/en.json'


export const store = createStore({
  state() {
    return {
      count: 1,
      selectedOrdersLSNs: [],
    }
  },
  getters: {
    /**
     * Returns the list of selected orders/LSNs in a comma=delimited list.  Useful in an input text field.
     * @param state The current state from the store.
     * @returns {string} The selected summary (e.g. 'M1001, M1002').
     */
    getSelectionString(state) {
      let s = '';
      for (let row of state.selectedOrdersLSNs) {
        if (s.length) {
          s += ', ';
        }
        let x = row.order;
        if (row.lsn) {
          x = row.lsn;
        }
        s += x;
      }
      return s;
    },
    /**
     * Summarizes the selected orders/LSNs.  Suitable as a note next to the field or tooltip.
     * @param state The current state from the store.
     * @returns {string} The selected summary (e.g. '(2 Selected)').  Empty string if none are selected.
     */
    getSelectionCount(state) {
      return state.selectedOrdersLSNs.length
    },
  },
  mutations: {
    setSelectedOrdersLSNs(state, selectedList) {
      state.selectedOrdersLSNs = selectedList;
    }
  }
})

const i18n = createI18n({
  locale: navigator.language,
  fallbackLocale: 'en',
  legacy: true,
  messages: Locales.getLocales(),
  silentTranslationWarn: true, silentFallbackWarn: true
});


const app = createApp(FlexType);
//window.app = app
//window.$app = app.component('OrderDashboard');
//console.log(app.component('OrderDashboard'));


app.use(store);
app.use(ToastService);
app.use(PrimeVueConfig);
app.use(i18n)
app.use(VueAxios, axios)

app.mount('#app');

//app.showToast();
//app.$toast.add({severity:'success', summary: 'Success Message', detail:'Order submitted', life: 3000});





