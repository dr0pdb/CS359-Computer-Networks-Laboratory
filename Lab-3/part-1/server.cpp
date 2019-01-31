#include <bits/stdc++.h>
#include <stdio.h>
#include <sys/types.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <unistd.h>
#include <string.h> 

using namespace std;

int main(int argc, char const *argv[])
{
	int sockFd, acceptFd, clientLength;
	struct sockaddr_in serverAddress, clientAddress;
	int serverPortNumber = 5000;

	// create socket
	cout<<"Server: Creating socket!\n";	
	sockFd = socket(AF_INET, SOCK_STREAM, 0); 
    if (sockFd < 0) { 
    	cout<<"Server: Socket creating failed!\n";
        exit(0); 
    } 
    else cout<<"Server: Successfully created socket\n";

	bzero(&serverAddress, sizeof(serverAddress)); // set to null.

	// assign IP, PORT 
    serverAddress.sin_family = AF_INET; 
    serverAddress.sin_addr.s_addr = INADDR_ANY; // set to ip of the machine. 
    serverAddress.sin_port = htons(serverPortNumber);

    // bind to the ip and port.
    if ((bind(sockFd, (struct sockaddr*)&serverAddress, sizeof(serverAddress))) != 0) { 
        printf("Server: Socket bind failed!\n"); 
        close(sockFd);
        exit(0); 
    } 
    else printf("Server: Socket successfully binded!\n"); 

    if(listen(sockFd, 5) != 0) {
        cout<<"Server: Listen failed\n";
        close(sockFd);
        exit(0);
    } else cout<<"Server: Listening successfully!\n";

    clientLength = sizeof(clientAddress);
    acceptFd = accept(sockFd, (struct sockaddr*)&clientAddress, (socklen_t*)&clientLength);
    if(acceptFd < 0) {
        cout<<"Server: Accept failure!\n";
        close(sockFd);
        exit(0);
    } else cout<<"Server: Accept successful!\n";

    char message[2000]; // message buffer.

    int message_size;
    while(true) {
    	message_size = 0;
    	bzero(message, sizeof(message));
        read(acceptFd, message, sizeof(message));
        cout<<"Message from client: ";
        printf("%s", message);

    	if(strncmp(message, "exit", 4) == 0) {
    		cout<<"Server: Exiting!\n";
    		break;
    	}
        bzero(message, sizeof(message));
    }

    close(sockFd);
    close(acceptFd);

	return 0;
}