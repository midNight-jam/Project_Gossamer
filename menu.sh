#!/bin/bash
PS3='----------Please enter your choice: ----------  '
options=("Read File " "Write File" "Run Ping" "Quit")
select opt in "${options[@]}"
do
    case $opt in
        "read")
            echo "you chose Read "
		echo "enter the file name to read"
		read fileName
		echo "Youe entered : "$fileName 
            ;;
        "write")
            echo "you chose Write "
		echo "enter the absolute file path"
		read filePath
		echo "Youe entered : "$filePath 
            ;;
        "Run Ping")
            echo "you chose Ping "
		export SVR_HOME="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
		echo "** starting from ${SVR_HOME} **"
		echo server home = $SVR_HOME            	
		JAVA_MAIN='gash.router.app.DemoApp'
		JAVA_ARGS="localhost 4168"
		#echo -e "\n** config: ${JAVA_ARGS} **\n"
		# superceded by http://www.oracle.com/technetwork/java/tuning-139912.html
		JAVA_TUNE='-client -Djava.net.preferIPv4Stack=true'
		java ${JAVA_TUNE} -cp .:${SVR_HOME}/lib/'*':${SVR_HOME}/classes ${JAVA_MAIN} ${JAVA_ARGS} 
            ;;
        "Quit")
		echo "Bye ... (^-^)"
            break
            ;;
        *) echo invalid option;;
    esac
done
