package org.avis.io.messages;

import java.util.Collections;
import java.util.Map;

import org.apache.mina.common.ByteBuffer;
import org.apache.mina.filter.codec.ProtocolCodecException;

import static org.avis.io.XdrCoding.getLongArray;
import static org.avis.io.XdrCoding.getNameValues;
import static org.avis.io.XdrCoding.putLongArray;
import static org.avis.io.XdrCoding.putNameValues;

public class NotifyDeliver extends Message
{
  public static final int ID = 57;

  public Map<String, Object> attributes;
  public long [] secureMatches;
  public long [] insecureMatches;
  
  public NotifyDeliver ()
  {
    this.attributes = Collections.emptyMap ();
  }
  
  public NotifyDeliver (Map<String, Object> attributes,
                        long [] secureMatches, long [] insecureMatches)
  {
    this.attributes = attributes;
    this.secureMatches = secureMatches;
    this.insecureMatches = insecureMatches;
  }

  @Override
  public int typeId ()
  {
    return ID;
  }

  @Override
  public void encode (ByteBuffer out)
    throws ProtocolCodecException
  {
    putNameValues (out, attributes);
    putLongArray (out, secureMatches);
    putLongArray (out, insecureMatches);
  }

  @Override
  public void decode (ByteBuffer in)
    throws ProtocolCodecException
  {
    attributes = getNameValues (in);
    secureMatches = getLongArray (in);
    insecureMatches = getLongArray (in);
  }
}
