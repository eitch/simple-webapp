package ch.eitchnet.simple.webapp;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.postgresql.Driver;

public class DbPool {

	private static final DbPool instance;

	static {
		instance = new DbPool();
	}

	private DataSource dataSource;

	public static DbPool getInstance() {
		return instance;
	}

	public void configure() {

		try {
			if (!Driver.isRegistered()) {
				Driver.register();
			}
		} catch (SQLException e) {
			throw new IllegalStateException("Failed to load PostgreSQL Driver", e);
		}

		HikariConfig config = new HikariConfig();
		config.setAutoCommit(false);
		config.setPoolName("simple");
		config.setJdbcUrl("jdbc:postgresql://localhost/simple");
		config.setUsername("simple");
		config.setPassword("simple");

		this.dataSource = new HikariDataSource(config);
	}

	public Connection getConnection() throws SQLException {
		return this.dataSource.getConnection();
	}
}
