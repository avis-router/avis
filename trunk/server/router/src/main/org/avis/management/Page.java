package org.avis.management;

public class Page
{
  public String uri;
  public String title;
  public HtmlView view;
  
  public Page (String uri, String title, HtmlView view)
  {
    this.uri = uri;
    this.title = title;
    this.view = view;
  }  
}
