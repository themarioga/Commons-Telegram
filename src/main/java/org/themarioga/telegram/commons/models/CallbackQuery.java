package org.themarioga.telegram.commons.models;

public class CallbackQuery {

    private String query;
    private String queryData;

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public String getQueryData() {
        return queryData;
    }

    public void setQueryData(String queryData) {
        this.queryData = queryData;
    }

    @Override
    public String toString() {
        return "CallbackQuery{" + "query='" + query + '\'' + ", queryData='" + queryData + '\'' + '}';
    }

}
