/**
* Copyright (C) 2013-2024 Nanjing Pengyun Network Technology Co., Ltd.
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*     http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
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
