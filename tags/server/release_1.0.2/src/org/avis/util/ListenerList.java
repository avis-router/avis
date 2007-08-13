package org.avis.util;

import java.util.ArrayList;
import java.util.List;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * A generic event listener list. Not thread safe.
 * 
 * @author Matthew Phillips
 */
public class ListenerList<E>
{
  private List<E> listeners;
  
  public ListenerList ()
  {
    this.listeners = new ArrayList<E> ();
  }
  
  public void add (E listener)
  {
    listeners.add (listener);
  }
  
  public void remove (E listener)
  {
    listeners.remove (listener);
  }

  /**
   * Fire an event.
   * 
   * @param method The method to call.
   * @param event The event parameter.
   */
  public void fire (String method, Object event)
  {
    Method listenerMethod = null; // lazy init
    Object [] args = null;        // lazy init
    
    for (int i = listeners.size () - 1; i >= 0; i--)
    {
      E listener = listeners.get (i);
      
      if (listenerMethod == null)
      {
        listenerMethod =
          lookupMethod (listener.getClass (), method, event.getClass ());
        
        args = new Object [] {event};
      }
      
      try
      {
        listenerMethod.invoke (listener, args); 
      } catch (InvocationTargetException ex)
      {
        throw new RuntimeException ("Error in listener method",
                                    ex.getCause ());
      } catch (Exception ex)
      {
        // should not be possible
        throw new RuntimeException (ex);
      }
    }
  }

  private static Method lookupMethod (Class<?> targetClass,
                                      String methodName,
                                      Class<?> paramClass)
  {
    try
    {
      Method method = targetClass.getMethod (methodName, paramClass);
      
      method.setAccessible (true);
      
      return method;
    } catch (Exception ex)
    {
      throw new IllegalArgumentException ("No method named " + methodName);
    }
  }
}
