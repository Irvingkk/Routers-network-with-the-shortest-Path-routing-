import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class pkt_INIT {
    int router_id;

    public pkt_INIT(int router_id) {
        this.router_id = router_id;
    }

    public static byte[] toByteArr(pkt_INIT pkt_init) {
        ByteBuffer buffer = ByteBuffer.allocate(4);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        buffer.putInt(pkt_init.router_id);
        return buffer.array();
    }

}
