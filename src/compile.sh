echo "Compiling lib..."
javac lib/*.java

echo "Compiling client..."
javac client/*.java

echo "Compiling server..."
javac server/*.java

echo "running rmi compiler..."
rmic server.DFSServer
rmic client.DFSClient
