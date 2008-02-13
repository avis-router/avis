package org.avis.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;

import static java.util.Arrays.asList;
import static java.util.Collections.singleton;
import static java.util.regex.Pattern.CASE_INSENSITIVE;

import static org.avis.util.Wildcard.toPattern;

/**
 * A filter that matches strings against wildcard patterns.
 * 
 * @author Matthew Phillips
 */
public class WildcardFilter implements Filter<String>
{
  private List<Pattern> patterns;
  
  public WildcardFilter (String wildcardPattern)
  {
    this (singleton (wildcardPattern));
  }
  
  public WildcardFilter (String... wildcardPatterns)
  {
    this (asList (wildcardPatterns));
  }
  
  public WildcardFilter (Collection<String> wildcardPatterns)
  {
    this.patterns = new ArrayList<Pattern> (wildcardPatterns.size ());
    
    for (String wildcardExpr : wildcardPatterns)
      patterns.add (toPattern (wildcardExpr, CASE_INSENSITIVE));
  }
  
  public boolean isNull ()
  {
    return patterns.isEmpty ();
  }
  
  public boolean matches (String string)
  {
    for (Pattern pattern : patterns)
    {
      if (pattern.matcher (string).matches ())
        return true;
    }
    
    return false;
  }
}
