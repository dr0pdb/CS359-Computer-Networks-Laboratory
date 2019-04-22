import os
import subprocess
from time import sleep
from multiprocessing import Process
from mininet.node import OVSController, Node, Host, OVSSwitch, OVSKernelSwitch, Controller, RemoteController, DefaultController
from mininet.topo import Topo 
from mininet.net import Mininet 
from mininet.util import dumpNodeConnections
from mininet.log import setLogLevel
import thread

class SingleSwitchTopo(Topo):
  "Single switch connected to n hosts."
  def build(self, n=2):
    switch = self.addSwitch('s1')
    for h in range(n):
      host =self.addHost('h%s'%(h +1))
      self.addLink(host, switch)


def t1(server):
  t = server.cmd('iperf -s -p 5111 -i 2 > output')


topo = SingleSwitchTopo(n=2)
net = Mininet(topo = topo, switch = OVSKernelSwitch, 
            	controller = DefaultController)                                                                        
net.start()

print "Dumping host connections"
dumpNodeConnections(net.hosts)

print "Testing network connectivity"
net.pingAll()

server = net.get('h1')
client = net.get('h2')

print "Starting Server"
t = Process(target = t1, args = (server, ))
t.start()
print "Server Started\n"
sleep(1)

print "Making request from client"
client.cmd('iperf -c %s -p 5111 -t 20 > results.txt'%(server.IP()))
print "Requesting Done"

client.cmd('kill %iperf')
t.terminate()

print "Server Output:"
f = open('output')
for line in f.readlines():
  print line,
f.close()

f = open('results.txt', 'r')
s = ''
for i in range(7):
  s = f.readline(1024)
s = s.split(' ')
print("\n\nThroughput = " + s[-2] + " " + s[-1])
os.remove("results.txt")
os.remove("output")
net.stop()