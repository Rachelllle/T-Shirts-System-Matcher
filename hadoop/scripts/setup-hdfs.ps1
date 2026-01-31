# PowerShell script to initialize HDFS user directory
# Usage: ./scripts/setup-hdfs.ps1 -UserName dataUser
# or: ./scripts/setup-hdfs.ps1 (defaults to HADOOP_USER_NAME env var or 'dataUser')

param(
    [string]$UserName = $env:HADOOP_USER_NAME,
    [string]$NameNodeContainer = $env:HADOOP_NAMENODE_CONTAINER
)

if (-not $UserName) {
    $UserName = Read-Host "Enter HDFS username (default: dataUser)"
    if (-not $UserName) {
        $UserName = "dataUser"
    }
}

if (-not $NameNodeContainer) {
    $NameNodeContainer = "namenode"
}

Write-Host "Setting up HDFS for user: $UserName" -ForegroundColor Green
Write-Host "Using NameNode container: $NameNodeContainer" -ForegroundColor DarkGray

try {
    Write-Host "Creating /user/$UserName directory in HDFS..." -ForegroundColor Cyan
    docker exec -it $NameNodeContainer bash -c "hdfs dfs -mkdir -p /user/$UserName/datasets"

    Write-Host "Ingesting /contenue/plain-tshirt-color if present..." -ForegroundColor Cyan
    docker exec -it $NameNodeContainer bash -c "if [ -d /contenue/plain-tshirt-color ] && ! hdfs dfs -test -e /user/$UserName/datasets/plain-tshirt-color; then hdfs dfs -put /contenue/plain-tshirt-color /user/$UserName/datasets; fi"
    
    Write-Host "Ingesting /contenue/tshirts if present..." -ForegroundColor Cyan
    docker exec -it $NameNodeContainer bash -c "if [ -d /contenue/tshirts ] && ! hdfs dfs -test -e /user/$UserName/datasets/tshirts; then hdfs dfs -put /contenue/tshirts /user/$UserName/datasets; fi"
    
    Write-Host "Setting ownership to $UserName..." -ForegroundColor Cyan
    docker exec -it $NameNodeContainer bash -c "hdfs dfs -chown $UserName`:$UserName /user/$UserName"
    
    Write-Host "Setting permissions to 755..." -ForegroundColor Cyan
    docker exec -it $NameNodeContainer bash -c "hdfs dfs -chmod 755 /user/$UserName/datasets"
    
    Write-Host "`nHDFS initialization complete for user: $UserName" -ForegroundColor Green
    Write-Host "HDFS path: /user/$UserName" -ForegroundColor Yellow
}
catch {
    Write-Host "Error: $_" -ForegroundColor Red
    exit 1
}
