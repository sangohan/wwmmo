// Code generated by Wire protocol buffer compiler, do not edit.
// Source file: ./messages.proto
package au.com.codeka.common.model;

import com.squareup.wire.Message;
import com.squareup.wire.ProtoField;
import java.util.Collections;
import java.util.List;

import static com.squareup.wire.Message.Label.REPEATED;

public final class Planets extends Message {

  public static final List<Planet> DEFAULT_PLANETS = Collections.emptyList();

  @ProtoField(tag = 1, label = REPEATED)
  public List<Planet> planets;

  private Planets(Builder builder) {
    super(builder);
    this.planets = immutableCopyOf(builder.planets);
  }

  @Override
  public boolean equals(Object other) {
    if (other == this) return true;
    if (!(other instanceof Planets)) return false;
    return equals(planets, ((Planets) other).planets);
  }

  @Override
  public int hashCode() {
    int result = hashCode;
    return result != 0 ? result : (hashCode = planets != null ? planets.hashCode() : 0);
  }

  public static final class Builder extends Message.Builder<Planets> {

    public List<Planet> planets;

    public Builder() {
    }

    public Builder(Planets message) {
      super(message);
      if (message == null) return;
      this.planets = copyOf(message.planets);
    }

    public Builder planets(List<Planet> planets) {
      this.planets = planets;
      return this;
    }

    @Override
    public Planets build() {
      return new Planets(this);
    }
  }
}
