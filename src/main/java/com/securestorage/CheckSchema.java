package com.securestorage;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.DescribeTableRequest;
import software.amazon.awssdk.services.dynamodb.model.DescribeTableResponse;
import software.amazon.awssdk.services.dynamodb.model.KeySchemaElement;

import java.io.FileInputStream;
import java.util.Properties;

public class CheckSchema {
    public static void main(String[] args) throws Exception {
        Properties props = new Properties();
        props.load(new FileInputStream("src/main/resources/application-secret.properties"));
        props.load(new FileInputStream("src/main/resources/application.properties"));
        
        String region = props.getProperty("aws.region");
        String accessKey = props.getProperty("aws.accessKeyId");
        String secretKey = props.getProperty("aws.secretKey");
        String tableName = props.getProperty("aws.dynamodb.table");
        
        DynamoDbClient ddb = DynamoDbClient.builder()
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKey, secretKey)))
                .build();
                
        DescribeTableResponse response = ddb.describeTable(DescribeTableRequest.builder()
                .tableName(tableName)
                .build());
                
        for (KeySchemaElement element : response.table().keySchema()) {
            System.out.println("KEY: " + element.attributeName() + " (" + element.keyTypeAsString() + ")");
        }
        
        System.exit(0);
    }
}
