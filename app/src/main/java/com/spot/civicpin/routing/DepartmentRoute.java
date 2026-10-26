package com.spot.civicpin.routing;

public class DepartmentRoute {
    public String departmentName;
    public String localityName;
    public String category;
    public String toEmails;
    public String ccEmails;
    public String bccEmails;
    public String verificationStatus; // "VERIFIED_DIRECTORY", "UNVERIFIED_DEFAULT"
    public String notes;

    public DepartmentRoute(String departmentName, String localityName, String category, String toEmails, String ccEmails, String verificationStatus, String notes) {
        this.departmentName = departmentName;
        this.localityName = localityName;
        this.category = category;
        this.toEmails = toEmails != null ? toEmails : "";
        this.ccEmails = ccEmails != null ? ccEmails : "";
        this.bccEmails = "";
        this.verificationStatus = verificationStatus;
        this.notes = notes;
    }
}
