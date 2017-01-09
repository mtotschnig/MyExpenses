package org.totschnig.myexpenses.sync;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.junit.Before;
import org.junit.Test;
import org.totschnig.myexpenses.sync.json.AdapterFactory;
import org.totschnig.myexpenses.sync.json.TransactionChange;
import org.totschnig.myexpenses.sync.json.Utils;

import java.io.StringReader;
import java.util.List;
import java.util.Locale;

import static org.junit.Assert.*;

public class ChangeReaderTest {
  private Gson gson;

  @Before
  public void setup() {
    gson = new GsonBuilder()
        .registerTypeAdapterFactory(AdapterFactory.create())
        .create();
  }

  @Test
  public void shouldParseIntoListOfChanges() {
    TransactionChange.Type created = TransactionChange.Type.created;
    String uuid = "825ec542-a434-4954-b59e-e47b71138b35";
    long timestamp = 1475560175;
    long date = 1475559751;
    long amount = -12300;
    String crStatus = "UNRECONCILED";
    TransactionChange expected = TransactionChange.builder().setType(created).setUuid(uuid).setTimeStamp(timestamp).setDate(date).setAmount(amount).setCrStatus(crStatus).build();
    StringReader reader = new StringReader(String.format(Locale.US,
        "[{\"type\":\"%s\",\"uuid\":\"%s\",\"timeStamp\":%d,\"date\":%d,\"amount\":%d,\"crStatus\":\"%s\"}]",
        created, uuid, timestamp, date, amount, crStatus));
    List<TransactionChange> result = Utils.getChanges(gson, reader);
    assertEquals(1, result.size());
    assertEquals(expected, result.get(0));
  }
}
