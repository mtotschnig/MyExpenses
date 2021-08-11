package org.totschnig.myexpenses.model;

import com.google.gson.Gson;

import java.math.BigDecimal;

public class TransactionDTO {

    String id;
    Boolean isSplit;
    String dateStr;
    String payee;
    BigDecimal amount;
    String labelMain;
    String labelSub;
    String fullLabel;
    String comment;
    String methodLabel;
    CrStatus status;
    String referenceNumber;
    String pictureFileName;
    String tagList;

    public TransactionDTO(){}

    public TransactionDTO(String id, Boolean isSplit, String dateStr, String payee, BigDecimal amount, String labelMain, String labelSub, String fullLabel, String comment, String methodLabel, CrStatus status, String referenceNumber, String pictureFileName, String tagList) {
        this.id = id;
        this.isSplit = isSplit;
        this.dateStr = dateStr;
        this.payee = payee;
        this.amount = amount;
        this.labelMain = labelMain;
        this.labelSub = labelSub;
        this.fullLabel = fullLabel;
        this.comment = comment;
        this.methodLabel = methodLabel;
        this.status = status;
        this.referenceNumber = referenceNumber;
        this.pictureFileName = pictureFileName;
        this.tagList = tagList;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Boolean getSplit() {
        return isSplit;
    }

    public void setSplit(Boolean split) {
        isSplit = split;
    }

    public String getDateStr() {
        return dateStr;
    }

    public void setDateStr(String dateStr) {
        this.dateStr = dateStr;
    }

    public String getPayee() {
        return payee;
    }

    public void setPayee(String payee) {
        this.payee = payee;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getLabelMain() {
        return labelMain;
    }

    public void setLabelMain(String labelMain) {
        this.labelMain = labelMain;
    }

    public String getLabelSub() {
        return labelSub;
    }

    public void setLabelSub(String labelSub) {
        this.labelSub = labelSub;
    }

    public String getFullLabel() {
        return fullLabel;
    }

    public void setFullLabel(String fullLabel) {
        this.fullLabel = fullLabel;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getMethodLabel() {
        return methodLabel;
    }

    public void setMethodLabel(String methodLabel) {
        this.methodLabel = methodLabel;
    }

    public CrStatus getStatus() {
        return status;
    }

    public void setStatus(CrStatus status) {
        this.status = status;
    }

    public String getReferenceNumber() {
        return referenceNumber;
    }

    public void setReferenceNumber(String referenceNumber) {
        this.referenceNumber = referenceNumber;
    }

    public String getPictureFileName() {
        return pictureFileName;
    }

    public void setPictureFileName(String pictureFileName) {
        this.pictureFileName = pictureFileName;
    }

    public String getTagList() {
        return tagList;
    }

    public void setTagList(String tagList) {
        this.tagList = tagList;
    }

    @Override
    public String toString(){
        return new Gson().toJson(this);
    }
}
