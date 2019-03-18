import socket, optparse, threading, sys

sys.stdout = open('host2_output.txt', 'w')

parser = optparse.OptionParser()
parser.add_option('-i', dest='ip', default='')
parser.add_option('-p', dest='port', type='int', default=5111)
(options, args) = parser.parse_args()

s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
s.bind( (options.ip, options.port))

def handle_client_connection(client_socket):
    request = client_socket.recv(1024)
    print 'Received {}'.format(request)
    client_socket.send('ACK!')
    client_socket.close()

while True:
	client_sock, address = server.accept()
	client_handler = threading.Thread(
		target=handle_client_connection, 
		args=(client_sock,)
		)
	client_handler.start()