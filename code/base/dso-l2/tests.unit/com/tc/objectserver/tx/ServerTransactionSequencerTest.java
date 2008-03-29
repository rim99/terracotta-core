/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.objectserver.tx;

import com.tc.exception.ImplementMe;
import com.tc.net.groups.ClientID;
import com.tc.net.protocol.tcm.ChannelID;
import com.tc.object.ObjectID;
import com.tc.object.dmi.DmiDescriptor;
import com.tc.object.dna.impl.ObjectStringSerializer;
import com.tc.object.gtx.GlobalTransactionID;
import com.tc.object.gtx.GlobalTransactionIDGenerator;
import com.tc.object.lockmanager.api.LockID;
import com.tc.object.tx.ServerTransactionID;
import com.tc.object.tx.TransactionID;
import com.tc.object.tx.TxnBatchID;
import com.tc.object.tx.TxnType;
import com.tc.objectserver.context.TransactionLookupContext;
import com.tc.objectserver.core.api.TestDNA;
import com.tc.test.TCTestCase;
import com.tc.util.Assert;
import com.tc.util.SequenceID;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

public class ServerTransactionSequencerTest extends TCTestCase {

  private int                            sqID;
  private int                            txnID;
  private int                            batchID;
  private ClientID                       clientID;
  private ServerTransactionSequencerImpl sequencer;
  private int                            start;
  private GlobalTransactionIDGenerator   gidGenerator;

  public ServerTransactionSequencerTest() {
    disableAllUntil("2008-04-15");
  }
  
  public void setUp() throws Exception {
    txnID = 100;
    sqID = 100;
    batchID = 100;
    start = 1;
    clientID = new ClientID(new ChannelID(0));
    sequencer = new ServerTransactionSequencerImpl();
    gidGenerator = new TestGlobalTransactionIDGenerator();
  }

  // Test 1
  // Nothing is pending - disjoint anyway
  public void testNoPendingDisjointTxn() throws Exception {
    List txns = createDisjointTxns(5);
    sequencer.addTransactionLookupContexts(createTxnLookupContexts(txns));
    assertEquals(txns, getAllTxnsPossible());
    assertFalse(sequencer.isPending(txns));
  }

  private Collection<TransactionLookupContext> createTxnLookupContexts(List txns) {
    List contexts = new ArrayList();
    for (Iterator i = txns.iterator(); i.hasNext();) {
      ServerTransaction txn = (ServerTransaction) i.next();
      contexts.add(new TransactionLookupContext(txn, true));
    }
    return contexts;
  }

  // Test 2
  // Nothing is pending - not disjoint though
  public void testNoPendingJointTxn() throws Exception {
    List txns = createIntersectingLocksTxns(5);
    sequencer.addTransactionLookupContexts(createTxnLookupContexts(txns));
    assertEquals(txns, getAllTxnsPossible());
    assertFalse(sequencer.isPending(txns));
  }

  // Test 3
  // txn goes pending - disjoint anyway
  public void testPendingDisJointTxn() throws Exception {
    List txns = createDisjointTxns(5);
    sequencer.addTransactionLookupContexts(createTxnLookupContexts(txns));
    ServerTransaction t1 = sequencer.getNextTxnLookupContextToProcess().getTransaction();
    assertNotNull(t1);
    // Make it pending
    sequencer.makePending(t1);
    assertTrue(sequencer.isPending(txns));
    txns.remove(t1);
    assertEquals(txns, getAllTxnsPossible());
    assertTrue(sequencer.isPending(Arrays.asList(new Object[] { t1 })));
    // No more txns
    assertNull(sequencer.getNextTxnLookupContextToProcess());
    sequencer.makeUnpending(t1);
    assertFalse(sequencer.isPending(Arrays.asList(new Object[] { t1 })));
    // No more txns
    assertNull(sequencer.getNextTxnLookupContextToProcess());
  }

