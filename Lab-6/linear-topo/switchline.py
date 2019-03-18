"""linear topology example:

   h1    h2    h3        hN
   |     |     |         |
   s1----s2----s3--...---sN

N can be set with the -N command-line option; the default value is 4.

"""

from mininet.topo import Topo
from mininet.net import Mininet
from mininet.node import Node, Host, OVSSwitch, OVSKernelSwitch, Controller, RemoteController, DefaultController
from mininet.cli import CLI
from mininet.log import setLogLevel
import argparse

class LineTopo( Topo ):
    "Linear topology example."

    def __init__( self , **kwargs):
        super(LineTopo, self).__init__(**kwargs)
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

def main(**kwargs):
    parser = argparse.ArgumentParser()
    parser.add_argument('-N', '--N', type=int)
    args = parser.parse_args()
    if args.N is None:
        N = 4
    else:
        N = args.N

    ltopo = LineTopo(N=N)
    setLogLevel('info')
    net = Mininet(topo = ltopo, switch = OVSKernelSwitch, 
                controller = DefaultController,
		autoSetMacs = True
                )
    net.start()
    
    for i in range(1, N+1):
       hi = net['h' + str(i)]
       hi.cmd('/usr/sbin/sshd')

    CLI( net)
    net.stop()

main()

