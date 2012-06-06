clear
rmiregistry $DLMPORT &
RMIREG_PID=$!
java play.ServerTest $DLMPORT
kill -9 $RMIREG_PID
