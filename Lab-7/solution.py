from mininet.topo import Topo
from mininet.net import Mininet
from mininet.node import Node, Host, OVSSwitch, OVSKernelSwitch, Controller, RemoteController, DefaultController
from mininet.cli import CLI
from mininet.log import setLogLevel
from mininet.link import TCLink
from mininet.util import dumpNodeConnections

class CustomTopo( Topo ):

    def __init__(self):

        # Initialize topology
        Topo.__init__( self )

        # add hosts
        h1 = self.addHost('h1', ip="10.0.0.10/24", mac="00:00:00:00:00:01", defaultRoute = 'via 10.0.0.2')
        h2 = self.addHost('h2', ip="10.0.2.10/24", mac="00:00:00:00:00:02", defaultRoute = 'via 10.0.2.1')

        # Add router
        r1 = self.addHost('r1')
        r2 = self.addHost('r2')

        # Add links
        self.addLink(r1, h1)
        self.addLink(r1, r2)
        self.addLink(r2, h2)


def main():
    ctopo = CustomTopo()
    setLogLevel('info')
    net = Mininet(topo = ctopo,
            switch = OVSKernelSwitch, 
            controller = DefaultController,
            link = TCLink,
		    autoSetMacs = True)
    
    net.start()

    h1 = net['h1']
    h2 = net['h2']
    r1 = net['r1']
    r2 = net['r2']

    r1.setIP('10.0.0.2/24',intf = 'r1-eth0')
    r1.setIP('10.0.1.1/24',intf = 'r1-eth1')

    r2.setIP('10.0.1.2/24',intf = 'r2-eth0')
    r2.setIP('10.0.2.1/24',intf = 'r2-eth1')

    r1.cmd("ip route add to 10.0.2.0/24 via 10.0.1.2 dev r1-eth1")
    r2.cmd("ip route add to 10.0.0.0/24 via 10.0.1.1 dev r2-eth0")

    r1.cmd("echo 1 > /proc/sys/net/ipv4/ip_forward")
    r2.cmd("echo 1 > /proc/sys/net/ipv4/ip_forward")

    CLI(net)
    dumpNodeConnections(net.hosts)
    net.stop()

main()