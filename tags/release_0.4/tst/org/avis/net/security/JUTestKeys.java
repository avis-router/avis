package org.avis.net.security;

import org.apache.mina.common.ByteBuffer;

import org.junit.Test;

import static org.avis.net.security.DualKeyScheme.CONSUMER;
import static org.avis.net.security.DualKeyScheme.PRODUCER;
import static org.avis.net.security.KeyScheme.SHA1_PRODUCER;
import static org.avis.net.security.KeyScheme.SHA1_CONSUMER;
import static org.avis.net.security.KeyScheme.SHA1_DUAL;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class JUTestKeys
{
  @Test public void IO ()
    throws Exception
  {
    ByteBuffer buff = ByteBuffer.allocate (1024);
    
    Keys keys = new Keys ();
    
    keys.add (SHA1_DUAL, PRODUCER, new Key ("dual key 1"));
    keys.add (SHA1_DUAL, CONSUMER, new Key ("dual key 2"));
    keys.add (SHA1_PRODUCER, new Key ("producer key 1"));
    keys.add (SHA1_CONSUMER, new Key ("consumer key 1"));
    keys.add (SHA1_CONSUMER, new Key ("consumer key 2"));
    
    // test roundtrip encode/decode
    keys.encode (buff);
    
    buff.flip ();
    
    Keys newKeys = Keys.decode (buff);
    
    assertEquals (keys, newKeys);
  }
  
  @Test public void equality ()
  {
    assertTrue
      (new Key ("a test key number 1").equals (new Key ("a test key number 1")));
    assertFalse
      (new Key ("a test key number 1").equals (new Key ("a test key number 2")));
    
    assertEquals (new Key ("a test key number 1").hashCode (),
                  new Key ("a test key number 1").hashCode ());
    
    // test Keys.equals ()
    Keys keys1 = new Keys ();
    keys1.add (SHA1_DUAL, PRODUCER, new Key ("dual key 1"));
    keys1.add (SHA1_DUAL, CONSUMER, new Key ("dual key 2"));
    
    keys1.add (SHA1_PRODUCER, new Key ("producer key 1"));
    keys1.add (SHA1_CONSUMER, new Key ("consumer key 1"));
    
    Keys keys2 = new Keys ();
    keys2.add (SHA1_DUAL, PRODUCER, new Key ("dual key 1"));
    keys2.add (SHA1_DUAL, CONSUMER, new Key ("dual key 2"));
    
    keys2.add (SHA1_PRODUCER, new Key ("producer key 1"));
    keys2.add (SHA1_CONSUMER, new Key ("consumer key 1"));
    
    assertEquals (keys1.hashCode (), keys2.hashCode ());
    assertEquals (keys1, keys2);
    
    keys2.remove (SHA1_CONSUMER, new Key ("consumer key 1"));
    
    assertFalse (keys1.equals (keys2));
  }
  
  /**
   * Test add/remove of single keys.
   */
  @Test public void addRemove ()
    throws Exception
  {
    Keys keys = new Keys ();
    
    keys.add (SHA1_DUAL, PRODUCER, new Key ("dual key 1"));
    keys.add (SHA1_DUAL, CONSUMER, new Key ("dual key 2"));
    
    keys.add (SHA1_PRODUCER, new Key ("producer key 1"));
    keys.add (SHA1_CONSUMER, new Key ("consumer key 1"));
    
    DualKeySet dualKeys = keys.keysetFor (SHA1_DUAL);
    
    assertEquals (1, dualKeys.consumerKeys.size ());
    assertEquals (1, dualKeys.producerKeys.size ());
    
    keys.add (SHA1_DUAL, PRODUCER, new Key ("dual key 3"));
    assertEquals (2, keys.keysetFor (SHA1_DUAL).producerKeys.size ());
    
    keys.remove (SHA1_DUAL, PRODUCER, new Key ("dual key 3"));
    assertEquals (1, keys.keysetFor (SHA1_DUAL).producerKeys.size ());
    
    keys.remove (SHA1_DUAL, PRODUCER, new Key ("dual key 1"));
    assertEquals (0, keys.keysetFor (SHA1_DUAL).producerKeys.size ());
    
    // remove a non-existent key, check nothing Bad happens
    keys.remove (SHA1_CONSUMER, new Key ("blah"));
    
    keys.remove (SHA1_DUAL, CONSUMER, new Key ("dual key 2"));
    assertEquals (0, keys.keysetFor (SHA1_DUAL).consumerKeys.size ());
    
    SingleKeySet consumerKeys = keys.keysetFor (SHA1_CONSUMER);
    assertEquals (1, consumerKeys.size ());
    keys.remove (SHA1_CONSUMER, new Key ("consumer key 1"));
    assertEquals (0, consumerKeys.size ());
    
    SingleKeySet producerKeys = keys.keysetFor (SHA1_PRODUCER);
    assertEquals (1, producerKeys.size ());
    keys.remove (SHA1_PRODUCER, new Key ("producer key 1"));
    assertEquals (0, producerKeys.size ());
    
    assertTrue (keys.isEmpty ());
    
    // remove a non-existent key, check nothing Bad happens
    keys.remove (SHA1_CONSUMER, new Key ("blah"));
  }
  
  /**
   * Test add/remove of entire keysets.
   */
  @Test public void addRemoveSets ()
    throws Exception
  {
    Keys keys1 = new Keys ();
    Keys keys2 = new Keys ();
    Keys keys3 = new Keys ();
    
    keys1.add (SHA1_DUAL, PRODUCER, new Key ("dual key prod 1"));
    keys1.add (SHA1_DUAL, CONSUMER, new Key ("dual key cons 1"));
    keys1.add (SHA1_PRODUCER, new Key ("producer key 1.1"));
    keys1.add (SHA1_PRODUCER, new Key ("producer key 1.2"));
    
    keys2.add (SHA1_DUAL, PRODUCER, new Key ("dual key prod 2"));
    keys2.add (SHA1_DUAL, CONSUMER, new Key ("dual key cons 2"));
    keys2.add (SHA1_CONSUMER, new Key ("consumer key 1"));
    
    // add keys in bulk to keys3
    keys3.add (keys1);
    
    assertEquals (1, keys3.keysetFor (SHA1_DUAL).consumerKeys.size ());
    assertEquals (1, keys3.keysetFor (SHA1_DUAL).producerKeys.size ());
    assertEquals (2, keys3.keysetFor (SHA1_PRODUCER).size ());
    assertEquals (0, keys3.keysetFor (SHA1_CONSUMER).size ());
    
    keys3.add (keys2);
    assertEquals (2, keys3.keysetFor (SHA1_DUAL).consumerKeys.size ());
    assertEquals (2, keys3.keysetFor (SHA1_DUAL).producerKeys.size ());
    assertEquals (2, keys3.keysetFor (SHA1_PRODUCER).size ());
    assertEquals (1, keys3.keysetFor (SHA1_CONSUMER).size ());
    
    keys3.remove (keys1);
    assertEquals (1, keys3.keysetFor (SHA1_DUAL).consumerKeys.size ());
    assertEquals (1, keys3.keysetFor (SHA1_DUAL).producerKeys.size ());
    assertEquals (0, keys3.keysetFor (SHA1_PRODUCER).size ());
    assertEquals (1, keys3.keysetFor (SHA1_CONSUMER).size ());
    
    keys3.remove (keys2);
    
    assertTrue (keys3.isEmpty ());
  }
  
  @Test
  public void producer ()
  {
    Key alicePrivate = new Key ("alice private");
    Key alicePublic = alicePrivate.publicKeyFor (SHA1_PRODUCER);
    
    Keys aliceNtfnKeys = new Keys ();
    aliceNtfnKeys.add (SHA1_PRODUCER, alicePrivate);
    
    Keys bobSubKeys = new Keys ();
    bobSubKeys.add (SHA1_PRODUCER, alicePublic);
    
    Keys eveSubKeys = new Keys ();
    eveSubKeys.add (SHA1_PRODUCER,
                    new Key ("Not alice's key").publicKeyFor (SHA1_PRODUCER));
    
    assertTrue (bobSubKeys.match (aliceNtfnKeys));
    assertFalse (eveSubKeys.match (aliceNtfnKeys));
  }
  
  @Test
  public void consumer ()
  {
    Key bobPrivate = new Key ("bob private");
    Key bobPublic = bobPrivate.publicKeyFor (SHA1_CONSUMER);
    
    Keys aliceNtfnKeys = new Keys ();
    aliceNtfnKeys.add (SHA1_CONSUMER, bobPublic);

    Keys bobSubKeys = new Keys ();
    bobSubKeys.add (SHA1_CONSUMER, bobPrivate);
    
    Keys eveSubKeys = new Keys ();
    eveSubKeys.add (SHA1_CONSUMER, new Key ("Not bob's key"));
    
    assertTrue (bobSubKeys.match (aliceNtfnKeys));
    assertFalse (eveSubKeys.match (aliceNtfnKeys));
  }
  
  @Test
  public void dual ()
  {
    Key alicePrivate = new Key ("alice private");
    Key alicePublic = alicePrivate.publicKeyFor (SHA1_DUAL);
    Key bobPrivate = new Key ("bob private");
    Key bobPublic = bobPrivate.publicKeyFor (SHA1_DUAL);
    
    Keys aliceNtfnKeys = new Keys ();
    aliceNtfnKeys.add (SHA1_DUAL, CONSUMER, bobPublic);
    aliceNtfnKeys.add (SHA1_DUAL, PRODUCER, alicePrivate);

    Keys bobSubKeys = new Keys ();
    bobSubKeys.add (SHA1_DUAL, CONSUMER, bobPrivate);
    bobSubKeys.add (SHA1_DUAL, PRODUCER, alicePublic);
    
    Keys eveSubKeys = new Keys ();
    Key randomPrivate = new Key ("Not bob's key");
    eveSubKeys.add (SHA1_DUAL, CONSUMER, randomPrivate);
    eveSubKeys.add (SHA1_DUAL, PRODUCER, randomPrivate.publicKeyFor (SHA1_DUAL));
    
    assertTrue (bobSubKeys.match (aliceNtfnKeys));
    assertFalse (aliceNtfnKeys.match (bobSubKeys));
    assertFalse (eveSubKeys.match (aliceNtfnKeys));
  }
}
