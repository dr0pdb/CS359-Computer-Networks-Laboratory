import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.PriorityQueue;
import java.util.Scanner;
import java.util.Map;

public class CUBIC {
    final static int SENT_BY_SOURCE = 1;
    final static int RECIEVED_AT_SWITCH = 2;
    final static int DISPATCH_FROM_SWITCH = 3;
    final static int RECIEVED_AT_SINK = 4;
    final static int SENT_BY_SINK = 5;
    final static int RECIEVED_AT_SWITCH_FROM_SINK =6 ;
    final static int DISPATCH_FROM_SWITCH_SINK = 7;
    final static int RECIEVED_AT_SOURCE = 8 ;
    final static int TIMEOUT = 9;

    final static int NUMBER_OF_SOURCE = 1;
    final static int SRC2SINK_QUEUE_SIZE = 10;
    final static int SINK2SRC_QUEUE_SIZE = 8;

    static int glb_source_id = 0;
    static int glb_packet_id = 0;
    static int glb_event_id = 0;
    static double glb_time = 0;
    static int time_of_simulation = 0;
    static int curr_que_size_src2sink = 0;
    static int curr_que_size_sink2src = 0;
    static PriorityQueue<Event> event_queue = new PriorityQueue<>();

    public static void main(String[] args) {
        for (int u = 0; u < 1; u++) {
            glb_source_id =0; glb_time=0; glb_packet_id=0;
            glb_source_id =0; curr_que_size_src2sink=0;
            event_queue.clear();

            int[] mean_packet_sending_rate = new int[NUMBER_OF_SOURCE];
            double time_to_process = 0;
            double[] d_source_to_switch = new double[NUMBER_OF_SOURCE];
            double d_time_to_sink = 0.5 ;
            double[] prev_arrival_time = new double[NUMBER_OF_SOURCE];
            int[] packet_dropped = new int[NUMBER_OF_SOURCE];

            time_of_simulation = 10000;
    
            Source[] src = new Source[NUMBER_OF_SOURCE];
            Link[] src_2_switch = new Link[NUMBER_OF_SOURCE];
            Link[] switch_2_src = new Link[NUMBER_OF_SOURCE];
            Packet[] start_pack = new Packet[NUMBER_OF_SOURCE];
            ArrayList<Packet> produced_packet = new ArrayList<>();
            ArrayList<Packet> recieved_packets = new ArrayList<>();
            ArrayList<Integer> curr_window_size = new ArrayList<>();   

            Switch swtch1 = new Switch(time_to_process, d_time_to_sink);

            Link switch_2_sink = new Link();
            Link sink_2_switch = new Link();
       
            TCP tcp = new TCP(1, 100.0);    

            //initilaise all the values 
            for(int l=0; l<NUMBER_OF_SOURCE; ++l) {
                mean_packet_sending_rate[l] = 2 ;
                prev_arrival_time[l] = 0.0;
                d_source_to_switch[l] = 0.9;
                src[l] = new Source(mean_packet_sending_rate[l], d_source_to_switch[l]);
                start_pack[l] = new Packet(src[l].id, 0.0);
                produced_packet.add(start_pack[l]);

                src_2_switch[l] = new Link();
                switch_2_src[l] = new Link();
            }

            //send packets.
            tcp.sendPackets(produced_packet, 0.0);

            // System.out.println(prev_arrival_time);
            while (glb_time <= time_of_simulation) {

                if (!event_queue.isEmpty()) {
                    Event curr_process = event_queue.poll();
                    int curr_process_packet_src_id = curr_process.packet.source_id;
                    curr_process_packet_src_id--;
                    
                    // tcp cubic.
                    while(glb_time < curr_process.scheduled_time) {
                        tcp.updateWindowSize(glb_time);

                        glb_time++;
                    }

                    switch (curr_process.event_type) {
                        case SENT_BY_SOURCE:
                            if (src_2_switch[curr_process_packet_src_id].curr_packet == null) {
                                curr_process.scheduled_time += (src[curr_process_packet_src_id].time_to_switch);
                                src_2_switch[curr_process_packet_src_id].curr_packet = curr_process.packet;
                                src_2_switch[curr_process_packet_src_id].checkpoint_reach = curr_process.scheduled_time;
                                curr_process.event_type = RECIEVED_AT_SWITCH;
                                event_queue.add(curr_process);
                            } else {
                                curr_process.scheduled_time = src_2_switch[curr_process_packet_src_id].checkpoint_reach;
                                src_2_switch[curr_process_packet_src_id].checkpoint_reach += src[curr_process_packet_src_id].time_to_switch;
                                event_queue.add(curr_process);
                            }
                            break;
                        case RECIEVED_AT_SWITCH:
                            if(curr_que_size_src2sink == SRC2SINK_QUEUE_SIZE) {
                                packet_dropped[curr_process_packet_src_id]++;
                            } else {
                                curr_que_size_src2sink++;
                                curr_process.scheduled_time += swtch1.time_to_process;
                                curr_process.event_type = DISPATCH_FROM_SWITCH;
                                event_queue.add(curr_process);
                            }
                            src_2_switch[curr_process_packet_src_id].curr_packet = null;                
                            break;
                        case DISPATCH_FROM_SWITCH:
                            if (switch_2_sink.curr_packet == null) {
                                curr_que_size_src2sink--;
                                curr_que_size_src2sink=(curr_que_size_src2sink<0)?0:curr_que_size_src2sink;
                                curr_process.scheduled_time += swtch1.time_to_sink;
                                switch_2_sink.curr_packet = curr_process.packet;
                                switch_2_sink.checkpoint_reach = curr_process.scheduled_time;
                                curr_process.event_type = RECIEVED_AT_SINK;
                                event_queue.add(curr_process);
                            } else {
                                curr_process.scheduled_time = switch_2_sink.checkpoint_reach;
                                switch_2_sink.checkpoint_reach += swtch1.time_to_sink;
                                event_queue.add(curr_process);
                            }
                            break;
                        case RECIEVED_AT_SINK:
                            switch_2_sink.curr_packet = null;
                            curr_process.packet.packet_reach_time = glb_time;
                            recieved_packets.add(curr_process.packet);

                            tcp.rcv_window.put(curr_process.packet,true);
                            int next_unacked = tcp.lastUnackedPacket();
                            if(next_unacked != 10000000) {
                                curr_process.ack_number = next_unacked;
                            } else {
                                curr_process.ack_number = -1;
                            }
                            curr_process.event_type = SENT_BY_SINK;
                            event_queue.add(curr_process);
                            break;
                        case SENT_BY_SINK:
                            if(sink_2_switch.curr_packet == null) {
                                curr_process.scheduled_time += swtch1.time_to_sink;
                                sink_2_switch.curr_packet = curr_process.packet;
                                sink_2_switch.checkpoint_reach = curr_process.scheduled_time;
                                curr_process.event_type= RECIEVED_AT_SWITCH_FROM_SINK;
                            } else {
                                curr_process.scheduled_time = sink_2_switch.checkpoint_reach;
                                sink_2_switch.checkpoint_reach+=swtch1.time_to_sink;
                            }
                            event_queue.add(curr_process);
                            break;
                        case RECIEVED_AT_SWITCH_FROM_SINK:
                            if(curr_que_size_sink2src != SINK2SRC_QUEUE_SIZE) {
                                curr_que_size_sink2src++;
                                curr_process.scheduled_time+=swtch1.time_to_process;
                                curr_process.event_type = DISPATCH_FROM_SWITCH_SINK;
                                event_queue.add(curr_process);
                            }  
                            sink_2_switch.curr_packet = null;
                            break;
                        case DISPATCH_FROM_SWITCH_SINK:
                            if(switch_2_src[curr_process_packet_src_id].curr_packet == null) {
                                curr_que_size_sink2src--;
                                curr_que_size_sink2src=(curr_que_size_sink2src<0)?0:curr_que_size_sink2src;
                                curr_process.scheduled_time+=src[curr_process_packet_src_id].time_to_switch;
                                switch_2_src[curr_process_packet_src_id].curr_packet = curr_process.packet ;
                                switch_2_src[curr_process_packet_src_id].checkpoint_reach = curr_process.scheduled_time;
                                curr_process.event_type= RECIEVED_AT_SOURCE;
                            } else {
                                curr_process.scheduled_time = switch_2_src[curr_process_packet_src_id].checkpoint_reach;
                                switch_2_src[curr_process_packet_src_id].checkpoint_reach+=src[curr_process_packet_src_id].time_to_switch;
                            }
                            event_queue.add(curr_process);
                            break;
                        case RECIEVED_AT_SOURCE:
                            switch_2_src[curr_process_packet_src_id].curr_packet = null;
                            tcp.src_ack_window.put(curr_process.packet, true);
                            int temp=-1;
                            if(curr_process.ack_number != -1) {
                                //  System.out.println(" "+curr_process.ack_number);
                                if(tcp.src_ack_dups_trace.containsKey(curr_process.ack_number)) {
                                    temp = tcp.src_ack_dups_trace.get(curr_process.ack_number);
                                    tcp.window_size_before_last_reduction = tcp.window_size;
                                }
                            }

                            //3 dups
                            if(temp == 2) {
                                if(tcp.window_size>1) {
                                    tcp.last_window_reduction_time = (int)curr_process.scheduled_time;
                                    tcp.updateWindowSize(glb_time);
                                }
                                tcp.isFullWindowSent = true;
                            } else {
                                if(tcp.src_ack_dups_trace.containsKey(curr_process.ack_number)) {
                                    tcp.src_ack_dups_trace.put(curr_process.ack_number, ++temp);
                                }
                                if( tcp.rcvWindowAllAcked() ) {
                                    curr_window_size.add(tcp.window_size);
                                    tcp.isFullWindowSent = true;
                                    tcp.packet_window_start+=tcp.window_size;
                                    tcp.updateWindowSize(glb_time);                   
                                    if(tcp.timeout/2 < tcp.min_timeout ) {
                                        tcp.timeout = tcp.min_timeout;
                                    } else {
                                        tcp.timeout/=2;
                                    } 
                                }
                            }
                            break;
                        case TIMEOUT:
                            if( !tcp.src_ack_window.get(curr_process.packet) ) {
                                tcp.last_window_reduction_time = (int)curr_process.scheduled_time;
                                tcp.window_size = 1;
                                tcp.isFullWindowSent = true;
                                if(tcp.timeout*10 > tcp.max_timeout ) {
                                    tcp.timeout = tcp.max_timeout;
                                } else {
                                    tcp.timeout*=10;
                                }   
                            }
                            break;
                        }
                }

                addNewPackets(produced_packet, src, prev_arrival_time);
                if(tcp.isFullWindowSent) {
                    tcp.sendPackets(produced_packet, glb_time);
                }
            }


            for(Integer val : curr_window_size) {
                System.out.println(""+val);
            }

            // int[] n = new int[NUMBER_OF_SOURCE];
          
            // for (Packet pc : recieved_packets) {
            //     ++n[pc.source_id-1];
               
            // }

            // for(int i=0;i<NUMBER_OF_SOURCE;++i) {
            //     System.out.print((double)packet_dropped[i]/(n[i]+packet_dropped[i] )+",");
            // }
            // System.out.println();
        }
    }

