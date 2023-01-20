package CyberScope.Filter;

import CyberScope.Main;
import CyberScope.Packet;

import java.util.ArrayList;

/**
 * This class is used for applying a MAC address filter on network traffic data.
 *
 * @author Charlie Jones - 100234961
 */
public class MACFilter extends AddressFilter {

    public MACFilter(String addr, boolean isSourceAddress, boolean isNegated) {
        super(addr, isSourceAddress, isNegated);

        filterPackets();
    }

    @Override
    public void filterPackets() {
        ArrayList<Packet> result = new ArrayList<>();

        // If filter is negated
        if(this.getIsNegated()){

            // For every packet in database
            for(Packet packet : Main.getDb().getPackets()){

                // If filter is for source address and the packet's source address does not equal the target addr
                if(this.getIsSourceAddress() && !packet.getSourceMAC().equals(this.getAddr())){

                    result.add(packet);

                }
                // Else if filter is for destination address and the packet's destination address does not equal the
                //  target address
                else if(!this.getIsSourceAddress() && !packet.getDestinationMAC().equals(this.getAddr())) {

                    result.add(packet);

                }

            }

        }
        // Else, filter is not negated
        else {

            // For every packet in database
            for(Packet packet : Main.getDb().getPackets()){

                // If filter is for source address and packet's source address equals the target address
                if(this.getIsSourceAddress() && packet.getSourceMAC().equals(this.getAddr())){

                    result.add(packet);

                }
                // Else if, filter is destination address and packet's destination address equals the target addr
                else if(!this.getIsSourceAddress() && packet.getDestinationMAC().equals(this.getAddr())) {

                    result.add(packet);

                }

            }

        }

        addPackets(result);

    }
}
