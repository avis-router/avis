package org.avis.io.messages;

import java.util.Map;

import org.avis.security.Keys;

import static org.avis.security.Keys.EMPTY_KEYS;

public class NotifyEmit extends Notify
{
  public static final int ID = 56;

  public NotifyEmit ()
  {
    super ();
  }
  
  public NotifyEmit (Map<String, Object> attributes)
  {
    this (attributes, true, EMPTY_KEYS);
  }
  
  public NotifyEmit (Map<String, Object> attributes,
                     boolean deliverInsecure,
                     Keys keys)
  {
    super (attributes, deliverInsecure, keys);
  }

  @Override
  public int typeId ()
  {
    return ID;
  }
}
