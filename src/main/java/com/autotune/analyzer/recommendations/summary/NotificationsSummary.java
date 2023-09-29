/*******************************************************************************
 * Copyright (c) 2023 Red Hat, IBM Corporation and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/

package com.autotune.analyzer.recommendations.summary;

/**
 * stores the summary of all the notifications based on the summarization type
 */
public class NotificationsSummary {

  private int info;

  private int notice;

  private int warning;

  private int error;

  private int critical;

  public int getInfo() {
    return info;
  }

  public void setInfo(int info) {
    this.info = info;
  }

  public int getNotice() {
    return notice;
  }

  public void setNotice(int notice) {
    this.notice = notice;
  }

  public int getWarning() {
    return warning;
  }

  public void setWarning(int warning) {
    this.warning = warning;
  }

  public int getError() {
    return error;
  }

  public void setError(int error) {
    this.error = error;
  }

  public int getCritical() {
    return critical;
  }

  public void setCritical(int critical) {
    this.critical = critical;
  }

  public NotificationsSummary mergeNotificationsSummary(NotificationsSummary notifications1, NotificationsSummary notifications2) {

    int infoCount = notifications1.getInfo() + notifications2.getInfo();
    int noticeCount = notifications1.getNotice() + notifications2.getNotice();
    int warningCount = notifications1.getWarning() + notifications2.getWarning();
    int errorCount = notifications1.getError() + notifications2.getError();
    int criticalCount = notifications1.getCritical() + notifications2.getCritical();

    NotificationsSummary mergedSummary = new NotificationsSummary();
    mergedSummary.setInfo(infoCount);
    mergedSummary.setNotice(noticeCount);
    mergedSummary.setWarning(warningCount);
    mergedSummary.setError(errorCount);
    mergedSummary.setCritical(criticalCount);
    return mergedSummary;
  }
  @Override
  public String toString() {
    return "NotificationsSummary{" +
            "info=" + info +
            ", notice=" + notice +
            ", warning=" + warning +
            ", error=" + error +
            ", critical=" + critical +
            '}';
  }
}