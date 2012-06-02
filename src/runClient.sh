rmiregistry $DLMPORT &
RMIREG_PID=$!
java play.ClientTest $UW21 $DLMPORT
kill -9 $RMIREG_PID
