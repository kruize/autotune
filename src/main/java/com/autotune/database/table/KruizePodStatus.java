import javax.persistence.*;
import java.sql.Timestamp;

@Entity
@Table(name = "pod_status")
public class PodStatus {

    @Id
    @Column(name = "pod_name")
    private String podName;

    @Column(name = "status")
    private String status;

    @Column(name = "reason")
    private String reason;

    @Column(name = "updated_at")
    private Timestamp updatedAt;

    // ➕ New field to track last time this pod was validated/checked
    @Column(name = "last_checked")
    private Timestamp lastChecked;

    // Getters and Setters

    public String getPodName() {
        return podName;
    }

    public void setPodName(String podName) {
        this.podName = podName;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public Timestamp getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Timestamp updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Timestamp getLastChecked() {
        return lastChecked;
    }

    public void setLastChecked(Timestamp lastChecked) {
        this.lastChecked = lastChecked;
    }
}
