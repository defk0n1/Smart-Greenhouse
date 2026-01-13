# 🚀 AWS Cloud Shell Deployment Guide - Smart Greenhouse

Complete guide to deploy the Smart Greenhouse system on AWS **using only AWS Cloud Shell**. All commands are executed directly from Cloud Shell without requiring local installation.

## 📋 Table of Contents

- [Prerequisites](#-prerequisites)
- [Step 1: Access AWS Cloud Shell](#step-1--access-aws-cloud-shell)
- [Step 2: Create SSH Key](#step-2--create-ssh-key)
- [Step 3: Create Security Group](#step-3--create-security-group)
- [Step 4: Launch EC2 Instance](#step-4--launch-ec2-instance)
- [Step 5: Initial Server Configuration](#step-5--initial-server-configuration)
- [Step 6: Download GitHub Applications](#step-6--download-github-applications)
- [Step 7: Install and Configure MongoDB](#step-7--install-and-configure-mongodb)
- [Step 8: Install WildFly](#step-8--install-wildfly)
- [Step 9: Deploy WAR Files](#step-9--deploy-war-files)
- [Step 10: Install and Configure Nginx](#step-10--install-and-configure-nginx)
- [Step 11: TLS Configuration with Let's Encrypt](#step-11--tls-configuration-with-lets-encrypt)
- [Step 12: HSTS Configuration](#step-12--hsts-configuration)
- [Step 13: Final Verification](#step-13--final-verification)
- [Useful Commands](#-useful-commands)
- [Troubleshooting](#-troubleshooting)

---

## 📦 Prerequisites

### AWS Account
- Active AWS account (credit card required)
- Free Tier activated (recommended for testing)

### Local Files to Prepare
Before starting, ensure you have:
- `iam-1.0.war` - Compiled IAM application
- `smartgreenhouse.war` - Compiled API application

### Required Information
- **DuckDNS domain name**: `your-domain.duckdns.org`
- **DuckDNS token**: obtain from [duckdns.org](https://www.duckdns.org)
- **Email**: for Let's Encrypt certificates

---

## Step 1: Access AWS Cloud Shell

### 1.1 Open Cloud Shell

1. Log in to [AWS Console](https://console.aws.amazon.com)
2. Click the **Cloud Shell** icon in the top right (or search for "CloudShell" in services)
3. Wait for the environment to launch (~30 seconds)

### 1.2 Verify Region

```bash
# Check current region
aws configure get region

# Change region if necessary (example: eu-west-1 for Paris)
export AWS_DEFAULT_REGION=eu-west-1
aws configure set region eu-west-1
```

### 1.3 Create Working Directory

```bash
# Create working folder
mkdir -p ~/greenhouse-deployment
cd ~/greenhouse-deployment
```

---

## Step 2: Create SSH Key

### 2.1 Create Key Pair

```bash
# Create SSH key pair
aws ec2 create-key-pair \
    --key-name greenhouse-key \
    --query 'KeyMaterial' \
    --output text > greenhouse-key.pem

# Secure the key
chmod 400 greenhouse-key.pem

# Verify creation
aws ec2 describe-key-pairs --key-names greenhouse-key
```

**Expected result**:
```json
{
    "KeyPairs": [
        {
            "KeyPairId": "key-xxxxxxxxxxxxx",
            "KeyFingerprint": "...",
            "KeyName": "greenhouse-key",
            "KeyType": "rsa"
        }
    ]
}
```

---

## Step 3: Create Security Group

### 3.1 Create the Security Group

```bash
# Create security group
SG_ID=$(aws ec2 create-security-group \
    --group-name pipeline-sg \
    --description "Security group for Smart Greenhouse pipeline" \
    --query 'GroupId' \
    --output text)

echo "✅ Security Group created: $SG_ID"
```

### 3.2 Add Security Rules

> **🔒 Security Recommendation**: For SSH, it is **strongly recommended** to restrict access to your IP address only instead of `0.0.0.0/0`. You can get your IP with: `curl ifconfig.me`

```bash
# Option 1 (RECOMMENDED): SSH restricted to YOUR IP only
# Get your public IP
MY_IP=$(curl -s ifconfig.me)
echo "Your IP: $MY_IP"

# Allow SSH from YOUR IP only (SECURE)
aws ec2 authorize-security-group-ingress \
    --group-id $SG_ID \
    --protocol tcp \
    --port 22 \
    --cidr $MY_IP/32

# Option 2 (NOT RECOMMENDED): SSH from anywhere (SECURITY RISK)
# aws ec2 authorize-security-group-ingress \
#     --group-id $SG_ID \
#     --protocol tcp \
#     --port 22 \
#     --cidr 0.0.0.0/0

# Allow HTTP (port 80) - Required for Let's Encrypt
aws ec2 authorize-security-group-ingress \
    --group-id $SG_ID \
    --protocol tcp \
    --port 80 \
    --cidr 0.0.0.0/0

# Allow HTTPS (port 443) - Public access required
aws ec2 authorize-security-group-ingress \
    --group-id $SG_ID \
    --protocol tcp \
    --port 443 \
    --cidr 0.0.0.0/0

echo "✅ Security rules configured"
```

> **💡 Note**: If your IP changes (mobile network, VPN, etc.), you will need to update the SSH rule by removing the old one and adding the new one.

### 3.3 Verify Rules

```bash
# Display all rules
aws ec2 describe-security-groups --group-ids $SG_ID
```

---

## Step 4: Launch EC2 Instance

### 4.1 Find Ubuntu 22.04 AMI

```bash
# Find Ubuntu 22.04 AMI in your region
AMI_ID=$(aws ec2 describe-images \
    --owners 099720109477 \
    --filters "Name=name,Values=ubuntu/images/hvm-ssd/ubuntu-jammy-22.04-amd64-server-*" \
    --query 'Images | sort_by(@, &CreationDate) | [-1].ImageId' \
    --output text)

echo "Ubuntu 22.04 AMI: $AMI_ID"
```

### 4.2 Launch Instance

> **💡 Recommendation**: For **production** environment, use **t3.medium** (2 vCPU, 4GB RAM) with at least 30GB storage. For **testing**, `t2.micro` (Free Tier) can suffice but with limited performance.

```bash
# Launch EC2 instance
# For PRODUCTION: t3.medium (better performance/price)
# For TESTING/FREE TIER: t2.micro (limited performance)

INSTANCE_ID=$(aws ec2 run-instances \
    --image-id $AMI_ID \
    --instance-type t3.medium \
    --key-name greenhouse-key \
    --security-group-ids $SG_ID \
    --tag-specifications 'ResourceType=instance,Tags=[{Key=Name,Value=Smart-Greenhouse-Server},{Key=Environment,Value=Production},{Key=Project,Value=SmartGreenhouse}]' \
    --block-device-mappings '[{"DeviceName":"/dev/sda1","Ebs":{"VolumeSize":30,"VolumeType":"gp3","DeleteOnTermination":true}}]' \
    --monitoring Enabled=true \
    --query 'Instances[0].InstanceId' \
    --output text)

echo "✅ EC2 instance created: $INSTANCE_ID"
```

**Instance type comparison:**

| Type | vCPU | RAM | Cost/Month | Recommended Usage |
|------|------|-----|-----------|-------------------|
| t2.micro | 1 | 1GB | Free (Free Tier) | Testing only |
| t3.small | 2 | 2GB | ~$15 USD | Development |
| **t3.medium** | 2 | 4GB | ~$30 USD | **Production (Recommended)** |
| t3.large | 2 | 8GB | ~$60 USD | High-load production |

### 4.3 Wait for Instance to be Ready

```bash
# Wait for instance to reach "running" state
echo "⏳ Waiting for instance to start..."
aws ec2 wait instance-running --instance-ids $INSTANCE_ID

echo "✅ Instance started"
```

### 4.4 Get Public IP

```bash
# Retrieve public IP
PUBLIC_IP=$(aws ec2 describe-instances \
    --instance-ids $INSTANCE_ID \
    --query 'Reservations[0].Instances[0].PublicIpAddress' \
    --output text)

echo "🌐 Public IP: $PUBLIC_IP"
```

### 4.5 Allocate Elastic IP (**REQUIRED for Production**)

> **⚠️ IMPORTANT**: An Elastic IP is **REQUIRED** for production deployment with DuckDNS and Let's Encrypt. Without it, your IP will change on every instance restart, breaking your DNS and SSL certificates.

```bash
# Allocate Elastic IP
ALLOCATION_ID=$(aws ec2 allocate-address \
    --domain vpc \
    --tag-specifications 'ResourceType=elastic-ip,Tags=[{Key=Name,Value=Smart-Greenhouse-IP},{Key=Project,Value=SmartGreenhouse}]' \
    --query 'AllocationId' \
    --output text)

ELASTIC_IP=$(aws ec2 describe-addresses \
    --allocation-ids $ALLOCATION_ID \
    --query 'Addresses[0].PublicIp' \
    --output text)

# Associate Elastic IP to instance
aws ec2 associate-address \
    --instance-id $INSTANCE_ID \
    --allocation-id $ALLOCATION_ID

echo "✅ Elastic IP allocated: $ELASTIC_IP"
PUBLIC_IP=$ELASTIC_IP

# Save for future reference
echo $ELASTIC_IP > ~/greenhouse-deployment/elastic-ip.txt
echo $ALLOCATION_ID > ~/greenhouse-deployment/allocation-id.txt
```

### 4.6 Update DuckDNS

```bash
# DuckDNS configuration
DUCKDNS_DOMAIN="your-domain"  # Replace with your domain (without .duckdns.org)
DUCKDNS_TOKEN="your-token"     # Replace with your DuckDNS token

# Update DuckDNS with new IP
curl "https://www.duckdns.org/update?domains=$DUCKDNS_DOMAIN&token=$DUCKDNS_TOKEN&ip=$PUBLIC_IP"

echo ""
echo "✅ DuckDNS updated: $DUCKDNS_DOMAIN.duckdns.org → $PUBLIC_IP"
```

### 4.7 Wait for Complete Initialization

```bash
# Wait for system checks to pass (2/2)
echo "⏳ Waiting for system checks (about 2 minutes)..."
aws ec2 wait instance-status-ok --instance-ids $INSTANCE_ID

echo "✅ Instance ready for SSH connection"
```

---

## Step 5: Initial Server Configuration

### 5.1 SSH Connection

```bash
# Connect to instance
ssh -i ~/greenhouse-deployment/greenhouse-key.pem ubuntu@$PUBLIC_IP

# If error "WARNING: UNPROTECTED PRIVATE KEY FILE"
chmod 400 ~/greenhouse-deployment/greenhouse-key.pem
```

> **From here on, all commands are executed ON THE EC2 SERVER**

### 5.2 Update System

```bash
# Update packages
sudo apt update && sudo apt upgrade -y

# Install basic tools
sudo apt install -y curl wget git unzip htop net-tools build-essential
```

### 5.3 Install Java 21

```bash
# Install OpenJDK 21
sudo apt install -y openjdk-21-jdk

# Verify installation
java -version
```

### 5.4 Configure Firewall

```bash
# Configure UFW (Uncomplicated Firewall)
sudo ufw allow 22/tcp    # SSH
sudo ufw allow 80/tcp    # HTTP
sudo ufw allow 443/tcp   # HTTPS

# Enable firewall
echo "y" | sudo ufw enable

# Check status
sudo ufw status
```

---

## Step 6: Download GitHub Applications

### 6.1 Clone PWA Branches

```bash
# Create applications directory
mkdir -p ~/apps
cd ~/apps

# Clone pwa branch
git clone -b pwa https://github.com/defk0n1/Smart-Greenhouse.git pwa

# Clone pwa-admin branch
git clone -b pwa-admin https://github.com/defk0n1/Smart-Greenhouse.git pwa-admin

echo "✅ PWA applications downloaded"
ls -la ~/apps/
```

---

## Step 7: Install and Configure MongoDB

### 7.1 Install MongoDB 7.0

```bash
# Import MongoDB GPG key
curl -fsSL https://www.mongodb.org/static/pgp/server-7.0.asc | \
   sudo gpg -o /usr/share/keyrings/mongodb-server-7.0.gpg --dearmor

# Add MongoDB repository
echo "deb [ arch=amd64,arm64 signed-by=/usr/share/keyrings/mongodb-server-7.0.gpg ] https://repo.mongodb.org/apt/ubuntu jammy/mongodb-org/7.0 multiverse" | \
    sudo tee /etc/apt/sources.list.d/mongodb-org-7.0.list

# Update and install
sudo apt update
sudo apt install -y mongodb-org

# Start MongoDB
sudo systemctl start mongod
sudo systemctl enable mongod

# Check status
sudo systemctl status mongod
```

### 7.2 Configure MongoDB without Password (Local Authentication)

> **💡 Recommendation**: For simplified configuration, we'll use Unix socket authentication which allows local connection without password.

```bash
# Edit MongoDB configuration file
sudo nano /etc/mongod.conf
```

Modify the `security` and `net` sections:

```yaml
# network interfaces
net:
  port: 27017
  bindIp: 127.0.0.1  # Listen on localhost only

# security
security:
  authorization: disabled  # Disable authentication (localhost only)
```

```bash
# Restart MongoDB
sudo systemctl restart mongod

# Check status
sudo systemctl status mongod
```

### 7.3 Create Database

```bash
# Connect to MongoDB (no password since local connection)
mongosh

# In MongoDB shell, execute:
```

```javascript
// Create project database
use CoT_Project

// Create collections
db.createCollection("actuators")
db.createCollection("greenhouses")
db.createCollection("identities")
db.createCollection("sensors")
db.createCollection("tenants")

// Verify collections
show collections

// Exit
exit
```

### 7.4 Import JSON Data

**From AWS Cloud Shell** (upload JSON files):

```bash
# From your local machine, upload JSON files to Cloud Shell
# Actions → Upload files
# Select all .json files from mongodb/ folder

# Then upload to EC2 instance
cd ~/greenhouse-deployment

# Upload JSON files
scp -i greenhouse-key.pem CoT_Project.actuators.json ubuntu@$PUBLIC_IP:/tmp/
scp -i greenhouse-key.pem CoT_Project.greenhouses.json ubuntu@$PUBLIC_IP:/tmp/
scp -i greenhouse-key.pem CoT_Project.identities.json ubuntu@$PUBLIC_IP:/tmp/
scp -i greenhouse-key.pem CoT_Project.sensors.json ubuntu@$PUBLIC_IP:/tmp/
scp -i greenhouse-key.pem CoT_Project.tenants.json ubuntu@$PUBLIC_IP:/tmp/
```

**On EC2 server**:

```bash
# Import data into MongoDB
mongoimport --db CoT_Project --collection actuators --file /tmp/CoT_Project.actuators.json --jsonArray
mongoimport --db CoT_Project --collection greenhouses --file /tmp/CoT_Project.greenhouses.json --jsonArray
mongoimport --db CoT_Project --collection identities --file /tmp/CoT_Project.identities.json --jsonArray
mongoimport --db CoT_Project --collection sensors --file /tmp/CoT_Project.sensors.json --jsonArray
mongoimport --db CoT_Project --collection tenants --file /tmp/CoT_Project.tenants.json --jsonArray

# Verify import
mongosh CoT_Project

// Count imported documents
db.actuators.countDocuments()    // Should display number of actuators
db.greenhouses.countDocuments()  // Should display number of greenhouses
db.identities.countDocuments()   // Should display number of identities
db.sensors.countDocuments()      // Should display number of sensors
db.tenants.countDocuments()      // Should display number of tenants

// Display some documents to verify
db.greenhouses.find().limit(2).pretty()

exit
```

### 7.5 Configure MongoDB Connection in Applications

Applications will connect to MongoDB with this configuration:

**Connection string**: `mongodb://localhost:27017/CoT_Project`

> **🔒 Security Note**: This configuration is safe because MongoDB only listens on localhost (127.0.0.1) and is not accessible from outside. Only applications on the server can connect.

---

## Step 8: Install WildFly

### 8.1 Download and Install WildFly 37

```bash
# Download WildFly
cd /tmp
wget https://github.com/wildfly/wildfly/releases/download/37.0.0.Final/wildfly-37.0.0.Final.tar.gz

# Extract
sudo tar xzf wildfly-37.0.0.Final.tar.gz -C /opt/
sudo mv /opt/wildfly-37.0.0.Final /opt/wildfly

# Create wildfly user
sudo groupadd -r wildfly
sudo useradd -r -g wildfly -d /opt/wildfly -s /sbin/nologin wildfly
sudo chown -R wildfly:wildfly /opt/wildfly
```

### 8.2 Create Systemd Service

```bash
# Create service file
sudo nano /etc/systemd/system/wildfly.service
```

Content:

```ini
[Unit]
Description=WildFly Application Server
After=network.target

[Service]
Type=notify
User=wildfly
Group=wildfly
Environment="JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64"
Environment="WILDFLY_HOME=/opt/wildfly"
Environment="JAVA_OPTS=-Xms512m -Xmx2048m -XX:MetaspaceSize=96M -XX:MaxMetaspaceSize=512m"
ExecStart=/opt/wildfly/bin/standalone.sh -b 127.0.0.1
TimeoutStartSec=600
TimeoutStopSec=600
Restart=on-failure

[Install]
WantedBy=multi-user.target
```

### 8.3 Start WildFly

```bash
# Reload systemd
sudo systemctl daemon-reload

# Start WildFly
sudo systemctl start wildfly
sudo systemctl enable wildfly

# Wait for startup (30 seconds)
sleep 30

# Check status
sudo systemctl status wildfly

# Check logs
sudo tail -f /opt/wildfly/standalone/log/server.log
```

---

## Step 9: Deploy WAR Files

### 9.1 Upload WAR Files from Cloud Shell

**Return to AWS Cloud Shell** (new window or tab):

```bash
# From Cloud Shell,# Upload Nginx config from your machine (OPTIONAL - will be created manually)
# scp -i greenhouse-key.pem deployment/nginx/greenhouse.conf ubuntu@$PUBLIC_IP:/tmp/
# scp -i greenhouse-key.pem deployment/nginx/ssl-params.conf ubuntu@$PUBLIC_IP:/tmp/

# Upload smartgreenhouse.war
scp -i greenhouse-key.pem /path/to/smartgreenhouse.war ubuntu@$PUBLIC_IP:/tmp/
```

> **Note**: You must first upload WAR files to Cloud Shell (Actions → Upload file) before copying them to EC2

### 9.2 Deploy on WildFly

**Return to EC2 server**:

```bash
# Copy WARs to deployment directory
sudo cp /tmp/iam-1.0.war /opt/wildfly/standalone/deployments/
sudo cp /tmp/smartgreenhouse.war /opt/wildfly/standalone/deployments/

# Change permissions
sudo chown wildfly:wildfly /opt/wildfly/standalone/deployments/*.war

# WildFly automatically detects and deploys applications (~30 seconds)
# Check logs
sudo tail -f /opt/wildfly/standalone/log/server.log
```

Wait to see:

```
WFLYSRV0010: Deployed "iam-1.0.war"
WFLYSRV0010: Deployed "smartgreenhouse.war"
```

### 9.3 Test Applications

```bash
# Test IAM
curl -I http://localhost:8080/iam-1.0/

# Test API
curl -I http://localhost:8080/smartgreenhouse/

# Should return HTTP/1.1 200 OK or 302 Redirect
```

---

## Step 10: Install and Configure Nginx

### 10.1 Install Nginx

```bash
# Install Nginx
sudo apt install -y nginx

# Start Nginx
sudo systemctl start nginx
sudo systemctl enable nginx

# Check status
sudo systemctl status nginx
```

### 10.2 Create Nginx Configuration

```bash
# Create configuration file
sudo nano /etc/nginx/sites-available/greenhouse
```

Content:

```nginx
# Nginx Configuration for Smart Greenhouse

# HTTP to HTTPS redirect
server {
    listen 80;
    listen [::]:80;
    server_name your-domain.duckdns.org;  # REPLACE WITH YOUR DOMAIN

    # Let's Encrypt challenge
    location /.well-known/acme-challenge/ {
        root /var/www/html;
    }

    # HTTPS redirect
    location / {
        return 301 https://$server_name$request_uri;
    }
}

# HTTPS configuration
server {
    listen 443 ssl http2;
    listen [::]:443 ssl http2;
    server_name your-domain.duckdns.org;  # REPLACE WITH YOUR DOMAIN

    # Logs
    access_log /var/log/nginx/greenhouse-access.log;
    error_log /var/log/nginx/greenhouse-error.log;

    # SSL certificates (will be configured by Certbot)
    # ssl_certificate /etc/letsencrypt/live/your-domain.duckdns.org/fullchain.pem;
    # ssl_certificate_key /etc/letsencrypt/live/your-domain.duckdns.org/privkey.pem;

    # SSL parameters
    include /etc/nginx/ssl-params.conf;

    # PWA Client
    location /pwa {
        alias /var/www/greenhouse/pwa;
        index index.html;
        try_files $uri $uri/ /pwa/index.html;
    }

    # PWA Admin
    location /admin {
        alias /var/www/greenhouse/pwa-admin;
        index index.html;
        try_files $uri $uri/ /admin/index.html;
    }

    # IAM API
    location /iam {
        proxy_pass http://127.0.0.1:8080/iam-1.0;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_redirect off;
    }

    # Smart Greenhouse API
    location /smartgreenhouse {
        proxy_pass http://127.0.0.1:8080/smartgreenhouse;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_redirect off;
    }

    # Home page
    location / {
        root /var/www/greenhouse;
        index index.html;
    }
}
```

### 10.3 Create SSL Parameters File

```bash
# Create ssl-params.conf file
sudo nano /etc/nginx/ssl-params.conf
```

Content:

```nginx
# SSL/TLS Security Parameters
# Based on Mozilla SSL Configuration Generator (Intermediate profile)

# SSL Protocols
ssl_protocols TLSv1.2 TLSv1.3;
ssl_prefer_server_ciphers off;

# SSL Ciphers
ssl_ciphers 'ECDHE-ECDSA-AES128-GCM-SHA256:ECDHE-RSA-AES128-GCM-SHA256:ECDHE-ECDSA-AES256-GCM-SHA384:ECDHE-RSA-AES256-GCM-SHA384:ECDHE-ECDSA-CHACHA20-POLY1305:ECDHE-RSA-CHACHA20-POLY1305:DHE-RSA-AES128-GCM-SHA256:DHE-RSA-AES256-GCM-SHA384';

# SSL Session
ssl_session_timeout 1d;
ssl_session_cache shared:SSL:50m;
ssl_session_tickets off;

# OCSP Stapling
ssl_stapling on;
ssl_stapling_verify on;
resolver 8.8.8.8 8.8.4.4 valid=300s;
resolver_timeout 5s;
```

### 10.4 Deploy PWAs

```bash
# Create web directory
sudo mkdir -p /var/www/greenhouse

# Copy PWAs
sudo cp -r ~/apps/pwa /var/www/greenhouse/
sudo cp -r ~/apps/pwa-admin /var/www/greenhouse/

# Change permissions
sudo chown -R www-data:www-data /var/www/greenhouse
```

### 10.5 Enable Configuration

```bash
# Enable site
sudo ln -s /etc/nginx/sites-available/greenhouse /etc/nginx/sites-enabled/

# Disable default site
sudo rm /etc/nginx/sites-enabled/default

# Test configuration
sudo nginx -t

# If OK, reload Nginx
sudo systemctl reload nginx
```

---

## Step 11: TLS Configuration with Let's Encrypt

### 11.1 Install Certbot

```bash
# Install Certbot and Nginx plugin
sudo apt install -y certbot python3-certbot-nginx
```

### 11.2 Obtain SSL Certificate

```bash
# Obtain certificate (replace with your domain and email)
sudo certbot --nginx \
    -d your-domain.duckdns.org \
    --non-interactive \
    --agree-tos \
    -m your-email@example.com

# Certbot automatically configures Nginx for HTTPS
```

### 11.3 Verify Certificate

```bash
# Verify certificate installation
sudo certbot certificates

# Test renewal
sudo certbot renew --dry-run
```

### 11.4 Configure Automatic Renewal

```bash
# Verify systemd timer is active
sudo systemctl status certbot.timer

# Automatic renewal is already configured
# Certificates will be automatically renewed every 60 days
```

---

## Step 12: HSTS Configuration

### 12.1 Add Security Headers

```bash
# Edit Nginx configuration
sudo nano /etc/nginx/sites-available/greenhouse
```

Add in the HTTPS `server` block (after `include /etc/nginx/ssl-params.conf;`):

```nginx
    # HSTS (HTTP Strict Transport Security)
    add_header Strict-Transport-Security "max-age=31536000; includeSubDomains; preload" always;

    # Other security headers
    add_header X-Frame-Options "SAMEORIGIN" always;
    add_header X-Content-Type-Options "nosniff" always;
    add_header X-XSS-Protection "1; mode=block" always;
    add_header Referrer-Policy "no-referrer-when-downgrade" always;
```

### 12.2 Apply Changes

```bash
# Test configuration
sudo nginx -t

# Reload Nginx
sudo systemctl reload nginx
```

---

## Step 13: Final Verification

### 13.1 Test HTTPS Connection

```bash
# From server
curl -I https://your-domain.duckdns.org

# Verify HTTP → HTTPS redirect
curl -I http://your-domain.duckdns.org
# Should return 301 Moved Permanently
```

### 13.2 Verify HSTS

```bash
# Check HSTS header
curl -I https://your-domain.duckdns.org | grep -i strict

# Should display:
# strict-transport-security: max-age=31536000; includeSubDomains; preload
```

### 13.3 Test Applications

Open in browser:

- **PWA Client**: `https://your-domain.duckdns.org/pwa`
- **PWA Admin**: `https://your-domain.duckdns.org/admin`
- **IAM API**: `https://your-domain.duckdns.org/iam`
- **Greenhouse API**: `https://your-domain.duckdns.org/smartgreenhouse`

### 13.4 Verify Services

```bash
# Check all services
sudo systemctl status mongod
sudo systemctl status wildfly
sudo systemctl status nginx

# Check logs
sudo tail -f /var/log/nginx/greenhouse-access.log
sudo tail -f /opt/wildfly/standalone/log/server.log
sudo tail -f /var/log/mongodb/mongod.log
```

### 13.5 Test SSL Security

Use [SSL Labs](https://www.ssllabs.com/ssltest/) to test your configuration:

```
https://www.ssllabs.com/ssltest/analyze.html?d=your-domain.duckdns.org
```

**Expected score**: A or A+

---

## 📊 CloudWatch Monitoring Configuration (Production)

> **💡 Recommended for Production**: Configure CloudWatch alarms to monitor your instance and receive alerts in case of issues.

### Create SNS Topic for Alerts

**From AWS Cloud Shell:**

```bash
# Create SNS topic for notifications
SNS_TOPIC_ARN=$(aws sns create-topic \
    --name smart-greenhouse-alerts \
    --query 'TopicArn' \
    --output text)

echo "SNS Topic created: $SNS_TOPIC_ARN"

# Subscribe with your email
aws sns subscribe \
    --topic-arn $SNS_TOPIC_ARN \
    --protocol email \
    --notification-endpoint your-email@example.com

echo "⚠️ Check your email and confirm SNS subscription"
```

### Create CloudWatch Alarms

```bash
# CPU alarm > 80%
aws cloudwatch put-metric-alarm \
    --alarm-name greenhouse-high-cpu \
    --alarm-description "CPU utilization > 80%" \
    --metric-name CPUUtilization \
    --namespace AWS/EC2 \
    --statistic Average \
    --period 300 \
    --threshold 80 \
    --comparison-operator GreaterThanThreshold \
    --evaluation-periods 2 \
    --alarm-actions $SNS_TOPIC_ARN \
    --dimensions Name=InstanceId,Value=$INSTANCE_ID

# Status Check Failed alarm
aws cloudwatch put-metric-alarm \
    --alarm-name greenhouse-status-check-failed \
    --alarm-description "Instance status check failed" \
    --metric-name StatusCheckFailed \
    --namespace AWS/EC2 \
    --statistic Maximum \
    --period 60 \
    --threshold 1 \
    --comparison-operator GreaterThanOrEqualToThreshold \
    --evaluation-periods 2 \
    --alarm-actions $SNS_TOPIC_ARN \
    --dimensions Name=InstanceId,Value=$INSTANCE_ID

# Disk Space alarm (requires CloudWatch Agent)
# CloudWatch Agent installation on EC2 instance (optional)

echo "✅ CloudWatch alarms configured"
echo "📧 You will receive emails in case of alerts"
```

### View Metrics

```bash
# List all alarms
aws cloudwatch describe-alarms --alarm-names \
    greenhouse-high-cpu \
    greenhouse-status-check-failed

# View metrics in console
echo "CloudWatch Console: https://console.aws.amazon.com/cloudwatch"
```

---

## 🛠 Useful Commands

### Service Management

```bash
# WildFly
sudo systemctl status wildfly
sudo systemctl restart wildfly
sudo systemctl stop wildfly
sudo journalctl -u wildfly -f

# MongoDB
sudo systemctl status mongod
sudo systemctl restart mongod
sudo journalctl -u mongod -f

# Nginx
sudo systemctl status nginx
sudo systemctl reload nginx
sudo systemctl restart nginx
sudo journalctl -u nginx -f
```

### Monitoring

```bash
# CPU and RAM
htop

# Disk space
df -h

# Open ports
sudo netstat -tulpn | grep LISTEN

# Active connections
sudo ss -tuln
```

### MongoDB

```bash
# Connect to MongoDB
mongosh -u greenhouseUser -p --authenticationDatabase CoT_Project

# List databases
show dbs

# Use CoT_Project database
use CoT_Project

# List collections
show collections

# Count documents
db.SensorData.countDocuments()
```

### Logs

```bash
# Real-time Nginx logs
sudo tail -f /var/log/nginx/greenhouse-access.log
sudo tail -f /var/log/nginx/greenhouse-error.log

# WildFly logs
sudo tail -f /opt/wildfly/standalone/log/server.log

# MongoDB logs
sudo tail -f /var/log/mongodb/mongod.log

# System logs
sudo journalctl -xe
```

---

## 🆘 Troubleshooting

### Problem: Cannot connect via SSH

```bash
# From Cloud Shell, check Security Group
aws ec2 describe-security-groups --group-ids $SG_ID

# Verify port 22 is open
aws ec2 describe-security-groups --group-ids $SG_ID \
    --query 'SecurityGroups[0].IpPermissions[?FromPort==`22`]'

# Check instance status
aws ec2 describe-instance-status --instance-ids $INSTANCE_ID
```

### Problem: WildFly doesn't start

```bash
# Check logs
sudo journalctl -u wildfly -n 100 --no-pager

# Verify Java
java -version

# Check permissions
sudo chown -R wildfly:wildfly /opt/wildfly

# Restart
sudo systemctl restart wildfly
```

### Problem: MongoDB authentication failed

```bash
# Connect without authentication (local mode)
mongosh

# Recreate user
use CoT_Project
db.dropUser("greenhouseUser")
db.createUser({
  user: "greenhouseUser",
  pwd: "NewPassword123!",
  roles: [{ role: "readWrite", db: "CoT_Project" }]
})

# Update WAR application configuration
sudo nano /opt/wildfly/standalone/deployments/smartgreenhouse.war/WEB-INF/classes/META-INF/microprofile-config.properties
```

### Problem: Nginx 502 Bad Gateway

```bash
# Verify WildFly is started
sudo systemctl status wildfly

# Verify applications are deployed
ls -la /opt/wildfly/standalone/deployments/

# Check Nginx logs
sudo tail -f /var/log/nginx/greenhouse-error.log

# Test local connection
curl -I http://localhost:8080/smartgreenhouse/
```

### Problem: Let's Encrypt fails

```bash
# Verify domain points to correct IP
nslookup your-domain.duckdns.org

# Verify port 80 is open
sudo netstat -tulpn | grep :80

# Check Certbot logs
sudo tail -f /var/log/letsencrypt/letsencrypt.log

# Retry with verbose mode
sudo certbot --nginx -d your-domain.duckdns.org --verbose
```

### Problem: PWA doesn't load

```bash
# Verify files exist
ls -la /var/www/greenhouse/pwa/
ls -la /var/www/greenhouse/pwa-admin/

# Check permissions
sudo chown -R www-data:www-data /var/www/greenhouse

# Verify Nginx configuration
sudo nginx -t

# Reload Nginx
sudo systemctl reload nginx
```

---

## 🔧 Post-Deployment Configuration

### Configure DuckDNS Auto-Update

```bash
# Create update script
sudo nano /usr/local/bin/duckdns-update.sh
```

Content:

```bash
#!/bin/bash
echo url="https://www.duckdns.org/update?domains=your-domain&token=your-token&ip=" | curl -k -o /tmp/duckdns.log -K -
```

```bash
# Make executable
sudo chmod 700 /usr/local/bin/duckdns-update.sh

# Test
sudo /usr/local/bin/duckdns-update.sh
cat /tmp/duckdns.log  # Should display "OK"

# Add to cron (every 5 minutes)
sudo crontab -e
```

Add line:

```cron
*/5 * * * * /usr/local/bin/duckdns-update.sh >/dev/null 2>&1
```

### Configure MongoDB Backups

```bash
# Create backup script
sudo nano /usr/local/bin/backup-mongodb.sh
```

Content:

```bash
#!/bin/bash

BACKUP_DIR="/home/ubuntu/backups/mongodb"
DATE=$(date +%Y%m%d_%H%M%S)
MONGO_USER="greenhouseUser"
MONGO_PASS="SecurePassword456!"  # CHANGE
DB_NAME="CoT_Project"

mkdir -p $BACKUP_DIR

mongodump \
    --username=$MONGO_USER \
    --password=$MONGO_PASS \
    --authenticationDatabase=$DB_NAME \
    --db=$DB_NAME \
    --out=$BACKUP_DIR/backup_$DATE

# Delete backups older than 7 days
find $BACKUP_DIR -type d -mtime +7 -exec rm -rf {} \;

echo "Backup completed: $BACKUP_DIR/backup_$DATE"
```

```bash
# Make executable
sudo chmod +x /usr/local/bin/backup-mongodb.sh

# Test
sudo /usr/local/bin/backup-mongodb.sh

# Add to cron (every day at 2 AM)
sudo crontab -e
```

Add:

```cron
0 2 * * * /usr/local/bin/backup-mongodb.sh >/dev/null 2>&1
```

---

## 📊 AWS Cost Information

### Estimated Monthly Costs (US-East-1 Region)

| Service | Configuration | Estimated Cost/Month | Notes |
|---------|---------------|---------------------|-------|
| **EC2 t3.medium** ⭐ | 2 vCPU, 4GB RAM | ~$30 USD | **Recommended Production** |
| EC2 t3.small | 2 vCPU, 2GB RAM | ~$15 USD | Development |
| EC2 t2.micro | 1 vCPU, 1GB RAM | Free (12 months) | Testing only |
| EBS 30GB gp3 | High-performance SSD | ~$2.40 USD | Included |
| Elastic IP | Fixed IP | **Free** if attached | Required |
| Data Transfer | Up to 100GB outbound | Free (Free Tier) | Sufficient |
| **TOTAL Production (t3.medium)** | | **~$32 USD/month** | |
| **TOTAL Free Tier (t2.micro)** | | **~$2.40 USD/month** | For 12 months |

### Cost Optimizations

1. **Reserved Instances**: Save up to 72% by committing to 1-3 years
2. **Savings Plans**: 30-40% reduction with flexible commitment
3. **Stop instance**: Stop instance when not in use (pay only for EBS storage)
4. **Monitoring**: Configure AWS billing alarms

> **💰 Tip**: For new AWS accounts, use **Free Tier** (t2.micro) for 12 months free!

---

## 🔒 Security Checklist

### ✅ Basic Security (Automatic)

- [x] UFW firewall configured
- [x] TLS/HTTPS enabled (Let's Encrypt)
- [x] HSTS enabled with preload
- [x] MongoDB authentication enabled
- [x] Restrictive AWS Security Group
- [x] Elastic IP for fixed IP
- [x] SSL auto-renewal configured
- [x] CloudWatch monitoring enabled

### ⚠️ Advanced Security (Manual)

**High Priority:**
- [ ] **Change ALL default passwords** (MongoDB, applications)
- [ ] **Update SMTP secrets** in `/opt/wildfly/standalone/deployments/iam-1.0.war`
- [ ] **Configure fail2ban** to protect SSH against brute force attacks
- [ ] **Configure automatic backups** MongoDB (script provided)

**Medium Priority:**
- [ ] **Restrict SSH**: Modify Security Group to allow SSH only from your IP
- [ ] **Enable MFA** on your AWS account
- [ ] **Configure CloudWatch alarms** for CPU, RAM, and disk
- [ ] **Create weekly EBS snapshot**

**Low Priority:**
- [ ] Configure AWS Config for compliance audit
- [ ] Enable AWS CloudTrail for activity logs
- [ ] Set up AWS Backup for automated backups

### 🛡️ Fail2ban Configuration (Recommended)

```bash
# Install fail2ban
sudo apt install -y fail2ban

# Create custom configuration
sudo nano /etc/fail2ban/jail.local
```

Content:

```ini
[sshd]
enabled = true
port = 22
filter = sshd
logpath = /var/log/auth.log
maxretry = 3
bantime = 3600
findtime = 600
```

```bash
# Start fail2ban
sudo systemctl enable fail2ban
sudo systemctl start fail2ban

# Check status
sudo fail2ban-client status sshd
```

---

## 📞 Resources

- **AWS Documentation**: https://docs.aws.amazon.com
- **DuckDNS**: https://www.duckdns.org
- **Let's Encrypt**: https://letsencrypt.org
- **MongoDB**: https://www.mongodb.com/docs
- **WildFly**: https://docs.wildfly.org
- **Nginx**: https://nginx.org/en/docs

---

## 🎉 Congratulations!

Your Smart Greenhouse is now deployed on AWS with:
- ✅ TLS/HTTPS enabled
- ✅ HSTS enabled
- ✅ MongoDB secured
- ✅ PWA applications deployed
- ✅ Backend APIs deployed
- ✅ Auto-renewable SSL certificates

**Access URL**: `https://your-domain.duckdns.org`

---

## 📝 Production Best Practices

### Pre-Production Checklist

**Infrastructure:**
- [ ] Instance **t3.medium** or higher
- [ ] Elastic IP configured and documented
- [ ] Security Group with SSH restricted to your IP
- [ ] CloudWatch alarms configured
- [ ] Automatic MongoDB backups enabled

**Security:**
- [ ] All default passwords changed
- [ ] fail2ban enabled and configured
- [ ] HSTS enabled with preload
- [ ] Valid SSL certificates (Let's Encrypt)
- [ ] MongoDB with authentication enabled

**Monitoring:**
- [ ] CloudWatch alarms for CPU, memory, disk
- [ ] SNS configured with your email
- [ ] Centralized and accessible logs
- [ ] WildFly application metrics

**Backup & Disaster Recovery:**
- [ ] Daily MongoDB backup configured
- [ ] Weekly EBS snapshot
- [ ] SSH key saved in safe location
- [ ] Up-to-date documentation

### Performance Optimizations

**WildFly:**
```bash
# Edit /etc/systemd/system/wildfly.service
# Increase memory according to your needs
Environment="JAVA_OPTS=-Xms1024m -Xmx3072m -XX:MetaspaceSize=128M -XX:MaxMetaspaceSize=512m"
```

**MongoDB:**
```bash
# Create indexes to improve performance
mongosh -u greenhouseUser -p --authenticationDatabase CoT_Project

use CoT_Project
db.SensorData.createIndex({ "timestamp": -1 })
db.SensorData.createIndex({ "greenhouseId": 1, "sensorType": 1 })
```

**Nginx:**
```nginx
# Add in /etc/nginx/nginx.conf (http block)
gzip on;
gzip_vary on;
gzip_types text/plain text/css application/json application/javascript text/xml application/xml;

# Static cache
location ~* \.(jpg|jpeg|png|gif|ico|css|js)$ {
    expires 1y;
    add_header Cache-Control "public, immutable";
}
```

### Disaster Recovery Plan

**1. Regular Backups**
```bash
# Complete backup script (run weekly)
#!/bin/bash
DATE=$(date +%Y%m%d)

# Backup MongoDB
mongodump --out=/backups/mongo_$DATE

# Backup configurations
tar -czf /backups/config_$DATE.tar.gz \
    /etc/nginx/sites-available \
    /opt/wildfly/standalone/deployments \
    /var/www/greenhouse

# Upload to S3 (optional)
# aws s3 cp /backups/ s3://my-greenhouse-backups/
```

**2. Restoration Procedure**
```bash
# Restore MongoDB
mongorestore --drop /backups/mongo_20260112/

# Restore configurations
tar -xzf /backups/config_20260112.tar.gz -C /
sudo systemctl restart nginx wildfly
```

**3. Backup Instance (AMI)**
```bash
# Create AMI of your instance (from Cloud Shell)
aws ec2 create-image \
    --instance-id $INSTANCE_ID \
    --name "smart-greenhouse-backup-$(date +%Y%m%d)" \
    --description "Backup instance Smart Greenhouse"
```

---

## 📝 Important Notes

1. **Replace all placeholders**:
   - `your-domain.duckdns.org` → your actual domain
   - `your-token` → your DuckDNS token
   - `your-email@example.com` → your email
   - `ChangeThisPassword123!` → your secure passwords

2. **Save**:
   - SSH key (`greenhouse-key.pem`)
   - MongoDB passwords
   - Nginx configuration
   - AWS IDs (Instance ID, Security Group ID, etc.)

3. **Monitoring**:
   - Monitor logs regularly
   - Check disk space
   - Monitor CPU/RAM usage
   - Configure CloudWatch alarms

4. **Maintenance**:
   - Update system regularly: `sudo apt update && sudo apt upgrade`
   - Verify SSL certificates before expiration
   - Test MongoDB backups
   - Monitor AWS costs

---

**Deployment completed successfully! 🚀**
