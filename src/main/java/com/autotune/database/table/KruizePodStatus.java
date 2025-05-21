package com.autotune.database.table;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.sql.Timestamp;

@Entity
@Table(
   name = "kruize_pod_status"
)
public class KruizePodStatus {
   @Id
   @Column(
      name = "pod_name"
   )
   private String podName;
   @Column(
      name = "status"
   )
   private String status;
   @Column(
      name = "reason"
   )
   private String reason;
   @Column(
      name = "updated_at"
   )
   private Timestamp updatedAt;
   @Column(
      name = "last_checked"
   )
   private Timestamp lastChecked;

   public String getPodName() {
      return this.podName;
   }

   public void setPodName(String podName) {
      this.podName = podName;
   }

   public String getStatus() {
      return this.status;
   }

   public void setStatus(String status) {
      this.status = status;
   }

   public String getReason() {
      return this.reason;
   }

   public void setReason(String reason) {
      this.reason = reason;
   }

   public Timestamp getUpdatedAt() {
      return this.updatedAt;
   }

   public void setUpdatedAt(Timestamp updatedAt) {
      this.updatedAt = updatedAt;
   }

   public Timestamp getLastChecked() {
      return this.lastChecked;
   }

   public void setLastChecked(Timestamp lastChecked) {
      this.lastChecked = lastChecked;
   }
}
