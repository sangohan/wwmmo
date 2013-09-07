// Code generated by Wire protocol buffer compiler, do not edit.
// Source file: ./messages.proto
package au.com.codeka.common.model;

import com.squareup.wire.Message;
import com.squareup.wire.ProtoField;
import java.util.Collections;
import java.util.List;

import static com.squareup.wire.Message.Label.REPEATED;

public final class DeviceRegistrations extends Message {

  public static final List<DeviceRegistration> DEFAULT_REGISTRATIONS = Collections.emptyList();

  @ProtoField(tag = 1, label = REPEATED)
  public List<DeviceRegistration> registrations;

  private DeviceRegistrations(Builder builder) {
    super(builder);
    this.registrations = immutableCopyOf(builder.registrations);
  }

  @Override
  public boolean equals(Object other) {
    if (other == this) return true;
    if (!(other instanceof DeviceRegistrations)) return false;
    return equals(registrations, ((DeviceRegistrations) other).registrations);
  }

  @Override
  public int hashCode() {
    int result = hashCode;
    return result != 0 ? result : (hashCode = registrations != null ? registrations.hashCode() : 0);
  }

  public static final class Builder extends Message.Builder<DeviceRegistrations> {

    public List<DeviceRegistration> registrations;

    public Builder() {
    }

    public Builder(DeviceRegistrations message) {
      super(message);
      if (message == null) return;
      this.registrations = copyOf(message.registrations);
    }

    public Builder registrations(List<DeviceRegistration> registrations) {
      this.registrations = registrations;
      return this;
    }

    @Override
    public DeviceRegistrations build() {
      return new DeviceRegistrations(this);
    }
  }
}