import * as pulumi from "@pulumi/pulumi";
import * as resources from "@pulumi/azure-native/resources";
import * as containerregistry from "@pulumi/azure-native/containerregistry";
import * as operationalinsights from "@pulumi/azure-native/operationalinsights";
import * as dbforpostgresql from "@pulumi/azure-native/dbforpostgresql";
import * as app from "@pulumi/azure-native/app";
import * as consumption from "@pulumi/azure-native/consumption";
import * as web from "@pulumi/azure-native/web";

// ============================================================================
// 1. Configuration & Parameters
// ============================================================================
const config = new pulumi.Config();
const azureConfig = new pulumi.Config("azure-native");

const env = config.get("environment") || "dev";
const location = azureConfig.get("location") || "westus";
const projectName = "smartbussing";
const prefix = `${projectName}-${env}`;

// Student Budget parameters
const budgetLimit = config.getNumber("budgetLimit") || 80; // Alert at $80 threshold
const alertEmail = config.get("alertEmail") || process.env.ALERT_EMAIL;

// App parameters
const appCpu = config.getNumber("appCpu") || 0.5;       // 0.5 vCPU
const appMemory = config.get("appMemory") || "1.0Gi";   // 1.0 GiB RAM
const imageTag = config.get("imageTag") || "latest";

// Database parameters
const dbName = config.get("dbName");
const dbAdminUsername = config.get("dbAdminUsername");
const dbAdminPassword = config.getSecret("dbAdminPassword") || pulumi.secret(process.env.PROD_DB_PASSWORD || "");
const dbSku = config.get("dbSku") || "Standard_B1ms"; // Cost-effective burstable tier

// Mapbox Token
const mapboxToken = config.getSecret("mapboxToken") || pulumi.secret(process.env.MAPBOX_API_KEY || "");

// Standard tags
const tags = {
    Project: "SmartBussing",
    Environment: env,
    ManagedBy: "Pulumi",
};

// ============================================================================
// 2. Resource Group
// ============================================================================
const resourceGroup = new resources.ResourceGroup(`${prefix}-rg`, {
    resourceGroupName: `${prefix}-rg`,
    location: location,
    tags: tags,
});

// ============================================================================
// 3. Azure Container Registry (ACR)
// ============================================================================
// ACR names must be alphanumeric only
const cleanPrefix = `${projectName}${env}`.replace(/[^a-zA-Z0-9]/g, "");
const registry = new containerregistry.Registry(`${cleanPrefix}acr`, {
    resourceGroupName: resourceGroup.name,
    registryName: `${cleanPrefix}acr`,
    sku: { name: "Basic" },
    adminUserEnabled: true,
    location: resourceGroup.location,
    tags: tags,
});

// Get ACR admin credentials for Azure Container Apps
const registryCredentials = containerregistry.listRegistryCredentialsOutput({
    registryName: registry.name,
    resourceGroupName: resourceGroup.name,
});
const registryPassword = registryCredentials.apply(creds => creds.passwords?.[0]?.value ?? "");

// ============================================================================
// 4. Log Analytics Workspace
// ============================================================================
const logAnalytics = new operationalinsights.Workspace(`${prefix}-logs`, {
    resourceGroupName: resourceGroup.name,
    workspaceName: `${prefix}-logs`,
    location: resourceGroup.location,
    sku: { name: "PerGB2018" },
    retentionInDays: 30,
    tags: tags,
});

// Get workspace shared key for Container Apps Environment
const logAnalyticsSharedKeys = operationalinsights.getSharedKeysOutput({
    resourceGroupName: resourceGroup.name,
    workspaceName: logAnalytics.name,
});

// ============================================================================
// 5. Azure Database for PostgreSQL (Flexible Server)
// ============================================================================
const postgresServer = new dbforpostgresql.Server(`${prefix}-psql`, {
    resourceGroupName: resourceGroup.name,
    serverName: `${prefix}-psql`,
    location: resourceGroup.location,
    version: "16",
    administratorLogin: dbAdminUsername,
    administratorLoginPassword: dbAdminPassword,
    sku: {
        name: dbSku,
        tier: "Burstable",
    },
    storage: {
        storageSizeGB: 32,
    },
    backup: {
        backupRetentionDays: 7,
        geoRedundantBackup: "Disabled",
    },
    highAvailability: {
        mode: "Disabled",
    },
    tags: tags,
});

// Create application database
const database = new dbforpostgresql.Database(`${prefix}-db`, {
    resourceGroupName: resourceGroup.name,
    serverName: postgresServer.name,
    databaseName: dbName,
    charset: "UTF8",
    collation: "en_US.utf8",
});

// Allow access from Azure services (Container Apps)
new dbforpostgresql.FirewallRule(`${prefix}-allow-azure`, {
    resourceGroupName: resourceGroup.name,
    serverName: postgresServer.name,
    firewallRuleName: "AllowAllAzureServicesAndResourcesWithinAzureIps",
    startIpAddress: "0.0.0.0",
    endIpAddress: "0.0.0.0",
});

