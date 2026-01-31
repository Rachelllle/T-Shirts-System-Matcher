# HDFS Setup Instructions

## Overview
Before running your Spark/Scala application, you need to initialize an HDFS user directory. This script automates that process.

## Quick Start

### On Windows (PowerShell):
```powershell
# Set your username (optional, defaults to 'dataUser')
$env:HADOOP_USER_NAME = "dataUser"

# Run the setup script
.\scripts\setup-hdfs.ps1

# Or specify user directly
.\scripts\setup-hdfs.ps1 -UserName dataUser
```

### On Linux/Mac (Bash):
```bash
# Set your username (optional, defaults to 'dataUser')
export HADOOP_USER_NAME=dataUser

# Run the setup script
./scripts/setup-hdfs.sh

# Or specify user directly
./scripts/setup-hdfs.sh dataUser
```

## Prerequisites
- Docker and docker-compose running with Hadoop services up:
  ```bash
  docker-compose up -d
  ```
- `namenode` container must be running and accessible

## What the Script Does
1. Creates `/user/{USERNAME}` directory in HDFS
2. Sets ownership to `{USERNAME}:{USERNAME}`
3. Sets permissions to 755 (readable by all, writable by owner)

## Environment Variable
You can set `HADOOP_USER_NAME` and the scripts will use it:

**PowerShell:**
```powershell
$env:HADOOP_USER_NAME = "myuser"
.\scripts\setup-hdfs.ps1
```

**Bash:**
```bash
export HADOOP_USER_NAME=myuser
./scripts/setup-hdfs.sh
```

## Manual Setup (if scripts don't work)
```bash
docker exec -it namenode bash -c "hdfs dfs -mkdir -p /user/{USERNAME} && hdfs dfs -chown {USERNAME}:{USERNAME} /user/{USERNAME}"
```

Replace `{USERNAME}` with your desired user.

## Verify Setup
```bash
docker exec -it namenode bash -c "hdfs dfs -ls /user/"
```

You should see your user's directory listed.
