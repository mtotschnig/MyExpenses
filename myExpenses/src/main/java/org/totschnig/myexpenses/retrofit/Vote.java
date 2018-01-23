package org.totschnig.myexpenses.retrofit;


import com.google.gson.annotations.SerializedName;

import java.util.HashMap;

public class Vote {
  public Vote(String key, HashMap<Integer, Integer> vote, boolean isPro) {
    this.key = key;
    this.vote = vote;
    this.isPro = isPro;
  }

  public boolean isPro() {
    return isPro;
  }

  public String getKey() {
    return key;
  }

  public HashMap<Integer, Integer> getVote() {
    return vote;
  }

  @SerializedName("pro")
  boolean isPro;

  @SerializedName("key")
  String key;

  @SerializedName("vote")
  HashMap<Integer, Integer> vote;
}
