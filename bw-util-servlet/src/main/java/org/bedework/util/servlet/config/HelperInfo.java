package org.bedework.util.servlet.config;

import java.util.ArrayList;
import java.util.List;

public class HelperInfo {
  private AppInfo parent;
  private final String name;
  private final String className;
  private final List<ForwardInfo> forwards = new ArrayList<>();
  private final List<HelperParameter> parameters =
          new ArrayList<>();

  // For jackson
  public HelperInfo() {
    this(null, null);
  }

  public HelperInfo(final String name,
                    final String className) {
    this.name = name;
    this.className = className;
  }

  public void setParent(final AppInfo val) {
    parent = val;
  }

  public AppInfo getParent() {
    return parent;
  }

  public String getName() {
    return name;
  }

  public String getClassName() {
    return className;
  }

  public List<ForwardInfo> getForwards() {
    return forwards;
  }

  public ForwardInfo getForward(final String name) {
    for (final ForwardInfo fi: forwards) {
      if (fi.getName().equals(name)) {
        return fi;
      }
    }

    var ai = parent;
    while (ai != null) {
      final ForwardInfo fi = ai.getDefaultForward(name);
      if (fi != null) {
        return fi;
      }
      ai = ai.getParent();
    }

    return null;
  }

  public HelperInfo addForward(final String name,
                               final String path,
                               final boolean redirect) {
    forwards.add(new ForwardInfo(name, path, redirect));

    return this;
  }

  public List<HelperParameter> getParameters() {
    return parameters;
  }

  public HelperParameter getParameter(final String name) {
    for (final var p: parameters) {
      if (p.getName().equals(name)) {
        return p;
      }
    }

    return null;
  }

  public HelperInfo addParameter(final HelperParameter val) {
    parameters.add(val);

    return this;
  }
}
