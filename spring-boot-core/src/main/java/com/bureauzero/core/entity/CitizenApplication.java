package com.bureauzero.core.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "citizen_applications")
public class CitizenApplication {
    @Id
    private String receiptId;
    @Column(nullable = false)
    private String fullName;
    private Integer age;
    private String gender;
    private String domicileState;
    private String socialCategory;
    private Long annualIncome;
    private String schemeName;
    private String citizenIdNumber;
    private String dobFormatted;
    @Column(nullable = false)
    private String status;
    @Column(nullable = false, unique = true)
    private String cryptographicHash;
    private LocalDateTime submittedAt;

    public CitizenApplication() {
    }

    public String getReceiptId() { return receiptId; }
    public void setReceiptId(String receiptId) { this.receiptId = receiptId; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public Integer getAge() { return age; }
    public void setAge(Integer age) { this.age = age; }
    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }
    public String getDomicileState() { return domicileState; }
    public void setDomicileState(String domicileState) { this.domicileState = domicileState; }
    public String getSocialCategory() { return socialCategory; }
    public void setSocialCategory(String socialCategory) { this.socialCategory = socialCategory; }
    public Long getAnnualIncome() { return annualIncome; }
    public void setAnnualIncome(Long annualIncome) { this.annualIncome = annualIncome; }
    public String getSchemeName() { return schemeName; }
    public void setSchemeName(String schemeName) { this.schemeName = schemeName; }
    public String getCitizenIdNumber() { return citizenIdNumber; }
    public void setCitizenIdNumber(String citizenIdNumber) { this.citizenIdNumber = citizenIdNumber; }
    public String getDobFormatted() { return dobFormatted; }
    public void setDobFormatted(String dobFormatted) { this.dobFormatted = dobFormatted; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getCryptographicHash() { return cryptographicHash; }
    public void setCryptographicHash(String cryptographicHash) { this.cryptographicHash = cryptographicHash; }
    public LocalDateTime getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(LocalDateTime submittedAt) { this.submittedAt = submittedAt; }
}
