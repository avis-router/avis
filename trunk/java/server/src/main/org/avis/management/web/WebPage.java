package org.avis.management.web;

import java.nio.charset.Charset;

import org.apache.asyncweb.common.DefaultHttpResponse;
import org.apache.asyncweb.common.HttpResponseStatus;
import org.apache.asyncweb.common.MutableHttpResponse;
import org.apache.asyncweb.server.HttpService;
import org.apache.asyncweb.server.HttpServiceContext;
import org.apache.mina.core.buffer.IoBuffer;

public abstract class WebPage implements HttpService
{
  public void handleRequest (HttpServiceContext context)
    throws Exception
  {
    MutableHttpResponse response = new DefaultHttpResponse ();

//    StringWriter buf = new StringWriter ();
//    PrintWriter writer = new PrintWriter (buf);
//    writer
//        .println ("<html><body><b>Your message of the day:</b><br/><br/>");
//    writer.println ("<h2><i>Hello!</h2></i><br/><br/>");
//    writeHeaders (context.getRequest (), writer);
//    writer.println ("<br/>");
//    writeParameters (context.getRequest (), writer);
//    writer.println ("<br/>");
//    writeCookies (context.getRequest (), writer);
//    writer.flush ();

    IoBuffer out = IoBuffer.allocate (1024);
    out.setAutoExpand (true);
    out.putString (htmlText (), Charset.forName ("UTF-8").newEncoder ());
    out.flip ();
    response.setContent (out);

    response.setHeader ("Pragma", "no-cache");
    response.setHeader ("Cache-Control", "no-cache");
    response.setStatus (HttpResponseStatus.OK);

    context.commitResponse (response);
  }

  protected abstract CharSequence htmlText ();
  
  public void start ()
  {
    // zip
  }

  public void stop ()
  {
    // zip
  }
}
