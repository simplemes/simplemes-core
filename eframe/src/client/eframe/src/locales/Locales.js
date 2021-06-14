/*
 * Copyright (c) Michael Houston 2021. All rights reserved.
 */

/**
 * Provides the getLocales() function to define all supported locales for this client.
 */

import en from '@/locales/en.js';
import de from './de.js';

export default {
  getLocales() {
    return {en: en.getMessages(), de: de.getMessages()}
  },
}