  // Test 4
  // txn goes pending - intersecting set
  public void testPendingJointAtLocksTxn() throws Exception {
    List txns = createIntersectingLocksTxns(5);
    sequencer.addTransactionLookupContexts(createTxnLookupContexts(txns));
    ServerTransaction t1 = sequencer.getNextTxnLookupContextToProcess().getTransaction();
    assertNotNull(t1);
    // Make it pending
    sequencer.makePending(t1);
    assertTrue(sequencer.isPending(txns));
    txns.remove(t1);

    // Since locks are common no txn should be available
    assertNull(sequencer.getNextTxnLookupContextToProcess());
    assertTrue(sequencer.isPending(Arrays.asList(new Object[] { t1 })));
    sequencer.makeUnpending(t1);
    assertFalse(sequencer.isPending(Arrays.asList(new Object[] { t1 })));
    // Rest of the txns
    assertEquals(txns, getAllTxnsPossible());
  }

  // Test 5
  // txn goes pending - intersecting set
  public void testPendingJointAtObjectsTxn() throws Exception {
    List txns = createIntersectingObjectsTxns(5);
    sequencer.addTransactionLookupContexts(createTxnLookupContexts(txns));
    ServerTransaction t1 = sequencer.getNextTxnLookupContextToProcess().getTransaction();
    assertNotNull(t1);
    // Make it pending
    sequencer.makePending(t1);
    assertTrue(sequencer.isPending(txns));
    txns.remove(t1);

    // Since locks are common no txn should be available
    assertNull(sequencer.getNextTxnLookupContextToProcess());
    assertTrue(sequencer.isPending(Arrays.asList(new Object[] { t1 })));
    sequencer.makeUnpending(t1);
    assertFalse(sequencer.isPending(Arrays.asList(new Object[] { t1 })));
    // Rest of the txns
    assertEquals(txns, getAllTxnsPossible());
  }

  // Test 6
  // txn goes pending - intersecting set
  public void testPendingJointAtBothLocksAndObjectsTxn() throws Exception {
    List txns = createIntersectingLocksObjectsTxns(5);
    sequencer.addTransactionLookupContexts(createTxnLookupContexts(txns));
    ServerTransaction t1 = sequencer.getNextTxnLookupContextToProcess().getTransaction();
    assertNotNull(t1);
    // Make it pending
    sequencer.makePending(t1);
    assertTrue(sequencer.isPending(txns));
    txns.remove(t1);

    // Since locks are common no txn should be available
    assertNull(sequencer.getNextTxnLookupContextToProcess());
    assertTrue(sequencer.isPending(Arrays.asList(new Object[] { t1 })));
    sequencer.makeUnpending(t1);
    assertFalse(sequencer.isPending(Arrays.asList(new Object[] { t1 })));
    // Rest of the txns
    assertEquals(txns, getAllTxnsPossible());
  }

  // Test 7
  // Test error conditions
  public void testErrorConditions() throws Exception {
    // Call makepending twice
    List txns = createDisjointTxns(5);
    sequencer.addTransactionLookupContexts(createTxnLookupContexts(txns));
    ServerTransaction t1 = sequencer.getNextTxnLookupContextToProcess().getTransaction();
    assertNotNull(t1);
    sequencer.makePending(t1);
    assertTrue(sequencer.isPending(txns));
    try {
      sequencer.makePending(t1);
      fail();
    } catch (Throwable t) {
      // expected
    }

    // Call make unpending for something that is not pending
    ServerTransaction t2 = sequencer.getNextTxnLookupContextToProcess().getTransaction();
    assertNotNull(t2);
    try {
      sequencer.makeUnpending(t2);
      fail();
    } catch (Throwable t) {
      // expected
    }
    sequencer.makeUnpending(t1);
  }

