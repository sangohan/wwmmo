// Code generated by Wire protocol buffer compiler, do not edit.
// Source file: ./messages.proto
package au.com.codeka.common.model;

import com.squareup.wire.Message;
import com.squareup.wire.ProtoField;

import static com.squareup.wire.Message.Datatype.INT64;
import static com.squareup.wire.Message.Datatype.STRING;
import static com.squareup.wire.Message.Label.REQUIRED;

public final class ChatMessage extends Message {

  public static final String DEFAULT_MESSAGE = "";
  public static final String DEFAULT_EMPIRE_KEY = "";
  public static final Long DEFAULT_DATE_POSTED = 0L;
  public static final String DEFAULT_ALLIANCE_KEY = "";
  public static final String DEFAULT_MESSAGE_EN = "";

  @ProtoField(tag = 1, type = STRING, label = REQUIRED)
  public String message;

  @ProtoField(tag = 2, type = STRING)
  public String empire_key;

  @ProtoField(tag = 3, type = INT64)
  public Long date_posted;

  @ProtoField(tag = 4, type = STRING)
  public String alliance_key;

  /**
   * if the message was auto-translated to english, this will be the english
   * translation.
   */
  @ProtoField(tag = 5, type = STRING)
  public String message_en;

  private ChatMessage(Builder builder) {
    super(builder);
    this.message = builder.message;
    this.empire_key = builder.empire_key;
    this.date_posted = builder.date_posted;
    this.alliance_key = builder.alliance_key;
    this.message_en = builder.message_en;
  }

  @Override
  public boolean equals(Object other) {
    if (other == this) return true;
    if (!(other instanceof ChatMessage)) return false;
    ChatMessage o = (ChatMessage) other;
    return equals(message, o.message)
        && equals(empire_key, o.empire_key)
        && equals(date_posted, o.date_posted)
        && equals(alliance_key, o.alliance_key)
        && equals(message_en, o.message_en);
  }

  @Override
  public int hashCode() {
    int result = hashCode;
    if (result == 0) {
      result = message != null ? message.hashCode() : 0;
      result = result * 37 + (empire_key != null ? empire_key.hashCode() : 0);
      result = result * 37 + (date_posted != null ? date_posted.hashCode() : 0);
      result = result * 37 + (alliance_key != null ? alliance_key.hashCode() : 0);
      result = result * 37 + (message_en != null ? message_en.hashCode() : 0);
      hashCode = result;
    }
    return result;
  }

  public static final class Builder extends Message.Builder<ChatMessage> {

    public String message;
    public String empire_key;
    public Long date_posted;
    public String alliance_key;
    public String message_en;

    public Builder() {
    }

    public Builder(ChatMessage message) {
      super(message);
      if (message == null) return;
      this.message = message.message;
      this.empire_key = message.empire_key;
      this.date_posted = message.date_posted;
      this.alliance_key = message.alliance_key;
      this.message_en = message.message_en;
    }

    public Builder message(String message) {
      this.message = message;
      return this;
    }

    public Builder empire_key(String empire_key) {
      this.empire_key = empire_key;
      return this;
    }

    public Builder date_posted(Long date_posted) {
      this.date_posted = date_posted;
      return this;
    }

    public Builder alliance_key(String alliance_key) {
      this.alliance_key = alliance_key;
      return this;
    }

    public Builder message_en(String message_en) {
      this.message_en = message_en;
      return this;
    }

    @Override
    public ChatMessage build() {
      checkRequiredFields();
      return new ChatMessage(this);
    }
  }
}
