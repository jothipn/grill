package com.inmobi.grill.driver.api;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.inmobi.grill.query.QueryCost;
import com.inmobi.grill.query.QueryHandle;
import com.inmobi.grill.query.QueryPlan;
import com.inmobi.grill.query.QueryPrepareHandle;

public abstract class DriverQueryPlan {

  public enum ExecMode {
    INTERACTIVE,
    BATCH,
    NOT_ACCEPTED
  };
  
  public enum ScanMode {
    IN_MEMORY,
    INDEX_SCAN,
    PARTIAL_SCAN,
    FULL_SCAN
  }

  protected int numJoins = 0;
  protected int numGbys = 0;
  protected int numSels = 0;
  protected int numSelDi = 0;
  protected int numHaving = 0;
  protected int numObys = 0;
  protected int numAggrExprs = 0;
  protected int numFilters = 0;
  protected final List<String> tablesQueried = new ArrayList<String>();
  protected boolean hasSubQuery = false;
  protected String resultDestination;
  protected ExecMode execMode;
  protected ScanMode scanMode;
  protected final Map<String, Double> tableWeights = new HashMap<String, Double>();
  protected Double joinWeight;
  protected Double gbyWeight;
  protected Double filterWeight;
  protected Double havingWeight;
  protected Double obyWeight;
  protected Double selectWeight;
  protected QueryPrepareHandle handle;

  /**
   * Get the query plan
   * 
   * @return The string representation of the plan
   */
  public abstract String getPlan();

  /**
   * Get the cost associated with the plan
   * 
   * @return QueryCost object
   */
  public abstract QueryCost getCost();

  /**
   * Get the number of group by expressions on query
   * 
   * @return the numGbys
   */
  public int getNumGbys() {
    return numGbys;
  }

  /**
   * Set the number of groupbys
   * 
   * @param numGbys the numGbys to set
   */
  protected void setNumGbys(int numGbys) {
    this.numGbys = numGbys;
  }

  /**
   * Get the number of select expressions
   * 
   * @return the numSels
   */
  public int getNumSels() {
    return numSels;
  }

  /**
   * Set the number of select expressions
   * 
   * @param numSels the numSels to set
   */
  protected void setNumSels(int numSels) {
    this.numSels = numSels;
  }

  /**
   * Get the number distinct select expressions
   * 
   * @return the numSelDi
   */
  public int getNumSelDistincts() {
    return numSelDi;
  }

  /**
   * Set the number of distinct select expressions
   * 
   * @param numSelDi the numSelDi to set
   */
  protected void setNumSelDistincts(int numSelDi) {
    this.numSelDi = numSelDi;
  }

  /**
   * Get number of joins in the query
   * 
   * @return the numJoins
   */
  public int getNumJoins() {
    return numJoins;
  }

  /**
   * Set the number of join expressions on query
   * 
   * @param numJoins the numJoins to set
   */
  protected void setNumJoins(int numJoins) {
    this.numJoins = numJoins;
  }

  /**
   * Get the number of having expressions on query
   * 
   * @return the numHaving
   */
  public int getNumHaving() {
    return numHaving;
  }

  /**
   * Set the number of having expressions on query
   * 
   * @param numHaving the numHaving to set
   */
  protected void setNumHaving(int numHaving) {
    this.numHaving = numHaving;
  }

  /**
   * Get the number of order by expressions on query
   * 
   * @return the numObys
   */
  public int getNumOrderBys() {
    return numObys;
  }

  /**
   * Set the number of order by expressions on query
   * 
   * @param numObys the numObys to set
   */
  protected void setNumOrderBys(int numObys) {
    this.numObys = numObys;
  }

  /**
   * Get the list of tables to be queried
   * 
   * @return the tablesQueried
   */
  public List<String> getTablesQueried() {
    return tablesQueried;
  }

  /**
   * Set the list of table names to be queried
   * 
   * @param tablesQueried the tablesQueried to set
   */
  protected void addTablesQueries(String table) {
    this.tablesQueried.add(table);
  }

  /**
   * Get the number of filters in query
   * 
   * @return the numFilters
   */
  public int getNumFilters() {
    return numFilters;
  }

  /**
   * Set the number of filters in query
   * 
   * @param numFilters the numFilters to set
   */
  protected void setNumFilters(int numFilters) {
    this.numFilters = numFilters;
  }

  /**
   * Get if the query has a subquery or not
   * 
   * @return the hasSubQuery true if query has subquery, false otherwise 
   */
  public boolean hasSubQuery() {
    return hasSubQuery;
  }

  /**
   * Set if query has subquery.
   */
  protected void setHasSubQuery() {
    this.hasSubQuery = true;
  }

  /**
   * Get the result destination
   * 
   * @return the resultDestination The destination can be another table or
   *           filesystem path or inmemory result
   */
  public String getResultDestination() {
    return resultDestination;
  }

