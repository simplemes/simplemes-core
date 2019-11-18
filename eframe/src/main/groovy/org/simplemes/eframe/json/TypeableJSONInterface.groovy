package org.simplemes.eframe.json

/*
 * Copyright Michael Houston 2018. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * This interface flags any POGO elements that are allowed to be stored in a typeable JSON list.
 * This is a list that contains the class name as an element and lets Jackson restore any typeable class
 * from the JSON.  This is used mainly for storing POGOs in a database TEXT/CLOB field.
 */
interface TypeableJSONInterface {

}