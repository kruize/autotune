package com.autotune.analyzer.exceptions;

public class MetadataProfileResponse {
    private String message;
    private int httpcode;
    private String documentationLink;
    private String status;

    public MetadataProfileResponse(String message, int httpcode, String documentationLink, String status) {
        this.message = message;
        this.httpcode = httpcode;
        this.documentationLink = documentationLink;
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public int getHttpcode() {
        return httpcode;
    }

    public void setHttpcode(int httpcode) {
        this.httpcode = httpcode;
    }

    public String getDocumentationLink() {
        return documentationLink;
    }

    public void setDocumentationLink(String documentationLink) {
        this.documentationLink = documentationLink;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