  /**
   * Set string representation of the destination
   * 
   * @param resultDestination the resultDestination to set
   */
  protected void setResultDestination(String resultDestination) {
    this.resultDestination = resultDestination;
  }

  /**
   * Get the table weights
   * 
   * @return the tableWeights
   */
  public Map<String, Double> getTableWeights() {
    return tableWeights;
  }

  /**
   * Get the weight of the table.
   * 
   * @param tableName
   * 
   * @return the weight
   */
  public Double getTableWeight(String tableName) {
    return tableWeights.get(tableName);
  }

  /**
   * Set the weight of the table
   * 
   * @param tableName The name of the table.
   * 
   * @param tableWeight Weight of the table being queried. This should reflect
   *  the amount of data being read/scanned from the table, scan cost
   */
  protected void setTableWeight(String tableName, Double tableWeight) {
    this.tableWeights.put(tableName, tableWeight);
  }

  /**
   * Get the exec mode
   * 
   * @return the {@link ExecMode}
   */
  public ExecMode getExecMode() {
    return execMode;
  }

  /**
   * Set the exec mode
   * 
   * @param execMode the {@link ExecMode} to set
   */
  protected void setExecMode(ExecMode execMode) {
    this.execMode = execMode;
  }

  /**
   * Get the scan mode.
   * 
   * @return the {@link ScanMode}
   */
  public ScanMode getScanMode() {
    return scanMode;
  }

  /**
   * Set the scan mode
   * 
   * @param scanMode the {@link ScanMode} to set
   */
  protected void setScanMode(ScanMode scanMode) {
    this.scanMode = scanMode;
  }

  /**
   * Get the weight associated with joins
   * 
   * @return the joinWeight
   */
  public Double getJoinWeight() {
    return joinWeight;
  }

  /**
   * Set the weight associated with joins
   * 
   * @param joinWeight the joinWeight to set
   */
  protected void setJoinWeight(Double joinWeight) {
    this.joinWeight = joinWeight;
  }

  /**
   * Set the weight associated with group by expressions.
   * 
   * @return the gbyWeight
   */
  public Double getGbyWeight() {
    return gbyWeight;
  }

  /**
   * Set the weight associated with group by expressions.
   * 
   * @param gbyWeight the gbyWeight to set
   */
  protected void setGbyWeight(Double gbyWeight) {
    this.gbyWeight = gbyWeight;
  }

  /**
   * Set the weight associated with filter expressions.
   * 
   * @return the filterWeight
   */
  public Double getFilterWeight() {
    return filterWeight;
  }

  /**
   * Set the weight associated with filter expressions.
   * 
   * @param filterWeight the filterWeight to set
   */
  protected void setFilterWeight(Double filterWeight) {
    this.filterWeight = filterWeight;
  }

  /**
   * Get the weight associated with order by expressions.
   * 
   * @return the obyWeight
   */
  public Double getObyWeight() {
    return obyWeight;
  }

  /**
   * Set the weight associated with order by expressions.
   * 
   * @param obyWeight the obyWeight to set
   */
  protected void setObyWeight(Double obyWeight) {
    this.obyWeight = obyWeight;
  }

  /**
   * Set the weight associated with having expressions.
   * 
   * @return the havingWeight
   */
  public Double getHavingWeight() {
    return havingWeight;
  }

  /**
   * Set the weight associated with having expressions.
   * 
   * @param havingWeight the havingWeight to set
   */
  protected void setHavingWeight(Double havingWeight) {
    this.havingWeight = havingWeight;
  }

  /**
   * Get the weight associated with select expressions.
   * 
   * @return the selectWeight
   */
  public Double getSelectWeight() {
    return selectWeight;
  }

  /**
   * Set the weight associated with select expressions.
   * 
   * @param selectWeight the selectWeight to set
   */
  protected void setSelectWeight(Double selectWeight) {
    this.selectWeight = selectWeight;
  }

  /**
   * @deprecated
   * @return the handle
   */
  public QueryHandle getHandle() {
    if (handle != null) {
      return new QueryHandle(handle.getPrepareHandleId());
    } else {
      return null;
    }
  }

  /**
   * 
   * @return the prepare handle
   */
  public QueryPrepareHandle getPrepareHandle() {
    return handle;
  }

  /**
   * 
   * @param handle the handle to set
   */
  public void setPrepareHandle(QueryPrepareHandle handle) {
    this.handle = handle;
  }

  public int getNumAggreagateExprs() {
    return numAggrExprs;
  }

  public QueryPlan toQueryPlan() {
    return new QueryPlan(numJoins, numGbys, numSels, numSelDi,
        numHaving, numObys, numAggrExprs, numFilters, tablesQueried, hasSubQuery,
        execMode.name(), scanMode.name(), tableWeights, joinWeight, gbyWeight, filterWeight,
        havingWeight, obyWeight, selectWeight, null, getPlan(), getCost());
  }
}
