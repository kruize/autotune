package com.autotune.analyzer.recommendations.summary;

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