// ============================================================================
// 6. Azure Container Apps Managed Environment
// ============================================================================
const containerAppEnv = new app.ManagedEnvironment(`${prefix}-ca-env`, {
    resourceGroupName: resourceGroup.name,
    environmentName: `${prefix}-ca-env`,
    location: resourceGroup.location,
    appLogsConfiguration: {
        destination: "log-analytics",
        logAnalyticsConfiguration: {
            customerId: logAnalytics.customerId,
            sharedKey: logAnalyticsSharedKeys.apply(keys => keys.primarySharedKey ?? ""),
        },
    },
    tags: tags,
});

// ============================================================================
// 7. Azure Container App (Spring Boot Backend API)
// ============================================================================
// Construct JDBC URL: jdbc:postgresql://<host>:5432/<dbname>?sslmode=require
const jdbcUrl = pulumi.interpolate`jdbc:postgresql://${postgresServer.fullyQualifiedDomainName}:5432/${dbName}?sslmode=require`;

const containerApp = new app.ContainerApp(`${prefix}-api`, {
    resourceGroupName: resourceGroup.name,
    containerAppName: `${prefix}-api`,
    location: resourceGroup.location,
    managedEnvironmentId: containerAppEnv.id,
    configuration: {
        ingress: {
            external: true,             // Publicly reachable via HTTPS
            targetPort: 8080,           // Spring Boot container port
            transport: "Auto",
            allowInsecure: false,
        },
        registries: [
            {
                server: registry.loginServer,
                username: registry.name,
                passwordSecretRef: "acr-password",
            },
        ],
        secrets: [
            { name: "acr-password", value: registryPassword },
            { name: "db-password", value: dbAdminPassword },
            { name: "mapbox-token", value: mapboxToken },
        ],
    },
    template: {
        containers: [
            {
                name: "smartbussing-api",
                image: pulumi.interpolate`${registry.loginServer}/smart-bussing-api:${imageTag}`,
                resources: {
                    cpu: appCpu,
                    memory: appMemory,
                },
                env: [
                    { name: "PROD_DB_URL", value: jdbcUrl },
                    { name: "PROD_DB_USERNAME", value: dbAdminUsername },
                    { name: "PROD_DB_PASSWORD", secretRef: "db-password" },
                    { name: "MAPBOX_API_KEY", secretRef: "mapbox-token" },
                    { name: "SPRING_APPLICATION_NAME", value: "smartbussingAPI" },
                    { name: "SPRING_JPA_HIBERNATE_DDL_AUTO", value: "update" },
                    { name: "SERVER_PORT", value: "8080" },
                ],
            },
        ],
        scale: {
            minReplicas: 1,
            maxReplicas: 5,
        },
    },
    tags: tags,
}, { dependsOn: [database] });

// ============================================================================
// 8. Azure Static Web App (SmartBussing React/Vite Frontend)
// ============================================================================
// Azure Static Web Apps is 100% Free: includes global CDN, free SSL, and custom domains
const staticWebApp = new web.StaticSite(`${prefix}-web`, {
    resourceGroupName: resourceGroup.name,
    name: `${prefix}-web`,
    location: "westus", // Supported region for Static Web Apps
    sku: {
        name: "Free",
        tier: "Free",
    },
    tags: tags,
});

// Retrieve the deployment token to deploy static assets from /smart-bussing-web/dist
const staticWebAppSecrets = web.listStaticSiteSecretsOutput({
    name: staticWebApp.name,
    resourceGroupName: resourceGroup.name,
});
const rawDeploymentToken = staticWebAppSecrets.apply(s => s.properties?.["apiKey"] ?? "");

// ============================================================================
// 9. Cost Management Budget & Safety Alerts (Student Protection)
// ============================================================================
if (alertEmail) {
    const today = new Date();
    const startDate = `${today.getFullYear()}-${String(today.getMonth() + 1).padStart(2, "0")}-01T00:00:00Z`;

    new consumption.Budget(`${prefix}-budget`, {
        scope: resourceGroup.id,
        budgetName: `${prefix}-budget`,
        amount: budgetLimit,
        timeGrain: "Monthly",
        category: "Cost",
        timePeriod: {
            startDate: startDate,
        },
        notifications: {
            "Warning_50": {
                enabled: true,
                operator: "GreaterThanOrEqualTo",
                threshold: 50,
                thresholdType: "Actual",
                contactEmails: [alertEmail],
            },
            "Warning_80": {
                enabled: true,
                operator: "GreaterThanOrEqualTo",
                threshold: 80,
                thresholdType: "Actual",
                contactEmails: [alertEmail],
            },
            "Critical_95": {
                enabled: true,
                operator: "GreaterThanOrEqualTo",
                threshold: 95,
                thresholdType: "Actual",
                contactEmails: [alertEmail],
            },
        },
    }, { parent: resourceGroup });
}

// ============================================================================
// 10. Stack Outputs
// ============================================================================
export const resourceGroupName = resourceGroup.name;
export const registryLoginServer = registry.loginServer;
export const postgresHost = postgresServer.fullyQualifiedDomainName;
export const databaseNameOutput = dbName;
export const backendBaseUrl = pulumi.interpolate`https://${containerApp.configuration.apply(c => c?.ingress?.fqdn ?? "")}`;
export const mobileBackendUrl = pulumi.interpolate`https://${containerApp.configuration.apply(c => c?.ingress?.fqdn ?? "")}/api/v1`;
export const webAppUrl = pulumi.interpolate`https://${staticWebApp.defaultHostname}`;
export const webDeploymentToken = pulumi.secret(rawDeploymentToken);