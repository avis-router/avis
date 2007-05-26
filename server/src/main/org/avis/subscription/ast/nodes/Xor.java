package org.avis.subscription.ast.nodes;

import java.util.Map;

import org.avis.subscription.ast.BoolParentNode;
import org.avis.subscription.ast.Node;

public class Xor extends BoolParentNode
{
  public Xor (Node<Boolean> node1)
  {
    super (node1);
  }
  
  public Xor (Node<Boolean> node1, Node<Boolean> node2)
  {
    super (node1, node2);
  }
  
  @Override
  public String expr ()
  {
    return "^^";
  }
  
  @Override
  public Node<Boolean> inlineConstants ()
  {
    for (int i = children.size () - 1; i >= 0; i--)
    {
      Node<Boolean> child = children.get (i);
      Node<Boolean> newChild = child.inlineConstants ();
      
      if (child != newChild)
        children.set (i, newChild);
    }
    
    Boolean result = evaluate (EMPTY_NOTIFICATION);
    
    if (result != BOTTOM)
      return Const.bool (result);
    else
      return this;
  }
  
  @Override
  public Boolean evaluate (Map<String, Object> attrs)
  {
    Boolean value = FALSE;
    
    for (int i = 0; i < children.size (); i++)
    {
      Boolean result = children.get (i).evaluate (attrs);
      
      if (result == BOTTOM)
      {
        return BOTTOM;
      } else if (result == TRUE)
      {
        if (value == TRUE)
          return FALSE;
        else
          value = TRUE;
      }
    }
    
    return value;
  }
}
