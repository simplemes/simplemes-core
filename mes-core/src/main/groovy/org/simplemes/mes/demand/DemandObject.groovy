package org.simplemes.mes.demand

/*
 * Copyright Michael Houston 2017. All rights reserved.
 * Original Author: mph
 *
*/

/**
 * A Marker interface to flag an Order or LSN class.  Used in place of Object to handle an Order or LSN
 * as a single input parameter.  This simplifies many APIs that can accept either an Order or LSN.
 * <p>
 * The API will generally check if the object is an instance of an Order or LSN.
 *
 */
interface DemandObject {

}