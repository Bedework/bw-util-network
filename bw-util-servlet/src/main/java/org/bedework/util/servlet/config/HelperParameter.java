package org.bedework.util.servlet.config;

import org.bedework.base.ToString;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class HelperParameter {
  private final String name;
  private final String value;

  @JsonCreator
  public HelperParameter(
          @JsonProperty("name") final String name,
          @JsonProperty("value") final String value) {
    this.name = name;
    this.value = value;
  }

  public String getName() {
    return name;
  }

  public String getValue() {
    return value;
  }

  public String toString() {
    return new ToString(this)
            .append("name", getName())
            .append("value", getValue())
            .toString();
  }
}
