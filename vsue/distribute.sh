#!/bin/bash

function print_usage() {
    echo "Usage: $0 <simple/fancy/debug> [<path_to_class_files>] [<path_to_hosts_file>]"
    echo "WARNING: This script must be run on a CIP pool computer!"
}

if [[ -z $1 ]]; then
    print_usage
    exit 1
fi

# verify testcase-name
case $1 in
	simple|fancy|debug|simple-try|fancy-try|debug-try)
		;;
	*)
		echo "ERROR: Invalid test scenario name!"
		print_usage
		exit 1
esac

if [[ -z $2 ]]; then
    program_path="$PWD"
elif [[ ! -d $2 ]]; then
    echo "ERROR: Path to class files is not an existing folder!"
    print_usage
    exit 1
else
    # make sure that the program path is absolute
    program_path="$(cd "$2" ; pwd)"
fi
if [[ -z $3 ]]; then
    path_to_configs="$program_path"
elif [[ ! -d $3 ]]; then
    echo "ERROR: Path to config files is not an existing folder!"
    print_usage
    exit 1
else
    # make sure that the config path is absolute
    path_to_configs="$(cd "$3" ; pwd)"
fi
java_cmd=("java" "-cp" "${program_path}" "-ea")
start_class="vsue.lamport.VSLamportTest"
log_name="distlock"
base_port=$(( 14000 + (UID * 337 % 977) * 13 ))

######################################################
##########                                 ###########
########## NO USER SERVICEABLE PARTS BELOW ###########
##########                                 ###########
######################################################

if [[ ! -f "${path_to_configs}/my_hosts" ]]; then
	echo "ERROR: my_hosts not found."
	echo "You need to copy those files to your current working directory."
	print_usage
	exit 1
fi

classfile="${program_path}/$(echo "$start_class" | sed -e "s?\\.?/?g").class"
if [[ ! -f "$classfile" ]]; then
	echo "ERROR: Please make sure the class file '$classfile' exists."
	print_usage
	exit 1
fi

total_hosts=0
host_count=0
group_list=""
hosts=()

while read -r next_host || [ -n "$next_host" ]; do
	test -z "$next_host" && continue;
	
	group_list="$group_list,$next_host[$((base_port+total_hosts))]"

	total_hosts=$((total_hosts+1))
	hosts+=("$next_host")
done <"${path_to_configs}/my_hosts"

if [ "$total_hosts" -le 1 -o "$total_hosts" -gt 10 ]; then
	echo "Found $total_hosts host entries, but must be between 2 and 10."
	exit 1
fi

cat >__screenrc__ <<EOF
startup_message off
zombie kc
logfile ${log_name}-log.%n
hardstatus alwayslastline "%-Lw%{= bW}%50>%n%f* %t%{-}%+Lw%<"
EOF

for host in "${hosts[@]}" ; do
    cd_parts=("cd" "$program_path")
    java_parts=("${java_cmd[@]}" "-Dconfigs_path=${path_to_configs}" "$start_class" "$host_count" "$base_port" "$1")

    echo "screen -t vs-${host} -L ssh -t $host /bin/bash -c \"\
$(printf '%q ' "${cd_parts[@]}"); \
$(printf '%q ' "${java_parts[@]}")\"" >>__screenrc__
    host_count=$((host_count+1))
done

rm -f "${log_name}-log."{0..9}
screen -S locktest -c __screenrc__
rm -f __screenrc__