    public static void addNewPackets(ArrayList<Packet> sent_packet, Source[]src, double[] prev_arrival_time) {
        for(int i=0; i<NUMBER_OF_SOURCE; ++i) {
            double delta_gap = -(Math.log(1.0-Math.random()))/src[i].mean_packet_sending_rate;
            if(prev_arrival_time[i] + delta_gap < time_of_simulation) {
                prev_arrival_time[i] += delta_gap;
                Packet newPack = new Packet(src[i].id, prev_arrival_time[i]);
                sent_packet.add(newPack);
            }
        }
    }

    static class Source {
        int id;
        int mean_packet_sending_rate;
        double time_to_switch;

        public Source(int mean_packet_sending_rate, double time_to_switch) {
            this.id = ++glb_source_id;
            this.mean_packet_sending_rate = mean_packet_sending_rate;
            this.time_to_switch = time_to_switch;
        }
    }

    static class Switch {
        double time_to_process;
        double time_to_sink;

        public Switch(double time_to_process, double time_to_sink) {
            this.time_to_process = time_to_process;
            this.time_to_sink = time_to_sink;
        }
    }

    static class Packet {
        int source_id;
        double time_stamp;
        int packet_id;
        double packet_reach_time;

        public Packet(int source_id, double time) {
            this.source_id = source_id;
            this.time_stamp = time;
            packet_id = ++glb_packet_id;
        }

