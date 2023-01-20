package CyberScope.Filter;

import CyberScope.Main;
import CyberScope.NetworkPacket;
import CyberScope.Packet;

import java.util.ArrayList;

/**
 * This class is used for applying a protocol filter on network traffic data.
 *
 * @author Charlie Jones - 100234961
 */
public class ProtocolFilter extends SpecifiedFilter {

    private final String protocol;

    public ProtocolFilter(boolean isNegated, String protocol) {
        super(isNegated);
        this.protocol = protocol;

        filterPackets();
    }

    @Override
    public void filterPackets() {
        ArrayList<Packet> result = new ArrayList<>();

        // If filter is negated
        if(this.isNegated()){

            // For every packet in database
            for(Packet packet : Main.getDb().getPackets()){

                // If packet is a network layer packet
                if(packet instanceof NetworkPacket) {

                    // If packet's protocol is not the same as the target protocol
                    if (!((NetworkPacket) packet).getProtocol().equals(protocol)) {

                        result.add(packet);

                    }

                }
                // Else packet is an ethernet layer packet
                else {

                    // If packet's ether type is not the same as the target ether type
                    if(!packet.getEtherType().equals(protocol)){

                        result.add(packet);

                    }

                }

            }

        }
        // Else, filter is not negated
        else {

            // For every packet in database
            for(Packet packet : Main.getDb().getPackets()){

                // If packet is a network layer packet
                if(packet instanceof NetworkPacket) {

                    // If packet's protocol is the same as the target protocol
                    if (((NetworkPacket) packet).getProtocol().equals(protocol)) {

                        result.add(packet);

                    }

                }
                // Else, packet is a link layer packet
                else {

                    // If packets ether type is the same as the target ether type
                    if(packet.getEtherType().equals(protocol)){

                        result.add(packet);

                    }

                }

            }

        }

        addPackets(result);

    }

    public String getProtocol(){
        return protocol;
    }

}
