/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.search


import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter

/**
 * Defines a filter that allows all fields to be serialized.  Used for the core filter used by the search interface.
 */
class PassAllJacksonFilter extends SimpleBeanPropertyFilter {
}
