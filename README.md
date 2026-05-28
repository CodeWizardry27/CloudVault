# 🔐 Cloud Vault - Project Documentation

## 📋 Project Overview

**Cloud Vault** is a full-stack web application that provides secure file storage with end-to-end encryption. The application leverages AWS cloud services to deliver enterprise-grade security while maintaining user privacy through hybrid envelope encryption.

### Project Type
- **Category**: Cloud-Based Secure File Storage Platform
- **Architecture Pattern**: Monolithic Spring Boot Application with Cloud Services Integration
- **Deployment Model**: Cloud-Native Application (AWS)

### Technology Stack

#### Backend
- **Framework**: Spring Boot 3.2.2
- **Language**: Java 17
- **Build Tool**: Maven
- **Key Dependencies**:
- Spring Web (REST API)
- Spring Security OAuth2 Resource Server
- AWS SDK for Java v2 (S3, DynamoDB, Cognito)
- Lombok (Boilerplate reduction)
=======
  - Spring Web (REST API)
  - Spring Security OAuth2 Resource Server
  - AWS SDK for Java v2 (S3, DynamoDB, Cognito)
  - Lombok (Boilerplate reduction)


#### Frontend
- **Core**: HTML5, JavaScript (Vanilla)
- **UI Framework**: Bootstrap 5.3.0
- **Icons**: Bootstrap Icons 1.11.0
- **Image Processing**: Cropper.js 1.5.13

#### Cloud Services (AWS)
- **Storage**: Amazon S3 (Simple Storage Service)
- **Database**: Amazon DynamoDB (NoSQL)
- **Authentication**: Amazon Cognito (User Pool)
  
- **Encryption**: Local Master Key (KMS not used in the current build)
- **Compute**: Configurable (EC2, Elastic Beanstalk, ECS, etc.)
=======
- **Encryption**: Local Master Key
- **Compute**: Configurable (EC2, S3, Cognito, etc.)


---

## 🏗️ System Architecture

### High-Level Architecture

```
┌─────────────┐         ┌──────────────────┐         ┌─────────────────┐
│   Browser   │◄───────►│  Spring Boot     │◄───────►│   AWS Cloud     │
│  (Client)   │  HTTPS  │   Application    │  SDK    │                 │
└─────────────┘         │  (Backend API)   │         │  ┌───────────┐  │
                        └──────────────────┘         │  │ S3 Bucket │  │
                                                     │  └───────────┘  │
                                                     │  ┌───────────┐  │
                                                     │  │ DynamoDB  │  │
                                                     │  └───────────┘  │
                                                     │  ┌───────────┐  │
                                                     │  │  Cognito  │  │
                                                     │  └───────────┘  │
                                                     └─────────────────┘
```

> Note: AWS KMS is not used in the current build; encryption uses a local master key only.

### Component Architecture

#### 1. **Presentation Layer** (Frontend)
- **Static HTML Page** (`index.html`)
- **Responsive UI** with Bootstrap
- **JavaScript Client** for API communication
- **Features**:
  - User authentication (Cognito integration)
  - File upload/download interface
  - Profile management with image cropping
  - Storage quota visualization
  - File type categorization

#### 2. **Application Layer** (Spring Boot)

**Controllers**:
- `FileController`: File upload/download operations
- `AccountController`: User account management
- `ProfileController`: Profile picture handling

**Services**:
- `FileService`: Core file operations (encrypt, upload, download, delete)
- `EncryptionService`: Cryptographic operations (AES-256-GCM)

**Configuration**:
- `SecurityConfig`: CORS, CSRF, OAuth2 JWT validation
- `AwsConfig`: AWS SDK client initialization (S3, DynamoDB, Cognito)

**Entities**:
- `FileEntity`: DynamoDB table mapping for file metadata

#### 3. **Data Layer**
- **DynamoDB Table** (`Users` table)
  - Stores file metadata
  - User information
  - Encrypted file keys
  - Initialization Vectors (IVs)

