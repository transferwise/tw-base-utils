package com.transferwise.common.baseutils.meters.cache;

import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import java.util.Arrays;
import javax.annotation.Nonnull;

public abstract class TagsSet {

  private static final TagsSet EMPTY = TagsSet.of(new String[0]);

  public static TagsSet empty() {
    return EMPTY;
  }

  public static TagsSet of(Iterable<Tag> tags) {
    return new IterableTagsSet(tags);
  }

  public static TagsSet of(String... tags) {
    return new StringArrayTagsSet(tags);
  }

  public static TagsSet of(Tag tag) {
    return new SingleTagsSet(tag);
  }

  public static TagsSet of(Tag... tags) {
    return new ArrayTagsSet(tags);
  }

  public abstract Tags getMicrometerTags();

  static class ArrayTagsSet extends TagsSet {

    private Tag[] tags;
    private int hashCode;

    public ArrayTagsSet(@Nonnull Tag[] tags) {
      this.tags = tags;
      this.hashCode = Arrays.hashCode(tags);
    }

    @Override
    public int hashCode() {
      return hashCode;
    }

    @Override
    public boolean equals(Object other) {
      if (this == other) {
        return true;
      }
      if (other == null) {
        return false;
      }
      if (!(other instanceof ArrayTagsSet)) {
        return false;
      }

      return Arrays.equals(this.tags, ((ArrayTagsSet) other).tags);
    }

    @Override
    public Tags getMicrometerTags() {
      return Tags.of(tags);
    }
  }

  static class SingleTagsSet extends TagsSet {

    private Tag tag;
    private int hashCode;

    public SingleTagsSet(@Nonnull Tag tag) {
      this.tag = tag;
      this.hashCode = tag.hashCode();
    }

    @Override
    public int hashCode() {
      return hashCode;
    }

    @Override
    public boolean equals(Object other) {
      if (this == other) {
        return true;
      }
      if (other == null) {
        return false;
      }
      if (!(other instanceof SingleTagsSet)) {
        return false;
      }

      return this.tag.equals(((SingleTagsSet) other).tag);
    }

    @Override
    public Tags getMicrometerTags() {
      return Tags.of(tag);
    }
  }

  static class StringArrayTagsSet extends TagsSet {

    private String[] tags;
    private int hashCode;

    public StringArrayTagsSet(String[] tags) {
      this.tags = tags;
      hashCode = Arrays.hashCode(tags);
    }

    @Override
    public int hashCode() {
      return hashCode;
    }

    @Override
    public boolean equals(Object other) {
      if (this == other) {
        return true;
      }
      if (other == null) {
        return false;
      }
      if (!(other instanceof StringArrayTagsSet)) {
        return false;
      }

      return Arrays.equals(this.tags, ((StringArrayTagsSet) other).tags);
    }

    @Override
    public Tags getMicrometerTags() {
      return Tags.of(tags);
    }
  }

  static class IterableTagsSet extends TagsSet {

    private Iterable<Tag> tags;
    private int hashCode;

    public IterableTagsSet(Iterable<Tag> tags) {
      this.tags = tags;
      hashCode = tags.hashCode();
    }

    @Override
    public int hashCode() {
      return hashCode;
    }

    @Override
    public boolean equals(Object other) {
      if (this == other) {
        return true;
      }
      if (other == null) {
        return false;
      }
      if (!(other instanceof IterableTagsSet)) {
        return false;
      }

      return this.tags.equals(((IterableTagsSet) other).tags);
    }

    @Override
    public Tags getMicrometerTags() {
      return Tags.of(tags);
    }
  }
}
