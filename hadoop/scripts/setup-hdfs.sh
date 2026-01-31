#!/bin/bash
# Bash script to initialize HDFS user directory
# Usage: ./scripts/setup-hdfs.sh dataUser
# or: ./scripts/setup-hdfs.sh (defaults to HADOOP_USER_NAME env var or 'dataUser')

USERNAME="${1:-${HADOOP_USER_NAME:-dataUser}}"
NAMENODE_CONTAINER="${HADOOP_NAMENODE_CONTAINER:-namenode}"

run_hdfs() {
  if command -v docker >/dev/null 2>&1; then
    docker exec -it "$NAMENODE_CONTAINER" bash -c "$1"
  else
    bash -c "$1"
  fi
}

echo "Setting up HDFS for user: $USERNAME"
echo "Using NameNode container: $NAMENODE_CONTAINER"

{
    echo "Creating /user/$USERNAME/datasets directory in HDFS..."
    run_hdfs "until hdfs dfs -ls / >/dev/null 2>&1; do echo 'Waiting for HDFS...'; sleep 5; done"
    run_hdfs "hdfs dfs -mkdir -p /user/$USERNAME/datasets"

    echo "Ingesting /contenue/plain-tshirt-color if present..."
    run_hdfs "if [ -d /contenue/plain-tshirt-color ] && ! hdfs dfs -test -e /user/$USERNAME/datasets/plain-tshirt-color; then hdfs dfs -put /contenue/plain-tshirt-color /user/$USERNAME/datasets; fi"

    echo "Ingesting /contenue/tshirts if present..."
    run_hdfs "if [ -d /contenue/tshirts ] && ! hdfs dfs -test -e /user/$USERNAME/datasets/tshirts; then hdfs dfs -put /contenue/tshirts /user/$USERNAME/datasets; fi"
    
    echo "Setting ownership to $USERNAME..."
    run_hdfs "hdfs dfs -chown $USERNAME:$USERNAME /user/$USERNAME/datasets"
    
    echo "Setting permissions to 755..."
    run_hdfs "hdfs dfs -chmod 755 /user/$USERNAME/datasets"
    
    echo ""
    echo "HDFS initialization complete for user: $USERNAME"
    echo "HDFS path: /user/$USERNAME"
} || {
    echo "Error during HDFS setup"
    exit 1
}
