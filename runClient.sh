cd bin
clear
rmiregistry $DLMPORT &
RMIREG_PID=$!
java DFSClient $1 $DLMPORT
kill -9 $RMIREG_PID
