package me.koyere.ecoxpert.core.data;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Implementation of QueryResult for database query results
 * 
 * Provides type-safe access to ResultSet data with proper
 * null handling and resource management.
 */
public class QueryResultImpl implements QueryResult {
    
    private final ResultSet resultSet;
    private final ResultSetMetaData metaData;
    private final List<String> columnNames;
    private boolean closed = false;
    private int rowCount = -1;
    
    public QueryResultImpl(ResultSet resultSet) throws SQLException {
        this.resultSet = resultSet;
        this.metaData = resultSet.getMetaData();
        this.columnNames = buildColumnNames();
    }
    
    @Override
    public boolean next() {
        try {
            checkClosed();
            return resultSet.next();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to move to next row", e);
        }
    }
    
    @Override
    public String getString(String columnName) {
        try {
            checkClosed();
            return resultSet.getString(columnName);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to get string value for column: " + columnName, e);
        }
    }
    
    @Override
    public Integer getInt(String columnName) {
        try {
            checkClosed();
            int value = resultSet.getInt(columnName);
            return resultSet.wasNull() ? null : value;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to get int value for column: " + columnName, e);
        }
    }
    
    @Override
    public Long getLong(String columnName) {
        try {
            checkClosed();
            long value = resultSet.getLong(columnName);
            return resultSet.wasNull() ? null : value;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to get long value for column: " + columnName, e);
        }
    }
    
    @Override
    public Double getDouble(String columnName) {
        try {
            checkClosed();
            double value = resultSet.getDouble(columnName);
            return resultSet.wasNull() ? null : value;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to get double value for column: " + columnName, e);
        }
    }
    
    @Override
    public BigDecimal getBigDecimal(String columnName) {
        try {
            checkClosed();
            return resultSet.getBigDecimal(columnName);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to get BigDecimal value for column: " + columnName, e);
        }
    }
    
    @Override
    public Boolean getBoolean(String columnName) {
        try {
            checkClosed();
            boolean value = resultSet.getBoolean(columnName);
            return resultSet.wasNull() ? null : value;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to get boolean value for column: " + columnName, e);
        }
    }
    
    @Override
    public Timestamp getTimestamp(String columnName) {
        try {
            checkClosed();
            return resultSet.getTimestamp(columnName);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to get timestamp value for column: " + columnName, e);
        }
    }
    
    @Override
    public Optional<String> getOptionalString(String columnName) {
        return Optional.ofNullable(getString(columnName));
    }
    
    @Override
    public Optional<Integer> getOptionalInt(String columnName) {
        return Optional.ofNullable(getInt(columnName));
    }
    
    @Override
    public boolean isEmpty() {
        try {
            checkClosed();
            return !resultSet.isBeforeFirst() && !resultSet.isAfterLast() && resultSet.getRow() == 0;
        } catch (SQLException e) {
            return true;
        }
    }
    
    @Override
    public int getRowCount() {
        if (rowCount == -1) {
            try {
                checkClosed();
                int currentRow = resultSet.getRow();
                resultSet.last();
                rowCount = resultSet.getRow();
                
                // Restore position
                if (currentRow == 0) {
                    resultSet.beforeFirst();
                } else {
                    resultSet.absolute(currentRow);
                }
            } catch (SQLException e) {
                rowCount = 0;
            }
        }
        return rowCount;
    }
    
    @Override
    public List<String> getColumnNames() {
        return new ArrayList<>(columnNames);
    }
    
    @Override
    public void close() {
        if (!closed) {
            try {
                resultSet.close();
            } catch (SQLException e) {
                // Log warning but don't throw
                System.err.println("Warning: Failed to close ResultSet: " + e.getMessage());
            } finally {
                closed = true;
            }
        }
    }
    
    /**
     * Build list of column names from metadata
     * 
     * @return List of column names
     * @throws SQLException if metadata access fails
     */
    private List<String> buildColumnNames() throws SQLException {
        List<String> names = new ArrayList<>();
        int columnCount = metaData.getColumnCount();
        
        for (int i = 1; i <= columnCount; i++) {
            names.add(metaData.getColumnName(i));
        }
        
        return names;
    }
    
    /**
     * Check if the result set is closed
     * 
     * @throws IllegalStateException if closed
     */
    private void checkClosed() {
        if (closed) {
            throw new IllegalStateException("QueryResult has been closed");
        }
    }
}