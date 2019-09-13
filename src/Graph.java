import java.io.PrintWriter;
import java.util.*;

public class Graph {
    Map<Integer, Node> vertices;

    public class Node {
        int id;
        int dist;
        Map<Integer, Integer> linkcosts; // <linkid, linkcost>
        Map<Integer, Integer> neighbours; // <routerid, linkid>
        Node(int id){
            this.id = id;
            dist = 0;
            linkcosts = new HashMap<>(0);
            neighbours = new HashMap<>(0);
        }
    }


    Graph(){
        vertices = new HashMap<>(0);
    }

    int addlink(int router, int link, int cost){
        // add a new node to graph, if this node(router) doesn't exist;
        if(!vertices.containsKey(router)){
           vertices.put(router, new Node(router));
        }

        // check if this linkcost pair has already exist.
        Node node = vertices.get(router);
        if(node.linkcosts.containsKey(link)){
            return 1;
        }

        /* search through the linkcosts hashmap of all Nodes in this graph.
            If it finds a link with the same id, then add neighbours of each
            other between that node and my node.
         */
        Node mynode = vertices.get(router);
        Set set = vertices.entrySet();
        Iterator i = set.iterator();
        while(i.hasNext()) {
            Map.Entry mentry = (Map.Entry) i.next();
            if(mentry.getKey().equals(router)) continue; // it can't be a neighbour since it's the same router id
            Node node1 = (Node)mentry.getValue();
            if(node1.linkcosts.containsKey(link)){
                node1.neighbours.put(router, link);
                mynode.neighbours.put(node1.id, link);
            }
        }

        // add link to mynode.
        mynode.linkcosts.put(link, cost);
        return 0;
    }

    public class NodeDistComparator implements Comparator<Node>{

        @Override
        public int compare(Node x, Node y) {
            if(x.dist < y.dist){
                return -1;
            } else if(x.dist > y.dist){
                return 1;
            }
            return 0;
        }
    }

    public class Predecessors{
        int source;
        Map<Integer, Integer> prede;
        Predecessors(int source){
            this.source = source;
            prede = new HashMap<>(0);
        }
        void addpair(int router, int predecessor){
            this.prede.put(router, predecessor);
        }
    }
    public Predecessors DijstraAlgo(int source){
        Comparator<Node> comparator = new NodeDistComparator();
        PriorityQueue<Node> pqueue = new PriorityQueue<>(comparator);
        Predecessors predecessor = new Predecessors(source);

        // initial all the node.dis in vertices.
        Set set = vertices.entrySet();
        Iterator i = set.iterator();
        while(i.hasNext()){
            Map.Entry mentry = (Map.Entry) i.next();
            Node node = (Node)mentry.getValue();
            if(mentry.getKey().equals(source)){
                node.dist = 0;
            } else {
                node.dist = 80000;
            }
        }

        // add all notes in vertices to priority queue
        Set set2 = vertices.entrySet();
        Iterator i2 = set2.iterator();
        while(i2.hasNext()){
            Map.Entry mentry = (Map.Entry) i2.next();
            Node node = (Node)mentry.getValue();
            pqueue.add(node);
        }


        while((!pqueue.isEmpty()) && ((pqueue.peek()).dist != 80000)){
            Node node = (Node)pqueue.poll(); // deleteMinKey from pqueue

            // traverse all the neighbours of node
            Set set1 = node.neighbours.entrySet();
            Iterator i1 = set1.iterator();
            while(i1.hasNext()){
                Map.Entry mentry = (Map.Entry)i1.next();
                int neigh = (int)mentry.getKey();

                // check if neighbour router belong to pqueue
                Iterator i3 = pqueue.iterator();
                boolean IsInPqueue = false;
                Node u = null;
                while(i3.hasNext()){
                    u = (Node) i3.next();
                    if(u.id == neigh){
                        IsInPqueue = true;
                        break;
                    }
                }
                if(!IsInPqueue) {
                    continue; //router doesn't belong to pqueue
                }

                // compare distance;
                int linkid = node.neighbours.get(neigh); // id of the link between(node and u)
                if(vertices.get(neigh).dist > node.dist + node.linkcosts.get(linkid)){
                    vertices.get(neigh).dist = node.dist + node.linkcosts.get(linkid);
                    predecessor.addpair(vertices.get(neigh).id, node.id);
                }
            }
        }
        return predecessor;
    }

    void logRIB(PrintWriter writer, Predecessors predecessor, int print){
        String[] logfile = new String[5];
        int source = predecessor.source;
        Map<Integer, Integer> PredMap= predecessor.prede;

        writer.println("# RIB " + print);

        for(int i = 1; i <= 5; i++){
            if(!vertices.containsKey(i)){
                logfile[i -1] = String.format("R%d -> R%d -> INF, INF", source, i);
            }
        }

        // iterate through each node in vertices. Check their distance, if it's INF(80000), put log message
        //  INF, INF to logfile; otherwise, traverse the predecessor map to find the next link path for source
        //  node in the shortest path, and put this info into logfile as well.
        Set set = vertices.entrySet();
        Iterator i = set.iterator();
        while(i.hasNext()){
            Map.Entry mentry = (Map.Entry)i.next();
            int routerid = (int)mentry.getKey();
            Node router = (Node)mentry.getValue();
            if(router.dist == 80000){
                logfile[routerid - 1] = String.format("R%d -> R%d -> INF, INF", source, routerid);
                continue;
            }
            if(routerid == source){
                logfile[routerid - 1] = String.format("R%d -> R%d -> Local, 0", source, routerid);
                continue;
            }
            // traverse the predecessor to find the shortest path.
            int prede = routerid;
            while(PredMap.get(prede) != source){
                prede = PredMap.get(prede);
            }

            Node srcnode = vertices.get(source);
            int linkid = srcnode.neighbours.get(prede);
            int cost = srcnode.linkcosts.get(linkid);
            logfile[routerid - 1] = String.format("R%d -> R%d -> %d, %d", source, routerid, linkid, cost);
        }

        // log all the lines in logfile.
        for(int p = 0; p< 5; ++p){
            writer.println(logfile[p]);
        }
    }

    void logTopoDB(PrintWriter writer, int source,int print){
        writer.println("# Topology database " + print);

        for(int index = 1; index <=5; ++index){
            boolean RouterIndexExist = false;
            Set set = vertices.entrySet();
            Iterator i = set.iterator();
            while(i.hasNext()){
                Map.Entry mentry = (Map.Entry)i.next();
                int routerid = (int)mentry.getKey();
                Node node = (Node)mentry.getValue();
                if(routerid != index) continue;

                // print node with id number: index
                RouterIndexExist = true;
                writer.format("R%d -> R%d nbr link %d\n", source, index, node.linkcosts.size());

                // traverse the node.linkcosts to print all pairs of link &cost of that node
                Set set1 = node.linkcosts.entrySet();
                Iterator i1 = set1.iterator();
                while(i1.hasNext()){
                    Map.Entry mentry1 = (Map.Entry)i1.next();
                    int key = (int)mentry1.getKey();
                    int value = (int)mentry1.getValue();
                    writer.format("R%d -> R%d link %d cost %d\n", source, index, key, value);
                }
            }
            if(!RouterIndexExist){ // Node with router(id = index) doesn't exist in vertices.
                writer.format("R%d -> R%d nbr link %d\n", source, index, 0);
            }
        }

    }




}
