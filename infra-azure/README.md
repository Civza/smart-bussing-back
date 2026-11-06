# SmartBussing - Pulumi Infrastructure as Code (Microsoft Azure)

This folder contains the complete Infrastructure as Code (IaC) written in TypeScript using **Pulumi** to provision, secure, and operate the **SmartBussing Backend** on **Microsoft Azure**.

---

## 🏗️ Azure Architecture Overview

1. **Azure Resource Group**: Logical container grouping all SmartBussing resources.
2. **Azure Container Registry (ACR)**: Private container registry to store and scan the Spring Boot Docker image.
3. **Azure Database for PostgreSQL (Flexible Server)**:
   - PostgreSQL 16 on burstable compute (`Standard_B1ms`).
   - Automated 7-day backups and encrypted 32 GB storage.
   - Automatically connected to the app via SSL (`jdbc:postgresql://...`).
4. **Azure Static Web Apps (Free Tier)**:
   - Hosts the **SmartBussing Web Frontend** (React + Vite SPA from `smart-bussing-web`).
   - **100% Free** forever: Includes global CDN content delivery, automated SSL certificate, and custom domain mapping.
5. **Log Analytics Workspace**: Centralized logging repository for container diagnostic and access logs.
6. **Azure Container Apps**:
   - Modern serverless container runtime (built on Kubernetes & Envoy).
   - **Built-in HTTPS Ingress**: Automatically provides a secure public URL with a managed SSL certificate.
   - Autoscaling: Automatically scales based on HTTP load.
   - Injects secrets (`dbAdminPassword`, `mapboxToken`, `acrPassword`) securely.
7. **Cost Management & Student Budget Alerts**:
   - Automatically tracks spending against your budget limit ($80).
   - Sends warning emails at 50%, 80%, and 95% of credit usage.

---

## 📋 Prerequisites

1. **Node.js** (v18+) and **npm**.
2. **Pulumi CLI**:
   ```powershell
   winget install Pulumi.Pulumi
   ```
3. **Azure CLI**:
   ```powershell
   winget install Microsoft.AzureCLI
   az login
   ```
4. **Docker** installed and running.

---

## 🚀 Deployment Guide

### 1. Install Dependencies
```powershell
cd smart-bussing-back\infra-azure
npm install
```

### 2. Initialize Stack & Set Secrets
```powershell
pulumi stack init dev

# Azure location
pulumi config set azure-native:location eastus

# Database credentials
pulumi config set smart-bussing-infra-azure:dbName smartbussing
pulumi config set smart-bussing-infra-azure:dbAdminUsername smartbusser
pulumi config set --secret smart-bussing-infra-azure:dbAdminPassword "YourStrongPassword123!"

# Mapbox token
pulumi config set --secret smart-bussing-infra-azure:mapboxToken "pk.eyJ1I..."
```

### 3. Build and Push Backend Docker Image to Azure Container Registry (ACR)
```powershell
# 1. Login to Azure ACR
az acr login --name <REGISTRY_NAME>

# 2. Build image from smart-bussing-back root
cd ..
docker build -t <LOGIN_SERVER>/smart-bussing-api:latest .

# 3. Push to ACR
docker push <LOGIN_SERVER>/smart-bussing-api:latest
```

### 4. Deploy Infrastructure
```powershell
cd infra-azure
pulumi up
```

### 5. Build & Deploy the Web Page (`smart-bussing-web`)
Once Pulumi creates your Static Web App, you can build and publish your Vite frontend directly:

```powershell
# 1. Build the Vite production bundle
cd c:\Users\emili\smart-bussing-web\smart-bussing
npm install
npm run build

# 2. Deploy using Azure Static Web Apps CLI
npx @azure/static-web-apps-cli deploy ./dist --deployment-token (pulumi -C c:\Users\emili\smart-bussing-back\infra-azure stack output webDeploymentToken --show-secrets)
```

---

## 📱 Connecting SmartBussingMobile

Once deployed, copy `mobileBackendUrl` from the Pulumi outputs and update [`SmartBussingMobile/.env`](file:///c:/Users/emili/smartBussing/SmartBussingMobile/.env):

```ini
BACKEND_URL = https://smartbussing-dev-api.xxxx.eastus.azurecontainerapps.io/api/v1
```

---

## 🧹 Destroying Resources
```powershell
pulumi destroy
```
