import socket, optparse, sys

sys.stdout = open('host1_output.txt', 'w')

parser = optparse.OptionParser()
parser.add_option('-i', dest='ip', default='127.0.0.1')
parser.add_option('-p', dest='port', type='int', default=5111)
parser.add_option('-m', dest='msg', default='Message from host 1 to host 2')
(options, args) = parser.parse_args()

s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
s.sendto(options.msg, (options.ip, options.port))