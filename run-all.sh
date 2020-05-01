#!/bin/sh

tmux new-session -d -s radish-servers
tmux send-keys 'cd registry-server' 'C-m'
tmux send-keys '../mvnw spring-boot:run' 'C-m'
tmux split-window -h 
tmux send-keys 'cd discovery-server' 'C-m'
tmux send-keys '../mvnw spring-boot:run' 'C-m'
tmux split-window -v -t 1
tmux send-keys 'cd apigateway-server' 'C-m'
tmux send-keys '../mvnw spring-boot:run' 'C-m'
tmux split-window -v -t 3
tmux -2 attach-session -t radish-servers
