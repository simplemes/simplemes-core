/**
 * Basic CRUD page for a single object.
 */

import FlexTypeCrud from '@/components/eframe/custom/flexType/FlexTypeCrud.vue'
import PageApp from '@/eframe-lib/web/PageApp.js'
import '@/eframe-lib/assets/styles/global.css'

import Locales from "@/locales/Locales"


const app = PageApp.createApp(FlexTypeCrud, Locales)
app.mount('#app')





