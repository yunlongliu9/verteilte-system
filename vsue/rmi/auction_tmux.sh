#!/bin/bash

if [ "$#" -ne 2 ]
then
    echo "Error: Not enough arguments."
    echo "Usage: ./auction_tmux.sh <idm_username> <vs_dir>. "
    echo "Example: ./auction_tmux.sh ag92ozin '~/verteilte-systeme-gruppe-04'"
    exit 1
fi

USER=$1
MW_DIR=$2

HOSTS=("$USER@cip2a1.cip.cs.fau.de" "$USER@cip1e3.cip.cs.fau.de" "$USER@cip1e6.cip.cs.fau.de" "$USER@cip1e7.cip.cs.fau.de")
COMMANDS=(
    "cd $MW_DIR && ./start_rmi_server.sh"
    "cd $MW_DIR && ./start_rmi_client.sh $USER cip2a1.cip.cs.fau.de 11111"
    "cd $MW_DIR && ./start_rmi_client.sh $USER cip2a1.cip.cs.fau.de 11111"
    "cd $MW_DIR && ./start_rmi_client.sh $USER cip2a1.cip.cs.fau.de 11111"
)

tmux new-session -d -s "multi-ssh" "ssh -t ${HOSTS[0]} '${COMMANDS[0]}; bash'"
sleep 3
tmux split-window -h "ssh -t ${HOSTS[1]} '${COMMANDS[1]}; bash'"
tmux select-pane -t 0
tmux split-window -v "ssh -t ${HOSTS[2]} '${COMMANDS[2]}; bash'"
tmux select-pane -t 2
tmux split-window -v "ssh -t ${HOSTS[3]} '${COMMANDS[3]}; bash'"

tmux select-pane -t 2
tmux attach-session -t "multi-ssh"