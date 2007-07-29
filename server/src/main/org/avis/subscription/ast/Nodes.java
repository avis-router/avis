package org.avis.subscription.ast;

import java.util.List;

import java.io.StringWriter;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import org.avis.subscription.ast.nodes.Or;
import org.avis.util.IndentingWriter;

/**
 * General utilities for node trees.
 */
public final class Nodes
{
  private Nodes ()
  {
    // zip
  }

  /**
   * Generate a pretty-printed version of a node tree suitable for
   * human consumption. Uses {@link Node#presentation()} to generate
   * text for each node.
   * 
   * @see #unparse(Node)
   */
  public static String toString (Node node)
  {
    IndentingWriter out = new IndentingWriter (new StringWriter ());
    
    print (out, node);
    
    return out.toString ();
  }

  private static void print (IndentingWriter out, Node node)
  {
    out.print (node.presentation ());
    
    if (node.hasChildren ())
    {
      out.indent ();
      
      for (Node child : node.children ())
      {
        out.println ();
        
        print (out, child);
      }
      
      out.unindent ();
    }
  }

  /**
   * "Unparse" an AST to a canonical S-expression-like string
   * expression useful for testing purposes. Uses the
   * {@link Node#expr()} method to unparse each node.
   * 
   * e.g. <code>field1 > 2 && field2 == 'hello there'</code> becomes
   * <code>(&& (> (field 'field1') (int32 2))
   * (== (field 'field2') (string 'hello there')))</code>
   * 
   * @see Node#expr()
   * @see #toString(Node)
   */
  public static String unparse (Node node)
  {
    StringBuilder str = new StringBuilder ();
    
    unparse (str, node);
    
    return str.toString ();
  }

  private static void unparse (StringBuilder str, Node node)
  {
    if (node.hasChildren ())
    {
      str.append ('(');
      
      str.append (node.expr ());

      for (Node child : node.children ())
      {
        str.append (' ');
       
        unparse (str, child);
      }

      str.append (')');
    } else
    {
      str.append (node.expr ());
    }
  }

  /**
   * Allow a node that usually operates on two arguments to optionally
   * operate on any number with an OR conjunction. e.g. can be used to
   * turn <tt>begins-with (name, 'value1', 'value2')</tt> into
   * <tt>begins-with (name, 'value') || begins-with (name, 'value2')</tt>.
   * 
   * @param <T> The node type to generate.
   * @param type The node type to generate.
   * @param constParam1 The constructor's first parameter type.
   * @param constParam2 The constructor's second parameter type.
   * @param args A list of arguments for the node (must be >= 2). If
   *          these are longer than 2, then arg0 is paired with
   *          arg1... as children to an OR parent node.
   * @return Either an instance of T or Or with T children.
   */
  public static <T extends Node> 
    Node createConjunction (Class<T> type,
                            Class<?> constParam1,
                            Class<?> constParam2,
                            List<? extends Node> args)
  {
    try
    {
      Constructor<T> cons =
        type.getConstructor (constParam1, constParam2);
      
      if (args.size () == 2)
      {
        return cons.newInstance (args.get (0), args.get (1));
      } else
      {
        Node arg0 = args.get (0);
        
        Or or = new Or ();
        
        for (int i = 1; i < args.size (); i++)
          or.addChild (cons.newInstance (arg0, args.get (i)));
        
        return or;
      }
    } catch (InvocationTargetException ex)
    {
      if (ex.getCause () instanceof RuntimeException)
        throw (RuntimeException)ex.getCause ();
      else
        throw new Error (ex);
    } catch (Exception ex)
    {
      throw new Error (ex);
    }
  }
}

