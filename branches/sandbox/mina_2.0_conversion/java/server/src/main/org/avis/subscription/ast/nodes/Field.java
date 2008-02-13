package org.avis.subscription.ast.nodes;

import java.util.Collection;
import java.util.Map;

import org.avis.subscription.ast.NameParentNode;
import org.avis.subscription.ast.Node;

import static java.util.Collections.singleton;

public class Field extends NameParentNode
{
  public Field (String name)
  {
    super (name);
  }
  
  public String fieldName ()
  {
    return name;
  }
  
  @Override
  public String expr ()
  {
    return "field";
  }

  @Override
  public Class<?> evalType ()
  {
    return Object.class;
  }
  
  @Override
  public Object evaluate (Map<String, Object> attrs)
  {
    return attrs.get (name);
  }
  
  /**
   * Children of name-based nodes are by default Field's, but this
   * would be recursive for a Field.
   */
  @Override
  public Collection<Node> children ()
  {
    return singleton ((Node)new Const (name));
  }
}
