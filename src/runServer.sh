clear
rmiregistry $DLMPORT &
RMIREG_PID=$!
java server.DFSServer $DLMPORT
kill -9 $RMIREG_PID