        @Override
        public String toString() {
            return "packet id-> " + this.packet_id + "time_stamp -> " + this.time_stamp;
        }
    }

    static class Event implements Comparable<Event> {
        int event_id;
        int event_type;
        double scheduled_time;
        Packet packet;
        int ack_number;

        public Event(int event_type, Packet packet, double scheduled_time) {
            this.event_id = ++glb_event_id;
            this.event_type = event_type;
            this.packet = packet;
            this.scheduled_time = scheduled_time;
        }

        @Override
        public int compareTo(Event e) {
            if (this.scheduled_time < e.scheduled_time) {
                return -1;
            } else if (this.scheduled_time > e.scheduled_time) {
                return 1;
            } else if (this.event_type > e.event_type) {
                return -1;
            } else if (this.event_type < e.event_type) {
                 return 1;   
            }  else if(this.packet.packet_id > e.packet.packet_id) {
                return 1;
            } else {
                return -1;
            }
        }

        @Override
        public String toString() {
            return "Event id -> " + this.event_id + "Event type -> " + this.event_type + " Scheduled time -> "
                    + this.scheduled_time + this.packet.toString();
        }
    }

    static class Link {
        Packet curr_packet;
        double checkpoint_reach;
    }

    static class TCP {
        int window_size, 
            window_size_before_last_reduction,
            beta,
            scaling_constant_c,
            last_window_reduction_time;

