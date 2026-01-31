Run the Scala Spark app from IntelliJ on Windows

Prerequisites
- IntelliJ IDEA with Scala plugin
- Java 11 installed
- sbt installed (for build/compile)
- Docker and the Hadoop cluster from this repo up (for HDFS access):
  docker-compose up -d

Steps
1. Ensure HDFS user exists (we created `/user/hvan`):
   - Use the provided Makefile target or script:
     ```powershell
     make setup-hdfs
     ```
   - Or run manually:
     ```powershell
     docker exec -it namenode bash -c "hdfs dfs -mkdir -p /user/hvan && hdfs dfs -chown hvan:hvan /user/hvan"
     ```

2. Open the project in IntelliJ (open `app/T-Shirts-System-Matcher/src/scala` as a project).

3. Set environment variable (session) in Windows PowerShell before starting IntelliJ or configure Run/Debug configuration:
   ```powershell
   $env:HADOOP_USER_NAME = "hvan"
   ```
   Alternatively, in IntelliJ Run configuration set Environment variable `HADOOP_USER_NAME=hvan`.

4. Confirm `CBIRTShirtScala` has HDFS configured (it sets `spark.hadoop.fs.defaultFS` to `hdfs://namenode:9000`).

5. Create an IntelliJ Run Configuration:
   - Type: `Application`
   - Main class: `CBIRTShirtScala`
   - Use classpath of module: the module created from the sbt project
   - VM options (optional remote debug): `-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005`
   - Program arguments: none
   - Environment variables: `HADOOP_USER_NAME=hvan`

6. Run / Debug
   - Use Run to execute locally (`local[*]` is set in code).
   - Use Debug to attach debugger. If you use the VM option above, IntelliJ will attach automatically when starting in debug mode.

Notes
- The code sets `System.setProperty("HADOOP_USER_NAME","hvan")` at startup to avoid Windows UGI issues.
- Data paths reference HDFS at `hdfs://namenode:9000`; ensure the Hadoop cluster is running and reachable.
- If you prefer an assembly JAR for running outside sbt, run `sbt assembly` (add sbt-assembly plugin) and run `java -jar target/scala-2.13/your-app.jar`.
