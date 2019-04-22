from mininet.topo import Topo
from mininet.net import Mininet
from mininet.node import CPULimitedHost, OVSController, Node, Host, OVSSwitch, OVSKernelSwitch, Controller, RemoteController, DefaultController
from mininet.link import TCLink
from mininet.util import dumpNodeConnections
from mininet.log import setLogLevel

class DoubleSwitchTopo(Topo):
	def build( self, n=2 ):
		s = self.addSwitch('s1')
		t = self.addSwitch('s2')
		a = self.addHost('a')
		b = self.addHost('b')
		e = self.addHost('e')
		c = self.addHost('c')
		d = self.addHost('d')
		u = self.addHost('u')
		self.addLink(a, s, bw=5, delay='3ms', loss=2, max_queue_size=300)
		self.addLink(b, s, bw=5, delay='3ms', loss=2, max_queue_size=300)
		self.addLink(e, s, bw=5, delay='3ms', loss=2, max_queue_size=300)
		self.addLink(c, t, bw=5, delay='3ms', loss=2, max_queue_size=300)
		self.addLink(d, t, bw=5, delay='3ms', loss=2, max_queue_size=300)
		self.addLink(u, t, bw=5, delay='3ms', loss=2, max_queue_size=300)
		self.addLink(s, t, bw=15, delay='2ms')


#Create network and run simple performance test
topo = DoubleSwitchTopo(n = 6)
net = Mininet(topo = topo, switch = OVSKernelSwitch, 
            	controller = DefaultController, host = CPULimitedHost, link = TCLink )
net.start()

print "Dumping host connections"
dumpNodeConnections( net.hosts )

host = net.get('a')
print "Host a : IP = %s MAC Address : %s"%(host.IP(), host.MAC())

host = net.get('b')
print "Host b : IP = %s MAC Address : %s"%(host.IP(), host.MAC())

host = net.get('e')
print "Host e : IP = %s MAC Address : %s"%(host.IP(), host.MAC())

host = net.get('c')
print "Host c : IP = %s MAC Address : %s"%(host.IP(), host.MAC())

host = net.get('d')
print "Host d: IP = %s MAC Address : %s"%(host.IP(), host.MAC())

host = net.get('u')
print "Host u : IP = %s MAC Address : %s"%(host.IP(), host.MAC())

print "Testing network connectivity"
net.pingAll()
net.stop()