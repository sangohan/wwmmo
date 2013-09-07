// Code generated by Wire protocol buffer compiler, do not edit.
// Source file: ./messages.proto
package au.com.codeka.common.model;

import com.squareup.wire.Message;
import com.squareup.wire.ProtoField;
import java.util.Collections;
import java.util.List;

import static com.squareup.wire.Message.Label.REPEATED;

public final class Stars extends Message {

  public static final List<Star> DEFAULT_STARS = Collections.emptyList();

  @ProtoField(tag = 1, label = REPEATED)
  public List<Star> stars;

  private Stars(Builder builder) {
    super(builder);
    this.stars = immutableCopyOf(builder.stars);
  }

  @Override
  public boolean equals(Object other) {
    if (other == this) return true;
    if (!(other instanceof Stars)) return false;
    return equals(stars, ((Stars) other).stars);
  }

  @Override
  public int hashCode() {
    int result = hashCode;
    return result != 0 ? result : (hashCode = stars != null ? stars.hashCode() : 0);
  }

  public static final class Builder extends Message.Builder<Stars> {

    public List<Star> stars;

    public Builder() {
    }

    public Builder(Stars message) {
      super(message);
      if (message == null) return;
      this.stars = copyOf(message.stars);
    }

    public Builder stars(List<Star> stars) {
      this.stars = stars;
      return this;
    }

    @Override
    public Stars build() {
      return new Stars(this);
    }
  }
}
