/**
 * Basic CRUD page for a single object.
 */

import AllFieldsDomainCrud from '@/components/sample/AllFieldsDomainCrud'
import PageApp from '@/eframe-lib/web/PageApp'
import '@/eframe-lib/assets/styles/global.css'

import Locales from "@/locales/Locales"


const app = PageApp.createApp(AllFieldsDomainCrud, Locales)
app.mount('#app')





