package org.totschnig.myexpenses.graphql;

import com.google.gson.annotations.SerializedName;

public class GraphQLQuery {
  public static GraphQLQuery create(String endCursor) {
    GraphQLQuery graphQLQuery = new GraphQLQuery();
    graphQLQuery.query = String.format("query { repository ( owner:\"mtotschnig\", name:\"MyExpenses\" ) { issues(first:100, states:OPEN %s) { nodes { title number } pageInfo { endCursor hasNextPage } } } }",
        endCursor != null ? String.format("after: \"%s\"", endCursor) : "");
    return graphQLQuery;
  }

  @SerializedName("query")
  String query;


  public String getQuery() {
    return query;
  }
}
