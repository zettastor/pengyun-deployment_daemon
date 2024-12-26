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
