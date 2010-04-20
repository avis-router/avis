package org.avis.management;

/**
 * HtmlView's that need to add their own items in the page's head section
 * implement this.
 * 
 * @author Matthew Phillips
 */
public interface HtmlViewWithCustomHeaders extends HtmlView
{
  public void addHeaders (HTML html);
}
