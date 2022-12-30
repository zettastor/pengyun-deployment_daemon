/*
 * Copyright (c) 2022. PengYunNetWork
 *
 * This program is free software: you can use, redistribute, and/or modify it
 * under the terms of the GNU Affero General Public License, version 3 or later ("AGPL"),
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 *  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 *  You should have received a copy of the GNU Affero General Public License along with
 *  this program. If not, see <http://www.gnu.org/licenses/>.
 */

package py.dd.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * xx.
 */
public class InfocenterDbTools {

  private static final Logger logger = LoggerFactory.getLogger(InfocenterDbTools.class);
  private String url;
  private boolean initialized = false;

  public InfocenterDbTools() {
  }

  /**
   * xx.
   */
  public void init() {
    // get URL of JDBC
    url = "jdbc:postgresql://localhost:5432/infocenterdb";
    initialized = true;
  }

  /**
   * xx.
   */
  public void clearInfoCenterDb() throws Exception {
    if (!initialized) {
      logger.error("Has not been initialized");
      throw new Exception();
    }

    Class.forName("org.postgresql.Driver").newInstance();
    Connection con = DriverManager.getConnection(url, "py", "312");
    Statement st = con.createStatement();
    try {
      st.execute("delete from access_rules;");
    } catch (SQLException e) {
      logger.error("", e);
    }
    try {
      st.execute("delete from archives;");
    } catch (SQLException e) {
      logger.error("", e);
    }
    try {
      st.execute("delete from domain_relationship;");
    } catch (SQLException e) {
      logger.error("", e);
    }
    try {
      st.execute("delete from drivers;");
    } catch (SQLException e) {
      logger.error("", e);
    }
    try {
      st.execute("delete from storages;");
    } catch (SQLException e) {
      logger.error("", e);
    }
    try {
      st.execute("delete from volumes;");
    } catch (SQLException e) {
      logger.error("", e);
    }
    try {
      st.execute("delete from vr_relationship;");
    } catch (SQLException e) {
      logger.error("", e);
    }
    try {
      st.execute("delete from storagepool_relationship;");
    } catch (SQLException e) {
      logger.error("", e);
    }
    st.execute("commit;");
    st.close();
    con.close();
  }
}