  public void testOrderingByOID() {
    List txns = new ArrayList();

    int lock = 0;

    ServerTransaction txn1 = new ServerTransactionImpl(gidGenerator, new TxnBatchID(batchID), new TransactionID(1),
                                                       new SequenceID(sqID++), createLocks(lock, lock++), clientID,
                                                       createDNAs(1, 1), new ObjectStringSerializer(),
                                                       Collections.EMPTY_MAP, TxnType.NORMAL, new LinkedList(),
                                                       DmiDescriptor.EMPTY_ARRAY, 1);

    ServerTransaction txn2 = new ServerTransactionImpl(gidGenerator, new TxnBatchID(batchID), new TransactionID(2),
                                                       new SequenceID(sqID++), createLocks(lock, lock++), clientID,
                                                       createDNAs(2, 2), new ObjectStringSerializer(),
                                                       Collections.EMPTY_MAP, TxnType.NORMAL, new LinkedList(),
                                                       DmiDescriptor.EMPTY_ARRAY, 1);

    ServerTransaction txn3 = new ServerTransactionImpl(gidGenerator, new TxnBatchID(batchID), new TransactionID(3),
                                                       new SequenceID(sqID++), createLocks(lock, lock++), clientID,
                                                       createDNAs(2, 3), new ObjectStringSerializer(),
                                                       Collections.EMPTY_MAP, TxnType.NORMAL, new LinkedList(),
                                                       DmiDescriptor.EMPTY_ARRAY, 1);

    ServerTransaction txn4 = new ServerTransactionImpl(gidGenerator, new TxnBatchID(batchID), new TransactionID(4),
                                                       new SequenceID(sqID++), createLocks(lock, lock++), clientID,
                                                       createDNAs(1, 2), new ObjectStringSerializer(),
                                                       Collections.EMPTY_MAP, TxnType.NORMAL, new LinkedList(),
                                                       DmiDescriptor.EMPTY_ARRAY, 1);

    txns.add(txn1);
    txns.add(txn2);
    txns.add(txn3);
    txns.add(txn4);
    sequencer.addTransactionLookupContexts(createTxnLookupContexts(txns));

    sequencer.makePending(sequencer.getNextTxnLookupContextToProcess().getTransaction());
    sequencer.makePending(sequencer.getNextTxnLookupContextToProcess().getTransaction());

    Object o;
    o = sequencer.getNextTxnLookupContextToProcess();
    Assert.assertNull(o);
    o = sequencer.getNextTxnLookupContextToProcess();
    Assert.assertNull(o);

    sequencer.makeUnpending(txn2);
    sequencer.makeUnpending(txn1);

    ServerTransaction shouldBe3 = sequencer.getNextTxnLookupContextToProcess().getTransaction();
    ServerTransaction shouldBe4 = sequencer.getNextTxnLookupContextToProcess().getTransaction();

    Assert.assertEquals(txn3, shouldBe3);
    Assert.assertEquals(txn4, shouldBe4);
  }

  public void testOrderingByLock() {
    List txns = new ArrayList();

    int oid = 0;

    ServerTransaction txn1 = new ServerTransactionImpl(gidGenerator, new TxnBatchID(batchID), new TransactionID(1),
                                                       new SequenceID(sqID++), createLocks(1, 1), clientID,
                                                       createDNAs(oid, oid++), new ObjectStringSerializer(),
                                                       Collections.EMPTY_MAP, TxnType.NORMAL, new LinkedList(),
                                                       DmiDescriptor.EMPTY_ARRAY, 1);

    ServerTransaction txn2 = new ServerTransactionImpl(gidGenerator, new TxnBatchID(batchID), new TransactionID(2),
                                                       new SequenceID(sqID++), createLocks(2, 2), clientID,
                                                       createDNAs(oid, oid++), new ObjectStringSerializer(),
                                                       Collections.EMPTY_MAP, TxnType.NORMAL, new LinkedList(),
                                                       DmiDescriptor.EMPTY_ARRAY, 1);

    ServerTransaction txn3 = new ServerTransactionImpl(gidGenerator, new TxnBatchID(batchID), new TransactionID(3),
                                                       new SequenceID(sqID++), createLocks(2, 3), clientID,
                                                       createDNAs(oid, oid++), new ObjectStringSerializer(),
                                                       Collections.EMPTY_MAP, TxnType.NORMAL, new LinkedList(),
                                                       DmiDescriptor.EMPTY_ARRAY, 1);

    ServerTransaction txn4 = new ServerTransactionImpl(gidGenerator, new TxnBatchID(batchID), new TransactionID(4),
                                                       new SequenceID(sqID++), createLocks(1, 2), clientID,
                                                       createDNAs(oid, oid++), new ObjectStringSerializer(),
                                                       Collections.EMPTY_MAP, TxnType.NORMAL, new LinkedList(),
                                                       DmiDescriptor.EMPTY_ARRAY, 1);

    txns.add(txn1);
    txns.add(txn2);
    txns.add(txn3);
    txns.add(txn4);
    sequencer.addTransactionLookupContexts(createTxnLookupContexts(txns));

    sequencer.makePending(sequencer.getNextTxnLookupContextToProcess().getTransaction());
    sequencer.makePending(sequencer.getNextTxnLookupContextToProcess().getTransaction());

    Object o;
    o = sequencer.getNextTxnLookupContextToProcess();
    Assert.assertNull(o);
    o = sequencer.getNextTxnLookupContextToProcess();
    Assert.assertNull(o);

    sequencer.makeUnpending(txn2);
    sequencer.makeUnpending(txn1);

    ServerTransaction shouldBe3 = sequencer.getNextTxnLookupContextToProcess().getTransaction();
    ServerTransaction shouldBe4 = sequencer.getNextTxnLookupContextToProcess().getTransaction();

    Assert.assertEquals(txn3, shouldBe3);
    Assert.assertEquals(txn4, shouldBe4);
  }

