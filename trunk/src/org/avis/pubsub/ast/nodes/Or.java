package org.avis.pubsub.ast.nodes;

import java.util.Map;

import org.avis.pubsub.ast.BoolParentNode;
import org.avis.pubsub.ast.Node;

import static org.avis.pubsub.ast.nodes.Const.CONST_FALSE;
import static org.avis.pubsub.ast.nodes.Const.CONST_TRUE;

public class Or extends BoolParentNode
{
  public Or ()
  {
    super ();
  }
  
  public Or (Node<Boolean> node1)
  {
    super (node1);
  }
  
  public Or (Node<Boolean> node1, Node<Boolean> node2)
  {
    super (node1, node2);
  }
  
  @Override
  public String expr ()
  {
    return "||";
  }
  
  @Override
  public Node<Boolean> inlineConstants ()
  {
    for (int i = children.size () - 1; i >= 0; i--)
    {
      Node<Boolean> child = children.get (i);
      Node<Boolean> newChild = child.inlineConstants ();
      
      Boolean result = newChild.evaluate (EMPTY_NOTIFICATION);
      
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
  public Boolean evaluate (Map<String, Object> attrs)
  {
    Boolean value = FALSE;
    
    for (int i = 0; i < children.size (); i++)
    {
      Boolean result = children.get (i).evaluate (attrs);
      
      if (result == TRUE)
        return TRUE;
      else if (result == BOTTOM)
        value = BOTTOM;
    }
    
    return value;
  }
}
