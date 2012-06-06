clear
rmiregistry $DLMPORT &
RMIREG_PID=$!
java client.DFSClient $UW21 $DLMPORT
kill -9 $RMIREG_PID
