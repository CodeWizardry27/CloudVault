# ☁️ AWS Cloud Setup & Deployment Guide

Complete setup and deployment guide for Secure Cloud Storage application on AWS.

---

## 📋 Prerequisites

- AWS account
- Domain name with SSL certificate files (.crt and .key)
- SSH client
- Maven installed locally

---

## 🎯 Deployment Workflow

```
1. IAM User → 2. S3 Bucket → 3. DynamoDB Table → 
4. Cognito → 5. Master Key → 6. EC2 Deployment
```

---

## 🔐 Step 1: IAM User Setup

### Create IAM User

1. Navigate to [IAM Console](https://console.aws.amazon.com/iam/)
2. Click **Users** → **Add users**
3. Configure:
   - User name: `secure-storage-app`
   - Access type: ☑️ Programmatic access
4. Attach policies:
   - ☑️ `AmazonS3FullAccess`
   - ☑️ `AmazonDynamoDBFullAccess`
5. **Save credentials** (Access Key ID and Secret Key)

✅ **Checkpoint**: Access Key ID and Secret Key saved

---

## 🪣 Step 2: Create S3 Bucket

1. Navigate to [S3 Console](https://s3.console.aws.amazon.com/s3/)
2. Click **Create bucket**
3. Configure:
   - Bucket name: Choose unique name (e.g., `my-secure-storage-files-2026`)
   - Region: Select your region (e.g., `ap-south-1`)
   - Block Public Access: ☑️ Keep all boxes checked
   - Bucket Versioning: Optional
   - Tags (optional): `Project`: `SecureCloudStorage`

✅ **Checkpoint**: S3 Bucket Name and AWS Region saved

---

## 🗄️ Step 3: Create DynamoDB Table

1. Navigate to [DynamoDB Console](https://console.aws.amazon.com/dynamodb/)
2. Click **Create table**
3. Configure:
   - Table name: `Users` (exact name required)
   - Partition key: `fileId` (Type: **String**)
   - Capacity mode: **On-demand** (recommended) or Provisioned (5 read/write units)
   - Encryption: AWS owned key (default)

✅ **Checkpoint**: DynamoDB Table `Users` with partition key `fileId`

---

## 👤 Step 4: Setup Amazon Cognito

1. Navigate to [Cognito Console](https://console.aws.amazon.com/cognito/)
2. Click **Create user pool**
3. Configure sign-in: ☑️ **Email**
4. Set password policy and recovery (Email only)
5. Enable self-registration with email verification
6. Required attributes: `name`, `email`
7. Email provider: **Send email with Cognito**
8. User pool name: `secure-storage-users`
9. Hosted UI: ☑️ Use Cognito Hosted UI
10. Domain prefix: Choose unique name (e.g., `my-secure-storage`)
11. App client settings:
    - Name: `secure-storage-web-client`
    - Authentication flows: `ALLOW_USER_PASSWORD_AUTH`, `ALLOW_REFRESH_TOKEN_AUTH`
    - OAuth grant types: Implicit grant
    - OAuth scopes: `openid`, `email`, `profile`
    - Callback URLs: `http://localhost:5000`, `https://your-domain.com`
12. Create user pool

### Save Configuration Values

- User Pool ID: `ap-south-1_XXXXXXXXX`
- App Client ID: 26-character string
- Issuer URI: `https://cognito-idp.{region}.amazonaws.com/{userPoolId}`
- Domain: `https://your-prefix.auth.region.amazoncognito.com`

✅ **Checkpoint**: Cognito User Pool ID, App Client ID, Issuer URI

---

## 🔑 Step 5: Generate Master Encryption Key

Generate a 256-bit AES key (base64 encoded):

**OpenSSL** (Linux/Mac/WSL):
```bash
openssl rand -base64 32
```

**PowerShell** (Windows):
```powershell
[Convert]::ToBase64String((1..32 | ForEach-Object { Get-Random -Minimum 0 -Maximum 256 }))
```

**Python**:
```bash
python3 -c "import os, base64; print(base64.b64encode(os.urandom(32)).decode())"
```

**Java**:
```java
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.util.Base64;

public class KeyGen {
    public static void main(String[] args) throws Exception {
        KeyGenerator keyGen = KeyGenerator.getInstance("AES");
        keyGen.init(256);
        SecretKey key = keyGen.generateKey();
        System.out.println(Base64.getEncoder().encodeToString(key.getEncoded()));
    }
}
```

Compile and run:
```bash
javac KeyGen.java
java KeyGen
```

### 5.3 📝 Save Your Master Key

```
Master Key: K7x2R9pQ3mN8vL5wY4jH6gF1dS0aZ9cX2bV8nM4kT7q=
```

⚠️ **Save this key securely** - required for deployment

✅ **Checkpoint**: Base64 master key saved

---

## 🖥️ Step 6: EC2 Deployment

### 6.1 Launch Instance

1. Navigate to [EC2 Console](https://console.aws.amazon.com/ec2/)
2. Click **Launch Instance**
3. Configure:
   - Name: `Secure-Storage-Server`
   - AMI: Ubuntu Server 22.04 LTS (64-bit x86)
   - Instance type: **t2.micro** (Free Tier: 1 vCPU, 1GB RAM)
   - Key pair: Create/select RSA .pem file
   - Security group rules:
     - SSH (22), HTTP (80), HTTPS (443)
   - Storage: 8-20 GB gp3
4. Launch and note Public IPv4 address
5. Point domain's A record to EC2 IP

### 6.2 Connect to Instance

**Windows**:
```powershell
ssh -i "your-key.pem" ubuntu@YOUR_EC2_IP
```

**Linux/Mac**:
```bash
chmod 400 your-key.pem
ssh -i "your-key.pem" ubuntu@YOUR_EC2_IP
```

### 6.3 Install Software & Optimize

```bash
# Update system
sudo apt update && sudo apt upgrade -y

# Install Java 17
sudo apt install openjdk-17-jdk -y
java -version

# Install Nginx
sudo apt install nginx -y
sudo systemctl start nginx
sudo systemctl enable nginx

# Enable 2GB swap (critical for t2.micro)
sudo fallocate -l 2G /swapfile
sudo chmod 600 /swapfile
sudo mkswap /swapfile
sudo swapon /swapfile
echo '/swapfile none swap sw 0 0' | sudo tee -a /etc/fstab
sudo sysctl vm.swappiness=10
echo 'vm.swappiness=10' | sudo tee -a /etc/sysctl.conf

# Create temp directory
sudo mkdir -p /tmp/tomcat-upload
sudo chmod 777 /tmp/tomcat-upload
```

### 6.4 Upload SSL Certificates

From local machine:
```bash
scp -i "your-key.pem" your-domain.crt ubuntu@YOUR_EC2_IP:/home/ubuntu/
scp -i "your-key.pem" your-domain.key ubuntu@YOUR_EC2_IP:/home/ubuntu/
```

On EC2:
```bash
sudo mkdir -p /etc/nginx/ssl
sudo mv ~/your-domain.crt /etc/nginx/ssl/
sudo mv ~/your-domain.key /etc/nginx/ssl/
sudo chmod 600 /etc/nginx/ssl/your-domain.key
sudo chmod 644 /etc/nginx/ssl/your-domain.crt

```

### 6.5 Configure Nginx

```bash
sudo nano /etc/nginx/sites-available/default
```

Replace content with (update `your-domain.com` and cert filenames):

```nginx
server {
    listen 80;
    server_name your-domain.com www.your-domain.com;
    return 301 https://$host$request_uri;
}

server {
    listen 443 ssl;
    server_name your-domain.com www.your-domain.com;

    ssl_certificate /etc/nginx/ssl/your-domain.crt;
    ssl_certificate_key /etc/nginx/ssl/your-domain.key;
    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_ciphers HIGH:!aNULL:!MD5;
    ssl_prefer_server_ciphers on;

    client_max_body_size 50M;
    gzip on;
    gzip_types text/plain text/css application/json application/javascript;

    location / {
        proxy_pass http://localhost:5000;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;

        sub_filter 'http://localhost:5000' 'https://your-domain.com';
        sub_filter_once off;
        sub_filter_types text/html;
        proxy_set_header Accept-Encoding "";
    }
}
```

Test and reload:
```bash
sudo nginx -t
sudo systemctl reload nginx
```

### 6.6 Upload & Launch Application

From local machine:
```bash
cd /path/to/SecureCloudStorage
mvn clean package
scp -i "your-key.pem" target/secure-cloud-storage-1.0.0.jar ubuntu@YOUR_EC2_IP:/home/ubuntu/
```

On EC2 (replace placeholders):
```bash
nohup java -Xmx512m -XX:+UseSerialGC -Djava.io.tmpdir=/tmp/tomcat-upload -jar \
  -Daws.accessKeyId=YOUR_ACCESS_KEY \
  -Daws.secretKey=YOUR_SECRET_KEY \
  -Daws.region=YOUR_REGION \
  -Daws.s3.bucket=YOUR_BUCKET_NAME \
  -Daws.dynamodb.table=Users \
  -Dapp.master-key=YOUR_BASE64_MASTER_KEY \
  -Dspring.security.oauth2.resourceserver.jwt.issuer-uri=YOUR_COGNITO_ISSUER_URI \
  -Daws.cognito.userPoolId=YOUR_USER_POOL_ID \
  -Dspring.servlet.multipart.max-file-size=50MB \
  -Dspring.servlet.multipart.max-request-size=50MB \
  secure-cloud-storage-1.0.0.jar > app.log 2>&1 &
```

Verify:
```bash
ps aux | grep java
tail -f app.log
curl http://localhost:5000
```

Access: `https://your-domain.com`

---

## 🧪 Step 7: Testing

**SSL**: `curl -I https://your-domain.com`
**Registration**: Create account and verify email
**Upload**: Upload file, check S3 bucket
**Download**: Download and verify content
**Deletion**: Delete file, verify removed from S3/DynamoDB

---

## 🔧 Step 8: Troubleshooting

**Application won't start**: `tail -n 50 app.log`
**500 Error on upload**: Verify IAM permissions (S3/DynamoDB FullAccess)
**413 Entity Too Large**: Check `client_max_body_size 50M` in Nginx
**Redirects to localhost**: Verify `sub_filter` in Nginx config
**Invalid Token**: Check Cognito issuer URI matches
**Out of Memory**: Verify swap enabled (`free -h`)

**Commands**:
```bash
tail -f app.log                    # View logs
ps aux | grep java                 # Check process
pkill -f 'secure-cloud-storage'    # Stop app
sudo systemctl restart nginx       # Restart Nginx
```

---

## 💰 Cost Estimation

**Free Tier** (12 months): $0
**After Free Tier**: EC2 ~$8/month + AWS services ~$1/month = ~$9/month

---

## 🛡️ Security Best Practices

- Use AWS Secrets Manager for credentials
- Enable CloudTrail for audit logging
- Enable MFA for Cognito
- Implement S3 bucket policies (HTTPS-only)
- Rotate IAM keys every 90 days
- Enable DynamoDB point-in-time recovery

---

## ✅ Configuration Completed

Application live at `https://your-domain.com`

For project architecture, see [Project.md](Project.md)

#### Issue 6: "Invalid Token / Authentication Failed"

**Solutions**:
- Verify Cognito issuer URI in launch command
- Check User Pool ID matches
- Ensure user is confirmed in Cognito
- Clear browser cookies and log in again

#### Issue 7: "Files Not Decrypting / Download Fails"

**Solutions**:
- Verify master key is correct (exactly as generated)
- Check logs for decryption errors:
  ```bash
  grep -i "decrypt\|cipher" app.log
  ```

### Useful Commands

```bash
# View live application logs
tail -f app.log

# Search for errors in logs
grep -i "error\|exception" app.log | tail -20

# Check disk space
df -h

# Check memory usage
free -h

# Check running processes
ps aux | grep java

# Check Nginx error logs
sudo tail -f /var/log/nginx/error.log

# Restart Nginx
sudo systemctl restart nginx

# Test domain DNS resolution
nslookup your-domain.com
```

---

## 💰 Step 9: Cost Estimation (AWS Free Tier)

### Free Tier Allowances (First 12 Months)

| Service | Free Tier | Beyond Free Tier |
|---------|-----------|------------------|
| **EC2** | 750 hours/month (t2.micro) | $0.0116/hour (~$8.41/month) |
| **S3** | 5 GB storage, 20,000 GET, 2,000 PUT | $0.023/GB/month |
| **DynamoDB** | 25 GB storage, 25 WCU, 25 RCU | $0.25/GB/month (On-Demand) |
| **Cognito** | 50,000 MAUs | $0.0055 per MAU after 50K |
| **Data Transfer** | 15 GB/month outbound | $0.114/GB |

### Example Monthly Cost (10 users, 5 GB files)

Within Free Tier: **$0**

After Free Tier expires:
- EC2 (t2.micro): **$8.41**
- S3 (5 GB): **$0.12**
- DynamoDB (1 GB): **$0.25**
- Cognito (10 users): **$0**
- **Total**: ~**$8.78/month**

---

## ✅ Setup Complete

Application live at `https://your-domain.com`

For project architecture, see [Project.md](Project.md)

SSH COMMAND: ssh -i "secure-storage-key.pem" ubuntu@13.232.45.67

APPLICATION URL: https://your-domain.com

===========================================
```

### Next Steps

1. ✅ Test all features thoroughly
2. ✅ Create user accounts
3. ✅ Upload and download test files
4. ✅ Monitor application logs
5. ✅ Set up CloudWatch alarms
6. ✅ Configure backups
7. ✅ Share application with users

For project architecture details, refer to [Project.md](Project.md)

---

**Need Help?** Check the troubleshooting section or review AWS service documentation.

# S3 CONFIG
aws.s3.bucket=cloud-s3-demoproject

# DYNAMODB CONFIG
aws.dynamodb.table=Users

# COGNITO CONFIG
spring.security.oauth2.resourceserver.jwt.issuer-uri=https://cognito-idp.ap-south-1.amazonaws.com/ap-south-1_xxxxxxxxx
aws.cognito.userPoolId=ap-south-1_xxxxxxxxx

# ENVIRONMENT VARIABLES (Set these as env vars)
# aws.accessKeyId=YOUR_ACCESS_KEY_ID
# aws.secretKey=YOUR_SECRET_ACCESS_KEY
# app.master-key=YOUR_BASE64_MASTER_KEY
```

### Set Environment Variables

#### Windows (PowerShell)
```powershell
$env:aws.accessKeyId="YOUR_ACCESS_KEY"
$env:aws.secretKey="YOUR_SECRET_KEY"
$env:app.master-key="YOUR_BASE64_KEY"
```

#### Linux/Mac
```bash
export aws.accessKeyId="YOUR_ACCESS_KEY"
export aws.secretKey="YOUR_SECRET_KEY"
export app.master-key="YOUR_BASE64_KEY"
**Need Help?** Check the troubleshooting section or review AWS service documentation.
