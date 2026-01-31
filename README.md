# T-Shirts-System-Matcher
T-Shirts System Matcher is a smart application that matches t-shirts to users based on their preferences, style, and fit. Users provide their preferences - such as color, size, fabric, or occasion - and the system suggests the best match. Images of the t-shirts are provided by the user, allowing the system to recommend from a custom selection.

## Prerequisites
- Docker Desktop
- Java 17 (recommended for Spark 3.5.x)
- sbt (for building the Scala JAR)

## Install / Setup (Docker)
1) Build the Scala JAR
```bash
cd src/scala
sbt clean package
```
This should create `src/scala/target/scala-2.12/scala_2.12-0.1.0-SNAPSHOT.jar`.

2) Start the Hadoop services
```bash
cd ../..
docker compose up -d namenode datanode resourcemanager nodemanager historyserver
```

3) Load datasets into HDFS (optional, but recommended)
- On Windows (host):
```powershell
powershell -ExecutionPolicy Bypass -File hadoop/scripts/setup-hdfs.ps1
```
- On Linux/Mac (host):
```bash
bash hadoop/scripts/setup-hdfs.sh
```

4) Run the Spark app in Docker
```bash
docker compose up -d spark-app
```

## Testing / Verification
1) Check HDFS is up
```bash
docker exec -it namenode hdfs dfs -ls /user/dataUser/datasets
```
You should see `plain-tshirt-color` and `tshirts` if data was loaded.

2) Check Spark app logs
```bash
docker logs -f spark-app
```
Look for the image count, feature rows, and "Query path" output.

## Host Run (IntelliJ / local Spark)
If you run Spark on the host and access HDFS in Docker, the host must resolve the DataNode hostname:
1) Add to `C:\Windows\System32\drivers\etc\hosts`:
```
127.0.0.1 datanode
```
2) Restart containers:
```bash
docker compose down
docker compose up -d
```

## Notes
- `docker compose down -v` will delete HDFS data (use only if you want a clean reset).