  public void testRandom() {
    for (int i = 0; i < 100; i++) {
      System.err.println("Running testRandom : " + i);
      doRandom();
      sequencer = new ServerTransactionSequencerImpl();
    }
  }

  public void testRandomFailedSeed1() {
    long seed = -7748167846395034562L;
    System.err.println("Testing failed seed : " + seed);
    doRandom(seed);
  }

  public void testRandomFailedSeed2() {
    long seed = -149113776740941224L;
    System.err.println("Testing failed seed : " + seed);
    doRandom(seed);
  }

  // XXX: multi-threaded version of this?
  // XXX: add cases with locks in common between TXNs
  private void doRandom() {
    final long seed = new SecureRandom().nextLong();
    System.err.println("seed is " + seed);
    doRandom(seed);
  }

  private void doRandom(long seed) {
    Random rnd = new Random(seed);

    int lock = 0;
    final int numObjects = 25;
    long versionsIn[] = new long[numObjects];
    long versionsRecv[] = new long[numObjects];

    Set pending = new HashSet();

    for (int loop = 0; loop < 5000; loop++) {
      List txns = new ArrayList();
      for (int i = 0, n = rnd.nextInt(3) + 1; i < n; i++) {
        txns.add(createRandomTxn(rnd.nextInt(3) + 1, versionsIn, rnd, lock++));
      }

      sequencer.addTransactionLookupContexts(createTxnLookupContexts(txns));

      TransactionLookupContext next = null;
      while ((next = sequencer.getNextTxnLookupContextToProcess()) != null) {
        if (rnd.nextInt(3) == 0) {
          ServerTransaction txn = next.getTransaction();
          sequencer.makePending(txn);
          pending.add(next.getTransaction());
          continue;
        }

        processTransaction(next.getTransaction(), versionsRecv);

        if (pending.size() > 0 && rnd.nextInt(4) == 0) {
          for (int i = 0, n = rnd.nextInt(pending.size()); i < n; i++) {
            Iterator iter = pending.iterator();
            ServerTransaction pendingTxn = (ServerTransaction) iter.next();
            iter.remove();
            processTransaction(pendingTxn, versionsRecv);
            sequencer.makeUnpending(pendingTxn);
          }
        }
      }

    }
  }

  private void processTransaction(ServerTransaction next, long[] versionsRecv) {
    for (Iterator iter = next.getChanges().iterator(); iter.hasNext();) {
      TestDNA dna = (TestDNA) iter.next();
      int oid = (int) dna.getObjectID().toLong();
      long ver = dna.version;
      long expect = versionsRecv[oid] + 1;
      if (expect != ver) {
        //
        throw new AssertionError(oid + " : Expected change to increment to version " + expect
                                 + ", but change was to version " + ver);
      }
      versionsRecv[oid] = ver;
    }
  }

  private ServerTransaction createRandomTxn(int numObjects, long[] versions, Random rnd, int lockID) {
    Map dnas = new HashMap();
    while (numObjects > 0) {
      int i = rnd.nextInt(versions.length);
      if (!dnas.containsKey(new Integer(i))) {
        TestDNA dna = new TestDNA(new ObjectID(i));
        dna.version = ++versions[i];
        dnas.put(new Integer(i), dna);
        numObjects--;
      }
    }

    return new ServerTransactionImpl(gidGenerator, new TxnBatchID(batchID), new TransactionID(txnID++),
                                     new SequenceID(sqID++), createLocks(lockID, lockID), clientID, new ArrayList(dnas
                                         .values()), new ObjectStringSerializer(), Collections.EMPTY_MAP,
                                     TxnType.NORMAL, new LinkedList(), DmiDescriptor.EMPTY_ARRAY, 1);
  }

