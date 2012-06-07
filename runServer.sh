cd bin
clear
rmiregistry $DLMPORT &
RMIREG_PID=$!
java DFSServer $DLMPORT
kill -9 $RMIREG_PID
