import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class pkt_HELLO implements Serializable{
    int router_id;
    int link_id;

    public pkt_HELLO(int router_id, int link_id) {
        this.router_id = router_id;
        this.link_id = link_id;
    }

    public static byte[] toByteArr(pkt_HELLO hello) {
        ByteBuffer buffer = ByteBuffer.allocate(8);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        buffer.putInt(hello.router_id);
        buffer.putInt(hello.link_id);
        return buffer.array();
    }

    public static pkt_HELLO toHello(byte[] ByteArr) {
        ByteBuffer buffer = ByteBuffer.wrap(ByteArr);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        pkt_HELLO hello = new pkt_HELLO(buffer.getInt(), buffer.getInt());
        return hello;
    }
}