  private List getAllTxnsPossible() {
    List txns = new ArrayList();
    TransactionLookupContext txnLC;
    while ((txnLC = sequencer.getNextTxnLookupContextToProcess()) != null) {
      txns.add(txnLC.getTransaction());
    }
    return txns;
  }

  private List createDisjointTxns(int count) {
    List txns = new ArrayList();
    batchID++;
    int j = 3;
    while (count-- > 0) {
      int e = start + j;
      txns.add(new ServerTransactionImpl(gidGenerator, new TxnBatchID(batchID), new TransactionID(txnID++),
                                         new SequenceID(sqID++), createLocks(start, e), clientID, createDNAs(start, e),
                                         new ObjectStringSerializer(), Collections.EMPTY_MAP, TxnType.NORMAL,
                                         new LinkedList(), DmiDescriptor.EMPTY_ARRAY, 1));
      start = e + 1;
    }
    return txns;
  }

  private List createIntersectingLocksTxns(int count) {
    List txns = new ArrayList();
    batchID++;
    int j = 3;
    while (count-- > 0) {
      int e = start + j;
      txns.add(new ServerTransactionImpl(gidGenerator, new TxnBatchID(batchID), new TransactionID(txnID++),
                                         new SequenceID(sqID++), createLocks(start, e + j), clientID, createDNAs(start,
                                                                                                                 e),
                                         new ObjectStringSerializer(), Collections.EMPTY_MAP, TxnType.NORMAL,
                                         new LinkedList(), DmiDescriptor.EMPTY_ARRAY, 1));
      start = e + 1;
    }
    return txns;
  }

  private List createIntersectingObjectsTxns(int count) {
    List txns = new ArrayList();
    batchID++;
    int j = 3;
    while (count-- > 0) {
      int e = start + j;
      txns.add(new ServerTransactionImpl(gidGenerator, new TxnBatchID(batchID), new TransactionID(txnID++),
                                         new SequenceID(sqID++), createLocks(start, e), clientID,
                                         createDNAs(start, e + j), new ObjectStringSerializer(), Collections.EMPTY_MAP,
                                         TxnType.NORMAL, new LinkedList(), DmiDescriptor.EMPTY_ARRAY, 1));
      start = e + 1;
    }
    return txns;
  }

  private List createIntersectingLocksObjectsTxns(int count) {
    List txns = new ArrayList();
    batchID++;
    int j = 3;
    while (count-- > 0) {
      int e = start + j;
      txns.add(new ServerTransactionImpl(gidGenerator, new TxnBatchID(batchID), new TransactionID(txnID++),
                                         new SequenceID(sqID++), createLocks(start, e + j), clientID,
                                         createDNAs(start, e + j), new ObjectStringSerializer(), Collections.EMPTY_MAP,
                                         TxnType.NORMAL, new LinkedList(), DmiDescriptor.EMPTY_ARRAY, 1));
      start = e + 1;
    }
    return txns;
  }

  private List createDNAs(int s, int e) {
    List dnas = new ArrayList();
    for (int i = s; i <= e; i++) {
      dnas.add(new TestDNA(new ObjectID(i)));
    }
    return dnas;
  }

  private LockID[] createLocks(int s, int e) {
    LockID[] locks = new LockID[e - s + 1];
    for (int j = s; j <= e; j++) {
      locks[j - s] = new LockID("@" + j);
    }
    return locks;
  }

  private final static class TestGlobalTransactionIDGenerator implements GlobalTransactionIDGenerator {

    long id = 0;

    public GlobalTransactionID getOrCreateGlobalTransactionID(ServerTransactionID serverTransactionID) {
      return new GlobalTransactionID(id++);
    }

    public GlobalTransactionID getLowGlobalTransactionIDWatermark() {
      throw new ImplementMe();
    }

  }
}
