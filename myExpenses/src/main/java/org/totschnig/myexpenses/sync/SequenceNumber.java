package org.totschnig.myexpenses.sync;

import org.totschnig.myexpenses.BuildConfig;
import org.totschnig.myexpenses.util.Utils;

import java.util.Locale;

public class SequenceNumber {
  private static final int LIMIT = BuildConfig.DEBUG ? 2 : 100;

  int shard;
  int number;

  public SequenceNumber(int shard, int number) {
    this.shard = shard;
    this.number = number;
  }

  public static SequenceNumber max(SequenceNumber first, SequenceNumber second) {
    switch (Utils.compare(first.shard, second.shard)) {
      case 1:
        return first;
      case -1:
        return second;
      default:
        return first.number >= second.number ? first : second;
    }
  }

  public static SequenceNumber parse(String serialized) {
    final String[] parts = serialized.split("_");
    if (parts.length == 2) {
      return new SequenceNumber(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]));
    } else {
      return new SequenceNumber(0, Integer.parseInt(parts[0]));
    }
  }

  public SequenceNumber next() {
    return number >= LIMIT ? new SequenceNumber(shard + 1, 1) :
        new SequenceNumber(shard, number + 1);
  }

  @Override
  public String toString() {
    return String.format(Locale.ROOT, "%d_%d", shard, number);
  }
}