#### 4. **Storage Layer**
- **S3 Bucket** (`cloud-s3-demoproject`)
  - Stores encrypted file content
  - Files are encrypted before upload
  - Named with UUID for uniqueness

---

## 🔒 Security Architecture

### Hybrid Envelope Encryption Model

The application implements a **multi-layered encryption strategy** to ensure zero-knowledge storage:

#### Encryption Workflow (Upload)

```
1. File Selection (Client)
        ↓
2. File Transmitted to Backend
        ↓
3. Generate Random AES-256 Key (unique per file)
        ↓
4. Generate Random IV (12 bytes)
        ↓
5. Encrypt File Content with AES-256-GCM
        ↓
6. Encrypt AES Key with Master Key (local key only; KMS not configured)
        ↓
7. Upload Encrypted File to S3
        ↓
8. Store Metadata in DynamoDB:
   - fileId (UUID)
   - fileName (original)
   - encryptedKey (base64)
   - iv (base64)
   - fileSize
   - contentType
   - ownerId
   - uploadDate
```

#### Decryption Workflow (Download)

```
1. User Requests File
        ↓
2. Fetch Metadata from DynamoDB
        ↓
3. Decrypt File Key using Master Key (local key only; KMS not configured)
        ↓
4. Download Encrypted File from S3
        ↓
5. Decrypt File Content using AES Key + IV
        ↓
6. Stream Decrypted File to User
```

### Security Features

✅ **End-to-End Encryption**: Files encrypted before cloud upload  
✅ **Key Wrapping**: File keys protected by local Master Key (KMS integration pending)  
✅ **Per-File Keys**: Unique AES key for each file  
✅ **Authentication**: AWS Cognito JWT tokens  
✅ **Authorization**: OAuth2 Resource Server  
✅ **CORS Protection**: Configured origins  
✅ **Secure Transmission**: HTTPS enforced  
✅ **Storage Limits**: 200 MB per user quota  

---

## 📂 Project Structure

```
SecureCloudStorage/
│
├── pom.xml                          # Maven dependencies & build config
├── README.md                        # Project documentation
├── Project.md                       # This file
├── cloudsetup.md                    # AWS setup guide
│
├── src/
│   ├── main/
│   │   ├── java/com/securestorage/
│   │   │   ├── SecureStorageApplication.java    # Main entry point
│   │   │   ├── SecurityConfig.java              # Spring Security config
│   │   │   ├── AwsConfig.java                   # AWS clients configuration
│   │   │   ├── EncryptionService.java           # Encryption/decryption logic
│   │   │   ├── FileService.java                 # File operations service
│   │   │   ├── FileController.java              # REST API for files
│   │   │   ├── AccountController.java           # Account management API
│   │   │   ├── ProfileController.java           # Profile picture API
│   │   │   └── FileEntity.java                  # DynamoDB entity mapping
│   │   │
│   │   └── resources/
│   │       ├── application.properties           # Application configuration
│   │       └── static/
│   │           └── index.html                   # Single-page application
│   │
│   └── test/                                    # Test files (if any)
│
└── target/                                      # Compiled artifacts
    ├── secure-cloud-storage-1.0.0.jar          # Executable JAR
    └── classes/                                 # Compiled .class files
```

---

## 🔄 Application Workflow

### 1. **User Registration & Login**
1. User registers via Cognito UI (hosted or custom)
2. Email verification (if enabled)
3. User logs in with credentials
4. Cognito returns JWT access token
5. Frontend stores token for API requests

### 2. **File Upload Process**
1. User selects file via web interface
2. File size validated (max 10 MB per file)
3. Storage quota checked (200 MB total)
4. File sent to `/api/files/upload` endpoint
5. Backend generates AES key and IV
6. File content encrypted with AES-256-GCM
7. AES key encrypted with Master Key (local key only; KMS disabled)
8. Encrypted file uploaded to S3 (UUID filename)
9. Metadata saved to DynamoDB
10. Success response returned

### 3. **File Listing**
1. User requests `/api/files/list`
2. JWT token validated
3. Query DynamoDB for user's files
4. Return list with metadata (name, size, date, type)
5. Frontend categorizes by file type (images, videos, documents)

