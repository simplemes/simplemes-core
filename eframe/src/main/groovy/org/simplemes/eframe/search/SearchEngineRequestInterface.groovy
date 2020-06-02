/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.search

/**
 * This is the common interface for all request handled by the search engine clients.
 * This interface mainly provides the run() method.
 * Since this is run in the background, any errors detected by the request object should be logged or
 * an exception should be thrown.
 */
interface SearchEngineRequestInterface extends Runnable {

}
