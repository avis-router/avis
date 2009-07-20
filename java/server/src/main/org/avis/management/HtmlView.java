package org.avis.management;

/**
 * An component that generates an HTML-rendered view of some kind.
 * 
 * @author Matthew Phillips
 */
public interface HtmlView
{
  /**
   * Render this view onto the given HTML.
   */
  public void render (HTML html);
}
