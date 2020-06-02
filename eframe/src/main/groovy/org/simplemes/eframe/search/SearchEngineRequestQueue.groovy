/*
 * Copyright (c) Michael Houston 2020. All rights reserved.
 */

package org.simplemes.eframe.search

import java.util.concurrent.LinkedTransferQueue

/**
 * The search request queue.  This is mostly a standard LinkedTransferQueue, but it tries to transfer the
 * request to a waiting thread.  If not possible, then a new thread may be created or the request will be
 * queued.
 */
class SearchEngineRequestQueue extends LinkedTransferQueue<Runnable> {
  @Override
  boolean offer(Runnable e) {
    return tryTransfer(e)
  }
}
