package org.lobobrowser.ua;

/**
 * Immutable snapshot of request permission counts for a frame.
 */
public final class RequestStats {
  public static final RequestStats EMPTY = new RequestStats(0, 0);

  public final int blocked;
  public final int allowed;

  public RequestStats(final int blocked, final int allowed) {
    this.blocked = blocked;
    this.allowed = allowed;
  }

  public int getTotal() {
    return blocked + allowed;
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof RequestStats)) {
      return false;
    }
    final RequestStats other = (RequestStats) obj;
    return (blocked == other.blocked) && (allowed == other.allowed);
  }

  @Override
  public int hashCode() {
    return (31 * blocked) + allowed;
  }

  @Override
  public String toString() {
    return "RequestStats[blocked=" + blocked + ", allowed=" + allowed + "]";
  }
}
