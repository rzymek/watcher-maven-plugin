package org.github.rzymek.maven;

import org.eclipse.xtend.lib.Data;
import org.eclipse.xtext.xbase.lib.util.ToStringHelper;

@Data
@SuppressWarnings("all")
public class Goal {
  private final String _prefix;
  
  public String getPrefix() {
    return this._prefix;
  }
  
  private final String _goal;
  
  public String getGoal() {
    return this._goal;
  }
  
  public Goal(final String prefix, final String goal) {
    super();
    this._prefix = prefix;
    this._goal = goal;
  }
  
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((_prefix== null) ? 0 : _prefix.hashCode());
    result = prime * result + ((_goal== null) ? 0 : _goal.hashCode());
    return result;
  }
  
  @Override
  public boolean equals(final Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    Goal other = (Goal) obj;
    if (_prefix == null) {
      if (other._prefix != null)
        return false;
    } else if (!_prefix.equals(other._prefix))
      return false;
    if (_goal == null) {
      if (other._goal != null)
        return false;
    } else if (!_goal.equals(other._goal))
      return false;
    return true;
  }
  
  @Override
  public String toString() {
    String result = new ToStringHelper().toString(this);
    return result;
  }
}
