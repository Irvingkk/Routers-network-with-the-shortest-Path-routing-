import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class pkt_LSPDU{
    int sender;
    int router_id;
    int link_id;
    int cost;
    int via;

    public pkt_LSPDU(int sender, int via) {
        this.sender = sender;
        this.via = via;
    }

    public pkt_LSPDU(int sender, int router_id, int link_id, int cost, int via) {
        this.sender = sender;
        this.router_id = router_id;
        this.link_id = link_id;
        this.cost = cost;
        this.via = via;
    }

    public static byte[] toByteArr(pkt_LSPDU lspdu) {
        ByteBuffer buffer = ByteBuffer.allocate(20);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        buffer.putInt(lspdu.sender);
        buffer.putInt(lspdu.router_id);
        buffer.putInt(lspdu.link_id);
        buffer.putInt(lspdu.cost);
        buffer.putInt(lspdu.via);
        return buffer.array();
    }

    public static pkt_LSPDU toLSPDU(byte[] ByteArr) {
        ByteBuffer buffer = ByteBuffer.wrap(ByteArr);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        pkt_LSPDU lspdu = new pkt_LSPDU(buffer.getInt(), buffer.getInt(),
                buffer.getInt(), buffer.getInt(), buffer.getInt());
        return lspdu;
    }
}
