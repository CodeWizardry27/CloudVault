package com.securestorage;

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;

@DynamoDbBean
public class FileEntity {
    private String fileId;
    private String ownerId;
    private String filename;
    private String s3Key;
    private String encryptedAesKey;
    private String iv;
    private String contentType;
    private Long fileSize;

    public FileEntity() {
    }

    public FileEntity(String fileId, String ownerId, String filename, String s3Key, String encryptedAesKey, String iv, String contentType, Long fileSize) {
        this.fileId = fileId;
        this.ownerId = ownerId;
        this.filename = filename;
        this.s3Key = s3Key;
        this.encryptedAesKey = encryptedAesKey;
        this.iv = iv;
        this.contentType = contentType;
        this.fileSize = fileSize;
    }

    @DynamoDbPartitionKey
    public String getFileId() { return fileId; }
    public void setFileId(String fileId) { this.fileId = fileId; }

    public String getOwnerId() { return ownerId; }
    public void setOwnerId(String ownerId) { this.ownerId = ownerId; }

    public String getFilename() { return filename; }
    public void setFilename(String filename) { this.filename = filename; }

    public String getS3Key() { return s3Key; }
    public void setS3Key(String s3Key) { this.s3Key = s3Key; }

    public String getEncryptedAesKey() { return encryptedAesKey; }
    public void setEncryptedAesKey(String encryptedAesKey) { this.encryptedAesKey = encryptedAesKey; }

    public String getIv() { return iv; }
    public void setIv(String iv) { this.iv = iv; }

    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }

    public Long getFileSize() { return fileSize; }
    public void setFileSize(Long fileSize) { this.fileSize = fileSize; }
}