#!/usr/bin/env bash

# Including functions only
source ./deploy_all.sh include-only

init_ansible_playbook

echo -e "\nChoose one of the options [ 1-2-3 ] :"
echo "1. Restart Exareme"
echo "2. Restart Portainer"
echo -e "3. Restart Exareme and Portainer\n"

read answer

while true
do
    if [[ "${answer}" == "1" ]]; then
        # Restart Exareme services
        stopService 1
        break
    elif [[ "${answer}" == "2" ]]; then
		# Restart Portainer service
		stopService 2
        break
    elif [[ "${answer}" == "3" ]]; then
		# Restart Exareme and Portainer services
		stopService 3
        break
    else
        echo "$answer is not a valid answer! Try again.. [ 1-2-3 ]"
        read answer
    fi
done

# If status code != 0 an error has occurred
if [[ ${ansible_playbook_code} -ne 0 ]]; then
    echo "Playbook \"Stop-Services.yaml\" exited with error." >&2
    exit 1
fi

if [[ "${answer}" == "1" ]]; then
    echo -e "\nStarting Exareme services..."

	ansible_playbook_start=${ansible_playbook}"../Start-Exareme.yaml --skip-tags portainer"
	${ansible_playbook_start}
	ansible_playbook_code=$?
	
elif [[ "${answer}" == "2" || "${answer}" == "3" ]]; then
    Portainer
fi

#If status code != 0 an error has occurred
if [[ ${ansible_playbook_code} -ne 0 ]]; then
    echo "Playbook \"Start-Exareme.yaml\" exited with error." >&2
    exit 1
fi
