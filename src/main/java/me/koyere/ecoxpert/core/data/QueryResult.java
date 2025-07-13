package me.koyere.ecoxpert.core.data;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

/**
 * Database query result wrapper
 * 
 * Provides type-safe access to query results with
 * proper null handling and data conversion.
 */
public interface QueryResult extends AutoCloseable {
    
    /**
     * Move to the next row in the result set
     * 
     * @return true if there is a next row
     */
    boolean next();
    
    /**
     * Get string value from current row
     * 
     * @param columnName Column name
     * @return String value or null
     */
    String getString(String columnName);
    
    /**
     * Get integer value from current row
     * 
     * @param columnName Column name
     * @return Integer value or null
     */
    Integer getInt(String columnName);
    
    /**
     * Get long value from current row
     * 
     * @param columnName Column name
     * @return Long value or null
     */
    Long getLong(String columnName);
    
    /**
     * Get double value from current row
     * 
     * @param columnName Column name
     * @return Double value or null
     */
    Double getDouble(String columnName);
    
    /**
     * Get BigDecimal value from current row
     * 
     * @param columnName Column name
     * @return BigDecimal value or null
     */
    BigDecimal getBigDecimal(String columnName);
    
    /**
     * Get boolean value from current row
     * 
     * @param columnName Column name
     * @return Boolean value or null
     */
    Boolean getBoolean(String columnName);
    
    /**
     * Get timestamp value from current row
     * 
     * @param columnName Column name
     * @return Timestamp value or null
     */
    Timestamp getTimestamp(String columnName);
    
    /**
     * Get optional string value from current row
     * 
     * @param columnName Column name
     * @return Optional string value
     */
    Optional<String> getOptionalString(String columnName);
    
    /**
     * Get optional integer value from current row
     * 
     * @param columnName Column name
     * @return Optional integer value
     */
    Optional<Integer> getOptionalInt(String columnName);
    
    /**
     * Check if the result set is empty
     * 
     * @return true if no rows returned
     */
    boolean isEmpty();
    
    /**
     * Get the number of rows in the result set
     * 
     * @return Row count
     */
    int getRowCount();
    
    /**
     * Get all column names in the result set
     * 
     * @return List of column names
     */
    List<String> getColumnNames();
    
    /**
     * Close the result set and free resources
     */
    @Override
    void close();
}