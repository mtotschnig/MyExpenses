package org.totschnig.myexpenses.retrofit;


import com.google.gson.annotations.SerializedName;

import java.util.Map;

public class Vote {

  public Vote(String key, Map<Integer, Integer> vote, boolean isPro, String email, int version) {
    this.key = key;
    this.vote = vote;
    this.isPro = isPro;
    this.email = email;
    this.version = version;
  }

  public boolean isPro() {
    return isPro;
  }

  public String getKey() {
    return key;
  }

  public Map<Integer, Integer> getVote() {
    return vote;
  }

  public String getEmail() {
    return email;
  }

  public int getVersion() {
    return version;
  }

  @SerializedName("email")
  String email;

  @SerializedName("pro")
  boolean isPro;

  @SerializedName("key")
  String key;

  @SerializedName("vote")
  Map<Integer, Integer> vote;

  @SerializedName("version")
  int version = 0;
}
