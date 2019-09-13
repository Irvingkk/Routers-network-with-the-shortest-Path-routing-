import java.io.*;
import java.net.*;
import java.util.*;

public class router {

    private static DatagramSocket socket;
    private static PrintWriter writer;
    private static int nse_port;
    private static String nse_host;
    private static int router_id;
    private static int router_port;
    private static int flag;

    static void sendpkt(byte[] bytearr) throws Exception{
        InetAddress IPAddr = InetAddress.getByName(nse_host); // create nse IP address.
        socket.send(new DatagramPacket(bytearr, bytearr.length, IPAddr, nse_port)); // send initial struct bytearr.
    }

    static byte[] revpkt() throws Exception{
        byte[] buffer= new byte[512];
        DatagramPacket pct= new DatagramPacket(buffer, buffer.length);
        socket.receive(pct);
        if(pct.getLength() == 8){
            flag = 0;
        } else if(pct.getLength() == 20){
            flag = 1;
        } else{
            System.out.println("receive packet with wrong size");
            System.exit(1);
        }
        return pct.getData();
    }

    public static void main(String[] args) throws Exception {

        // initial parameters
        router_id = Integer.parseInt(args[0]);
        nse_host = args[1];
        nse_port = Integer.parseInt(args[2]);
        router_port = Integer.parseInt(args[3]);

        // Inital UDP socket and writer
        socket = new DatagramSocket(router_port);
        writer = new PrintWriter("router" + router_id + ".log", "UTF-8");

        // send initial packet to nse
        pkt_INIT pkt_init = new pkt_INIT(router_id);
        byte[] bytearr = pkt_INIT.toByteArr(pkt_init);
        InetAddress IPAddr = InetAddress.getByName(nse_host); // create nse IP address.
        socket.send(new DatagramPacket(bytearr, bytearr.length, IPAddr, nse_port)); // send initial struct bytearr.
        writer.format("R%d sends an INIT: router_id %d\n", router_id, router_id);

        // receive circuit_DB structure
        byte[] buffer = new byte[1024];
        DatagramPacket pct = new DatagramPacket(buffer, buffer.length);
        socket.receive(pct);
        circuit_DB circuit_db = circuit_DB.toCircuit_DB(buffer);
        writer.format("R%d receives a CIRCUIT_DB: nbr_link %d\n", router_id, circuit_db.nbr_link);

        /* store links in circuit DB in topology database. Using vector(array)?
            {format:
                routerID -> routerID nbr link number
                routerID -> routerID linkID cost#
                ...
            }
         */

        Graph graph = new Graph();
        for (int i = 0; i < circuit_db.nbr_link; ++i) {
            link_cost lsobject = circuit_db.linkcost[i];
            graph.addlink(router_id, lsobject.link, lsobject.cost);
        }

        // send Hello pct to the neighbour.(routerID(who send Hello) linkID)
        for(int i = 0; i< circuit_db.nbr_link; ++i){
            link_cost lsobject = circuit_db.linkcost[i];
            pkt_HELLO hello = new pkt_HELLO(router_id, lsobject.link);
            sendpkt(pkt_HELLO.toByteArr(hello));
            writer.format("R%d sends a HELLO: router_id %d link_id %d\n",
                    router_id, router_id, lsobject.link);
        }

        /* use a loop to wait at socket to receive packet. If it's Hello, do above thing;
            otherwise, (check the routerID and link info, if it's a duplicate info in topology database, do nothing!)
            else, add the info in LSPDU to topology database(if it's not duplicate), also add
            1 to link nbr of certain router.
            Then change the sender and linkSendBy of LSPDU and send to other neighbours.

           After that, run Dijkstra algorithm based on topology database to get RIB

           Note: {make LSPDU packets and send to neighbours by UDP socket using their port number.
                sender: routerID
                routerID;
                linkID and cost#;
                linkSendBy;
                }
         */ // set a timer to loop, otherwise, it won't stop.

        int print = 1;
        socket.setSoTimeout(3000);
        List<Integer> Hellolinks = new LinkedList<>();
        try{
            while(true){
                byte[] bytebuffer = revpkt(); // might timeout here
                if(flag == 0){ // it's a Hello packet
                    pkt_HELLO hello = pkt_HELLO.toHello(bytebuffer);
                    Hellolinks.add(hello.link_id);
                    writer.format("R%d receives a HELLO: router_id %d link_id %d\n",
                            router_id, hello.router_id, hello.link_id);

                    /* As long as receive Hello, retrieve routerID and link#. Then, using a loop to
                        send nbr # of LSPDU to him.
                    */
                    for(int p = 0; p < circuit_db.nbr_link; ++p){
                        link_cost lsobject = circuit_db.linkcost[p];
                        pkt_LSPDU lspdu = new pkt_LSPDU(router_id, hello.link_id);
                        lspdu.link_id = lsobject.link;
                        lspdu.cost = lsobject.cost;
                        lspdu.router_id = router_id;
                        sendpkt(pkt_LSPDU.toByteArr(lspdu));
                        writer.format("R%d sends an LS PDU: sender %d, router_id %d, link_id %d, cost %d, via %d\n",
                                router_id, lspdu.sender, lspdu.router_id, lspdu.link_id, lspdu.cost, lspdu.via);
                    }
                    continue;
                }

                // receive a LSPDU packet
                pkt_LSPDU lspdu = pkt_LSPDU.toLSPDU(bytebuffer);
                int router = lspdu.router_id;
                int link = lspdu.link_id;
                int cost = lspdu.cost;
                int via = lspdu.via;

                // log receiving pkt_LSPDU
                writer.format("R%d receives an LS PDU: sender %d, router_id %d, link_id %d, cost %d, via %d\n",
                        router_id, lspdu.sender, router, link, cost, via);
                // add to topology database(graph)
                int flag = graph.addlink(router, link, cost);
                if(flag == 1) continue; // duplicate LSPDU

                lspdu.sender = router_id; // edit lspdu sender field
                for(int i = 0; i< circuit_db.nbr_link; ++i){
                    link_cost lsobject = circuit_db.linkcost[i];
                    lspdu.via = lsobject.link; // edit lspdu via field
                    if(lsobject.link == via) continue;  // don't send lspdu of sender link.
                    if(!Hellolinks.contains(lsobject.link)) continue; // don't send lspdu to the router from
                                                                        //which you don't receive hello

                    // send lspdu
                    sendpkt(pkt_LSPDU.toByteArr(lspdu));
                    writer.format("R%d sends an LS PDU: sender %d, router_id %d, link_id %d, cost %d, via %d\n",
                            router_id, lspdu.sender, lspdu.router_id, lspdu.link_id, lspdu.cost, lspdu.via);
                }
                graph.logTopoDB(writer, router_id, print);
                Graph.Predecessors predes = graph.DijstraAlgo(router_id);
                graph.logRIB(writer, predes, print);
                print++;
            }
        } catch (SocketTimeoutException e){}

        // close the socket and writer.


        writer.close();
    }
}






































