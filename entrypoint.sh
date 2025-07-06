#!/bin/bash
set -e

echo "Starting container with NODE_TYPE: $NODE_TYPE"
echo "All arguments passed: $@"

if [ "$NODE_TYPE" = "namenode" ]; then
  # Format HDFS only if not already formatted
  if [ ! -d /hadoop/dfs/name/current ]; then
    echo "Formatting HDFS NameNode..."
    hdfs namenode -format -force -nonInteractive
  fi
  
  # Start NameNode in background to upload files
  hdfs namenode &
  NAMENODE_PID=$!
  
  # Wait for NameNode to be ready and DataNodes to register
  echo "Waiting for NameNode to be ready..."
  for i in {1..30}; do
    if hdfs dfsadmin -report > /dev/null 2>&1; then
      echo "NameNode is ready!"
      break
    fi
    echo "Attempt $i/30: NameNode not ready yet, waiting..."
    sleep 3
  done
  
  # Wait for DataNodes to register and be available
  echo "Waiting for DataNodes to be available..."
  for i in {1..60}; do
    LIVE_NODES=$(hdfs dfsadmin -report 2>/dev/null | grep "Live datanodes" | grep -o '[0-9]\+' | head -1)
    if [ "$LIVE_NODES" ] && [ "$LIVE_NODES" -ge 1 ]; then
      echo "Found $LIVE_NODES live DataNode(s)!"
      break
    fi
    echo "Attempt $i/60: No DataNodes available yet, waiting..."
    sleep 2
  done
  
  # Give DataNodes a bit more time to fully initialize
  echo "Giving DataNodes additional time to fully initialize..."
  sleep 10
  
  # Upload files to HDFS
  echo "Uploading files to HDFS..."
  hadoop fs -mkdir -p /user/root || true
  hadoop fs -put -f /job/bonjour.txt /user/root/bonjour.txt || echo "bonjour.txt already exists or failed to upload"
  hadoop fs -mkdir -p /user/root/miserables || true
  hadoop fs -put -f /job/miserables/* /user/root/miserables/ || echo "miserables files already exist or failed to upload"
  
  # Wait for the NameNode process to complete
  wait $NAMENODE_PID

elif [ "$NODE_TYPE" = "datanode" ]; then
  # Wait a bit for NameNode to be ready
  echo "Waiting for NameNode to be available..."
  sleep 15
  
  # Start DataNode without any arguments (default is regular mode)
  echo "Starting DataNode..."
  exec hdfs datanode
else
  # Default: run whatever is passed
  exec "$@"
fi
