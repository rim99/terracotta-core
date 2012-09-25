/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.persistence.db;

import com.tc.objectserver.persistence.api.StringIndexPersistor;
import com.tc.objectserver.persistence.db.DBPersistorImpl.DBPersistorBase;
import com.tc.objectserver.storage.api.PersistenceTransaction;
import com.tc.objectserver.storage.api.PersistenceTransactionProvider;
import com.tc.objectserver.storage.api.TCLongToStringDatabase;

import gnu.trove.TLongObjectHashMap;

import java.util.concurrent.atomic.AtomicBoolean;

public final class StringIndexPersistorImpl extends DBPersistorBase implements StringIndexPersistor {

  private final PersistenceTransactionProvider ptp;
  private final TCLongToStringDatabase         stringIndexDatabase;
  private final AtomicBoolean                  initialized = new AtomicBoolean(false);

  public StringIndexPersistorImpl(PersistenceTransactionProvider ptp, TCLongToStringDatabase stringIndexDatabase) {
    this.ptp = ptp;
    this.stringIndexDatabase = stringIndexDatabase;
  }

  public TLongObjectHashMap loadMappingsInto(TLongObjectHashMap target) {
    if (initialized.getAndSet(true)) throw new AssertionError("Attempt to use more than once.");
    PersistenceTransaction tx = ptp.newTransaction();
    try {
      return stringIndexDatabase.loadMappingsInto(target, tx);
    } finally {
      tx.commit();
    }
  }

  public void saveMapping(long index, String string) {
    PersistenceTransaction tx = ptp.newTransaction();
    try {
      stringIndexDatabase.insert(index, string, tx);
    } catch (Throwable t) {
      throw new DBException(t);
    } finally {
      tx.commit();
    }
  }
}