from mininet.topo import Topo
from mininet.net import Mininet
from mininet.node import Node, Host, OVSSwitch, OVSKernelSwitch, Controller, RemoteController, DefaultController
from mininet.cli import CLI
from mininet.log import setLogLevel
import argparse, time

class SimpleTopo( Topo ):
    def __init__( self , **kwargs):
        super(SimpleTopo, self).__init__(**kwargs)
        for key in kwargs:
           if key == 'N': N=kwargs[key]

        h = []		# list of hosts; h[0] will be h1, etc
        s = []		# list of switches

        # add N hosts  h1..hN
        for i in range(1,N+1):
           h.append(self.addHost('h' + str(i)))

        # add N switches s1..sN
        for i in range(1,N+1):
           s.append(self.addSwitch('s' + str(i)))

        # Add links from hi to si
        for i in range(N):
           self.addLink(h[i], s[i])

        # link switches
        for i in range(N-1):
           self.addLink(s[i],s[i+1])

N = 2
SimpleTopo = SimpleTopo(N=N)
setLogLevel('info')
net = Mininet(topo = SimpleTopo, switch = OVSKernelSwitch, 
            controller = DefaultController, autoSetMacs = True
            )
net.start()

for i in range(1, N+1):
  hi = net['h' + str(i)]
  hi.cmd('/usr/sbin/sshd')

h1 = net.get('h1')
h2 = net.get('h2')

p2 = h2.popen('python2 host2.py -i %s > ./host2_output.txt &')
p1 = h1.popen('python2 host1.py -i %s > ./host1_output.txt &' % h2.IP())

time.sleep(30)
f = open('host1_output.txt')
lineno = 1
for line in f.readlines():
    print "%d: %s" % ( lineno, line.strip() )
    lineno += 1
f.close()

CLI( net )
net.stop()