### 4. **File Download**
1. User clicks download button
2. Request sent to `/api/files/{fileId}/download`
3. Metadata fetched from DynamoDB
4. Encrypted AES key decrypted
5. Encrypted file downloaded from S3
6. File decrypted using AES key + IV
7. Decrypted content streamed to browser
8. Browser saves file with original name

### 5. **File Deletion**
1. User confirms deletion
2. Request to `/api/files/{fileId}/delete`
3. File removed from S3
4. Metadata removed from DynamoDB
5. Storage quota updated

### 6. **Account Management**
1. User views storage usage via `/api/account/usage`
2. DynamoDB aggregates file sizes
3. Storage meter displayed (used/total)

### 7. **Profile Picture**
1. User uploads image
2. Client-side cropping with Cropper.js
3. Cropped image sent to `/api/profile/upload`
4. Stored in S3 (separate from encrypted files)
5. Profile pic URL stored in DynamoDB

---

## 🎯 Key Features

### Functional Features
- ✅ **Secure File Upload** with client-side progress tracking
- ✅ **Encrypted Storage** using industry-standard AES-256
- ✅ **File Management** (list, download, delete)
- ✅ **Storage Quota** enforcement (200 MB per user)
- ✅ **File Type Recognition** (images, videos, documents, etc.)
- ✅ **Profile Customization** with image cropping
- ✅ **User Authentication** via AWS Cognito

### Non-Functional Features
- ✅ **Scalability**: Cloud-native design
- ✅ **Security**: Multi-layer encryption
- ✅ **Reliability**: AWS managed services
- ✅ **Performance**: Efficient streaming for large files
- ✅ **Usability**: Clean, responsive UI
- ✅ **Maintainability**: Modular architecture

---

## 🛠️ Configuration

### Configuration
Located in `src/main/resources/application.properties`:

**Key Properties**:
- Server port: `5000`
- AWS region, S3 bucket, DynamoDB table name
- Cognito issuer URI and user pool ID
- AWS credentials (via environment variables)
- Master encryption key (base64-encoded, via environment variable; local key is the only mode currently)

For detailed configuration steps, refer to [cloudsetup.md](cloudsetup.md)

---

## 🚀 Getting Started

**Local Development**:
```bash
git clone <repo-url>
cd SecureCloudStorage
mvn clean package
java -jar target/secure-cloud-storage-1.0.0.jar
# Access at http://localhost:5000
```

**Deployment**: See [cloudsetup.md](cloudsetup.md) for complete AWS setup and deployment

---

## 📊 Database Schema

### DynamoDB Table: `Users`

**Primary Key**: `fileId` (String)

| Attribute | Type | Description |
|-----------|------|-------------|
| `fileId` | String (PK) | UUID of the file |
| `fileName` | String | Original filename |
| `fileSize` | Number | Size in bytes |
| `contentType` | String | MIME type |
| `ownerId` | String | Cognito user ID |
| `uploadDate` | String | ISO 8601 timestamp |
| `encryptedKey` | String | Base64 encrypted AES key |
| `iv` | String | Base64 initialization vector |
| `s3Key` | String | S3 object key |

---

## 🐛 Common Issues

- **Access Denied (S3)**: Verify IAM permissions include S3 and DynamoDB full access
- **Invalid Token**: Check Cognito issuer URI matches user pool configuration
- **Decryption Errors**: Ensure master key is correctly base64-encoded
- **CORS Errors**: Verify CORS configuration in `SecurityConfig.java` allows your domain

---

## 📚 Documentation

- **[cloudsetup.md](cloudsetup.md)**: Complete AWS setup and deployment guide
- **Application Logs**: Check logs for runtime errors and debugging

---

## 📈 Future Enhancements

- [ ] File sharing with expiring links
- [ ] Folder organization
- [ ] File versioning
- [ ] Multi-file upload
- [ ] File preview (images, PDFs)
- [ ] Two-factor authentication
