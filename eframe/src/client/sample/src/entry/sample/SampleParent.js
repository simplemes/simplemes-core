/**
 * Basic CRUD page for a single object.
 */

import SampleParentCrud from '@/components/sample/SampleParentCrud.vue'
import PageApp from '@/eframe-lib/web/PageApp'
import '@/eframe-lib/assets/styles/global.css'

import Locales from "@/locales/Locales"


const app = PageApp.createApp(SampleParentCrud, Locales)
app.mount('#app')





