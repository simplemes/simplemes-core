/*
 * Copyright (c) Michael Houston 2021. All rights reserved.
 */

package org.simplemes.eframe.custom;

import java.util.HashMap;

/**
 * A simple parent class for the Groovy FieldHolderMap class.  Used to simplify the
 * {@link org.simplemes.eframe.data.annotation.ExtensibleFieldHolderTransformation} creation of the dummy fields
 * needed for a field holder.  Used as Java class so it is available in the Java world.
 * The Groovy class implements most of the logic.
 */
public class BaseFieldHolderMap extends HashMap {
}
