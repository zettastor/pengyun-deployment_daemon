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

package py.dd.license;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.util.Date;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * backup the license decrypt key into a file.
 */
public class LicenseKeyBackup {

  @JsonIgnore
  private static final Logger logger = LoggerFactory.getLogger(LicenseKeyBackup.class);
  @JsonIgnore
  private static File file = new File("/tmp/licensekeybackup");
  private String key;
  private String uuid;
  private Date backupTime;

  /**
   * xx.
   */
  @JsonIgnore
  public boolean save() {
    ObjectMapper objectMapper = new ObjectMapper();
    try {
      objectMapper.writeValue(file, this);
      return true;
    } catch (Exception e) {
      logger.error("Caught an exception", e);
      return false;
    }
  }

  /**
   * xx.
   */
  @JsonIgnore
  public void load() {
    ObjectMapper objectMapper = new ObjectMapper();
    try {
      LicenseKeyBackup tmp = objectMapper.readValue(file, LicenseKeyBackup.class);
      this.setKey(tmp.getKey());
      this.setUuid(tmp.getUuid());
      this.setBackupTime(tmp.getBackupTime());
    } catch (Exception e) {
      logger.error("Caught an exception", e);
    }
  }

  public String getKey() {
    return key;
  }

  public void setKey(String key) {
    this.key = key;
  }

  public String getUuid() {
    return uuid;
  }

  public void setUuid(String uuid) {
    this.uuid = uuid;
  }

  public Date getBackupTime() {
    return backupTime;
  }

  public void setBackupTime(Date backupTime) {
    this.backupTime = backupTime;
  }
}