        HashMap<Packet, Boolean> src_ack_window;
        HashMap<Integer, Integer> src_ack_dups_trace;
        HashMap<Packet,Boolean> rcv_window;

        boolean isFullWindowSent;
        double min_timeout = 100.0;
        double max_timeout = 10000000.0;
        double timeout;

        int packet_window_start;
        
        public TCP(int window_size, double timeout) {
            this.window_size = window_size ;
            this.timeout = timeout;
            this.isFullWindowSent = false;
       
            packet_window_start=0;
            window_size_before_last_reduction = 1;
            beta = 2;
            scaling_constant_c = 1;
            last_window_reduction_time = 0;

            this.src_ack_dups_trace = new HashMap<>();
            this.src_ack_window = new HashMap<>();
            this.rcv_window = new HashMap<>();
        } 

        public void sendPackets(ArrayList<Packet> produced_packet, double timestamp) {
            rcv_window.clear();
            src_ack_dups_trace.clear();
            src_ack_window.clear();

            if(packet_window_start+window_size<=produced_packet.size()) {
                for(int i=packet_window_start;i<packet_window_start+window_size;++i) {
                    rcv_window.put(produced_packet.get(i), false);
                    src_ack_window.put(produced_packet.get(i),false);
                    src_ack_dups_trace.put(produced_packet.get(i).packet_id,0); 
                    event_queue.add(new Event(SENT_BY_SOURCE,produced_packet.get(i),timestamp));
                    event_queue.add(new Event(SENT_BY_SOURCE,produced_packet.get(i),timestamp+timeout));
                }
            }
            isFullWindowSent = false;          
        }

        public int lastUnackedPacket() {
            int min=10000000;
            Iterator it = rcv_window.entrySet().iterator();
            while(it.hasNext()) {
                Map.Entry pair = (Map.Entry)it.next();
                if(!(Boolean)pair.getValue() && ((Packet)pair.getKey()).packet_id<min){
                    min=((Packet)pair.getKey()).packet_id;
                }
            }
            return min;
        }
       
        public boolean rcvWindowAllAcked() {
            Iterator it = src_ack_window.entrySet().iterator();
            while(it.hasNext()) {
                Map.Entry pair = (Map.Entry)it.next();
                if(!(Boolean)pair.getValue()) {
                    return false;
                }
            }
            return true;
        }

        public void updateWindowSize(double globalTime) {
            double k = Math.cbrt(this.window_size_before_last_reduction * this.beta / this.scaling_constant_c);

            this.window_size = this.window_size_before_last_reduction + 
                (int)(this.scaling_constant_c * Math.pow(globalTime - k, 3));
        }
    }
}