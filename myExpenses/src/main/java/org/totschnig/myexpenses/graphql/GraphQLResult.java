package org.totschnig.myexpenses.graphql;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class GraphQLResult {
  @SerializedName("data")
  private Data data;

  public Data getData() {
    return data;
  }

  public class Data {
    @SerializedName("repository")
    private Repository repository;

    public Repository getRepository() {
      return repository;
    }
  }

  public class Repository {
    @SerializedName("issues")
    private Issues issues;

    public Issues getIssues() {
      return issues;
    }
  }

  public class Issues {
    @SerializedName("nodes")
    private List<Node> nodes;
    @SerializedName("pageInfo")
    private PageInfo pageInfo;

    public List<Node> getNodes() {
      return nodes;
    }

    public PageInfo getPageInfo() {
      return pageInfo;
    }
  }

  public class Node {
    @SerializedName("title")
    private String title;

    @SerializedName("number")
    private int number;

    public String getTitle() {
      return title;
    }
    
    public int getNumber() {
      return number;
    }
  }

  public class PageInfo {
    private String endCursor;
    private boolean hasNextPage;

    public String getEndCursor() {
      return endCursor;
    }

    public boolean isHasNextPage() {
      return hasNextPage;
    }
  }
}
