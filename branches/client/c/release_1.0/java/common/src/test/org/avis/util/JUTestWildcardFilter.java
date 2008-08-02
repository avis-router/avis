package org.avis.util;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class JUTestWildcardFilter
{
  @Test
  public void wildcards () 
    throws Exception
  {
    assertFalse (new WildcardFilter ().matches (""));
    assertTrue  (new WildcardFilter ("").matches (""));
    assertFalse (new WildcardFilter ("").matches ("a"));
    assertTrue  (new WildcardFilter ("abc").matches ("abc"));
    assertFalse (new WildcardFilter ("abc").matches ("abcd"));
    assertTrue  (new WildcardFilter ("abc*").matches ("abcd"));
    assertTrue  (new WildcardFilter ("abc*z").matches ("abcdefgz"));
    assertTrue  (new WildcardFilter ("def", "abc*").matches ("abcd"));
    assertTrue  (new WildcardFilter ("abc?").matches ("abcd"));
    assertFalse (new WildcardFilter ("abc?").matches ("abcde"));
  }
}
