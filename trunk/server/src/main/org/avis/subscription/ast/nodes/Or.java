package org.avis.subscription.ast.nodes;

import java.util.Collection;
import java.util.Map;

import org.avis.subscription.ast.BoolParentNode;
import org.avis.subscription.ast.Node;

import static org.avis.subscription.ast.nodes.Const.CONST_FALSE;
import static org.avis.subscription.ast.nodes.Const.CONST_TRUE;

public class Or extends BoolParentNode
{
  public Or ()
  {
    super ();
  }
  
  public Or (Node node1)
  {
    super (node1);
  }
  
  public Or (Node node1, Node node2)
  {
    super (node1, node2);
  }
  
  public Or (Collection<? extends Node> children)
  {
    super (children);
  }

  public Or (Node... children)
  {
    super (children);
  }

  @Override
  public String expr ()
  {
    return "||";
  }
  
  @Override
  public Node inlineConstants ()
  {
    for (int i = children.size () - 1; i >= 0; i--)
    {
      Node child = children.get (i);
      Node newChild = child.inlineConstants ();
      
      Boolean result = (Boolean)newChild.evaluate (EMPTY_NOTIFICATION);
      
      if (result == TRUE)
        return CONST_TRUE;
      else if (result == FALSE)
        children.remove (i);
      else if (child != newChild)
        children.set (i, newChild);
    }
    
    if (children.isEmpty ())
      return CONST_FALSE;
    else if (children.size () == 1)
      return children.get (0);
    else
      return this;
  }
  
  @Override
  public Object evaluate (Map<String, Object> attrs)
  {
    Boolean value = FALSE;
    
    for (int i = 0; i < children.size (); i++)
    {
      Boolean result = (Boolean)children.get (i).evaluate (attrs);
      
      if (result == TRUE)
        return TRUE;
      else if (result == BOTTOM)
        value = BOTTOM;
    }
    
    return value;
  }
}
