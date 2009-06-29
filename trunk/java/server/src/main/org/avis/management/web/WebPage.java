package org.avis.management.web;

import java.nio.charset.Charset;

import org.apache.asyncweb.common.DefaultHttpResponse;
import org.apache.asyncweb.common.MutableHttpResponse;
import org.apache.asyncweb.server.HttpService;
import org.apache.asyncweb.server.HttpServiceContext;
import org.apache.mina.core.buffer.IoBuffer;

import static org.apache.asyncweb.common.HttpResponseStatus.OK;

public abstract class WebPage implements HttpService
{
  public void handleRequest (HttpServiceContext context)
    throws Exception
  {
    MutableHttpResponse response = new DefaultHttpResponse ();
    response.setHeader ("Pragma", "no-cache");
    response.setHeader ("Cache-Control", "no-cache");
    response.setHeader ("Content-Type", "text/html; charset=UTF-8");
    response.setStatus (OK);

    IoBuffer out = IoBuffer.allocate (1024);
    out.setAutoExpand (true);

    out.putString (content (), Charset.forName ("UTF-8").newEncoder ());
    out.flip ();
    
    response.setContent (out);

    context.commitResponse (response);
  }

  protected abstract String content ();
  
  public void start ()
  {
    // zip
  }

  public void stop ()
  {
    // zip
  }
}
