import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class circuit_DB {
    int nbr_link;
    link_cost[] linkcost;

    public static circuit_DB toCircuit_DB(byte[] ByteArr) {
        ByteBuffer buffer = ByteBuffer.wrap(ByteArr);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        int linkNbr = buffer.getInt();
        circuit_DB db = new circuit_DB();
        db.nbr_link = linkNbr;
        db.linkcost = new link_cost[linkNbr];
        for(int i = 0; i < linkNbr; ++i){
            db.linkcost[i] = new link_cost(buffer.getInt(), buffer.getInt());
        }
        return db;
    }
}
