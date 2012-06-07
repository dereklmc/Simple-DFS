clear
rmiregistry $DLMPORT &
RMIREG_PID=$!
java DFSClient $UW21 $DLMPORT
kill -9 $RMIREG_PID
