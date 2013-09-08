package org.github.rzymek.maven;

import org.apache.maven.model.Plugin;
import org.eclipse.xtend.lib.Data;
import org.eclipse.xtext.xbase.lib.util.ToStringHelper;

@Data
@SuppressWarnings("all")
public class PluginGoal {
  private final Plugin _plugin;
  
  public Plugin getPlugin() {
    return this._plugin;
  }
  
  private final String _goal;
  
  public String getGoal() {
    return this._goal;
  }
  
  public PluginGoal(final Plugin plugin, final String goal) {
    super();
    this._plugin = plugin;
    this._goal = goal;
  }
  
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((_plugin== null) ? 0 : _plugin.hashCode());
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
    PluginGoal other = (PluginGoal) obj;
    if (_plugin == null) {
      if (other._plugin != null)
        return false;
    } else if (!_plugin.equals(other._plugin))
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
