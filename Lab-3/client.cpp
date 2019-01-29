#include <bits/stdc++.h>
#include <sys/types.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <arpa/inet.h>
#include <stdlib.h>
#include <unistd.h>
#include <string.h>
#include <netdb.h>
#include <sys/uio.h>
#include <sys/time.h>
#include <sys/wait.h>
#include <fcntl.h>
#include <fstream>

using namespace std;

int main(int argc, char const *argv[])
{
	int sockFd;
	struct sockaddr_in serverAddress;
	string serverIp("127.0.0.1");
	int serverPortNumber = 5000;

	if(argc != 3) {
		cout<<"Client: No Server IP and Port provided! Using default ones!\n";
	} else {
		serverIp = argv[1];
		serverPortNumber = atoi(argv[2]);
	}

	// create socket
	cout<<"Client: Creating socket!\n";	
	sockFd = socket(AF_INET, SOCK_STREAM, 0); 
    if (sockFd == -1) { 
    	cout<<"Client: Socket creating failed!\n";
        exit(0); 
    } 
    else cout<<"Client: Successfully created socket\n";

	bzero(&serverAddress, sizeof(serverAddress)); // set to null.

	// assign IP, PORT 
    serverAddress.sin_family = AF_INET; 
    serverAddress.sin_addr.s_addr = inet_addr(serverIp.c_str()); 
    serverAddress.sin_port = htons(serverPortNumber);

    // connect the client socket to server socket 
    if (connect(sockFd, (struct sockaddr*)&serverAddress, sizeof(serverAddress)) != 0) { 
        cout<<"Client: Connection with the server failed!\n";
        close(sockFd);
        exit(0);
    }
    else cout<<"Client: Connected to the server..\n";

    char message[2000]; // message buffer.

    int message_size;
    while(true) {
    	message_size = 0;
    	bzero(message, sizeof(message));
    	cout<<"Enter message: ";
    	message[message_size++] = getchar();
    	while(message[message_size-1] != '\n') {
    		message[message_size++] = getchar();
    		if(message_size == 2000) {
    			cout<<"You cannot enter a message longer than 2000 characters\n";
    			close(sockFd);
    			exit(0);
    		}
    	}
    	write(sockFd, message, sizeof(message));
    	if(strncmp(message, "exit", 4) == 0) {
    		cout<<"Client: Exiting!\n";
    		break;
    	}
    	bzero(message, sizeof(message));
    }

    close(sockFd);

	return 0;
}