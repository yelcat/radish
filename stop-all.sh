#!/bin/sh

for SVR in 'ApiGatewayServer' 'RegistryServer' 'DiscoveryServer'
do
    PID=$( ps -ef | grep java | grep $SVR | awk 'NF {print $2}' )
    if [ "" !=  "$PID" ]; then
        kill $PID
        echo "Stopped server $SVR pid $PID !"
    fi
done
tmux kill-session -t radish-servers
