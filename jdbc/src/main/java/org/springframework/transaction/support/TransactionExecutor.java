package org.springframework.transaction.support;

import java.sql.Connection;
import java.sql.SQLException;
import javax.sql.DataSource;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.datasource.DataSourceUtils;

public class TransactionExecutor {

    private final DataSource dataSource;

    public TransactionExecutor(final DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public void execute(final TransactionWorker worker) {
        final Connection connection = getTransactionalConnection();

        try {
            connection.setAutoCommit(false);

            worker.run(connection);

            connection.commit();
        } catch (Exception e) {
            rollback(connection);
            throw new DataAccessException(e);
        } finally {
            DataSourceUtils.releaseConnection(connection, dataSource);
        }
    }

    private Connection getTransactionalConnection() {
        final Connection connection = DataSourceUtils.getConnection(dataSource);
        final ConnectionHolder connectionHolder = new ConnectionHolder(connection);
        connectionHolder.setTransactionActive(true);
        TransactionSynchronizationManager.bindResource(dataSource, connectionHolder);

        return connection;
    }

    private void rollback(final Connection connection) {
        if (connection == null) {
            return;
        }

        try {
            connection.rollback();
        } catch (SQLException ex) {
            throw new DataAccessException(ex);
        }
    }
}
