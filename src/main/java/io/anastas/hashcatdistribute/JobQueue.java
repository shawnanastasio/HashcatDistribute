package io.anastas.hashcatdistribute;

import java.sql.*;

public class JobQueue {
	/**
	 * Get the id of the current job the server is working on
	 * @return id of the current job or 0 if none
	 * @throws SQLException if a database error occurs
	 */
	public static int getCurrentJobID() throws SQLException {
		PreparedStatement pst = App.dbcon.prepareStatement("SELECT jobID FROM jobs WHERE DONE IS NULL LIMIT 1");
		ResultSet rs = pst.executeQuery();
		if (!rs.next()) return 0;
		return rs.getInt(1);
	}
}
