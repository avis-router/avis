package org.avis.pubsub.ast.nodes;

import java.util.List;
import java.util.Map;

import org.avis.pubsub.ast.Node;
import org.avis.pubsub.ast.Nodes;
import org.avis.pubsub.ast.ParentBiNode;

import static org.avis.util.Numbers.highestPrecision;
import static org.avis.util.Numbers.upconvert;

/**
 * @author Matthew Phillips
 */
public class Compare extends ParentBiNode<Boolean, Comparable>
{
  private int inequality;
  private int equality;
  
  public static Node<Boolean> create (List<Node<? extends Comparable>> args)
  {
    if (args.size () == 2)
    {
      return new Compare (args.get (0), args.get (1), 0, 0);
    } else
    {
      Node<? extends Comparable> arg0 = args.get (0);
      
      Or or = new Or ();
      
      for (int i = 1; i < args.size (); i++)
        or.addChild (new Compare (arg0, args.get (i), 0, 0));
      
      return or;
    }
  }
  
  public Compare (Node<? extends Comparable> child1,
                  Node<? extends Comparable> child2,
                  int inequality, int equality)
  {
    this.inequality = inequality;
    this.equality = equality;
  
    init (child1, child2);
  }

  @Override
  protected String validateChild (Node child)
  {
    Class childType = child.evalType ();
    
    if (childType == Object.class)
    {
      // allow generic nodes such as fields
      return null;
    } else if (childType != Number.class &&
               (childType == Boolean.class ||
                !Comparable.class.isAssignableFrom (childType)))
    {
      return expr () + " cannot have expression of type " +
             Nodes.className (childType) + " as an argument";
    } else if (child1 != null)
    {
      Class evalType = child1.evalType ();
    
      if (evalType != Object.class &&
          evalType != childType)
      {
        if (!(Number.class.isAssignableFrom (evalType) &&
              Number.class.isAssignableFrom (childType)))
        {
          return expr () + ": argument ("  +
                 child.expr () + ") cannot be compared to " +
                 "(" + child1.expr () + ")";
        }
      }
    }   
    
    return null;
  }
  
  @Override
  public Class evalType ()
  {
    return Boolean.class;
  }

  @Override
  public String expr ()
  {
    StringBuilder str = new StringBuilder ();
    
    if (inequality < 0)
      str.append ('<');
    else if (inequality > 0)
      str.append ('>');
    
    if (equality == 0)
    {
      if (str.length () == 0)
        str.append ("==");
      else
        str.append ('=');
    }
    
    return str.toString ();
  }
  
  @Override
  public String presentation ()
  {
    return "Compare: " + expr ();
  }

  @Override
  @SuppressWarnings("unchecked")
  public Boolean evaluate (Map<String, Object> attrs)
  {
    Object result1 = child1.evaluate (attrs);
    
    if (!(result1 instanceof Comparable))
      return BOTTOM;

    Object result2 = child2.evaluate (attrs);
    
    if (!(result2 instanceof Comparable))
      return BOTTOM;
    
    Class class1 = result1.getClass ();
    Class class2 = result2.getClass ();
    
    // check for compatible types
    if (class1 != class2)
    {
      // if numeric, can upconvert
      if (class1.getSuperclass () == Number.class &&
          class2.getSuperclass () == Number.class)
      {
        Class newType = highestPrecision (class1, class2);
        
        if (class1 != newType)
          result1 = upconvert ((Number)result1, class2);
        else
          result2 = upconvert ((Number)result2, class1);
      } else
      {
        // incompatible types
        return BOTTOM;
      }
    }
    
    int compare = ((Comparable)result1).compareTo (result2);
    
    if (compare == 0)
      return equality == 0;
    else if (compare < 0)
      return inequality < 0;
    else
      return inequality > 0;
  }
}
