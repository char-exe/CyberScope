package CyberScope.Filter;

import CyberScope.Main;
import CyberScope.NetworkPacket;
import CyberScope.Packet;

import java.util.ArrayList;

/**
 * This class is used for applying a port filter on network traffic data.
 *
 * @author Charlie Jones - 100234961
 */
public class PortFilter extends RangeFilter {

    private boolean isSourcePort;

    public PortFilter(int lowerBound, int upperBound, boolean isSourcePort) {
        super(lowerBound, upperBound);
        this.isSourcePort = isSourcePort;

        filterPackets();
    }

    @Override
    public void filterPackets() {
        ArrayList<Packet> result = new ArrayList<>();

        // If the filter is for the source port
        if(isSourcePort) {

            // For every packet in database
            for(Packet packet : Main.getDb().getPackets()) {

                // If packet's source port is within filter boundaries
                if(packet instanceof NetworkPacket &&
                        ((NetworkPacket) packet).getSourcePort() >= this.getLowerBound() &&
                        ((NetworkPacket) packet).getSourcePort() <= this.getUpperBound()
                ) {

                    result.add(packet);

                }

            }

        }
        // Else, filter is for the destination port
        else {

            // For every packet in database
            for(Packet packet : Main.getDb().getPackets()) {

                // If packet's destination port is within filter boundaries
                if(packet instanceof NetworkPacket &&
                        ((NetworkPacket) packet).getDestinationPort() >= this.getLowerBound() &&
                        ((NetworkPacket) packet).getDestinationPort() <= this.getUpperBound()
                ) {

                    result.add(packet);

                }

            }

        }

        addPackets(result);

    }

    public void setIsSourcePort(boolean isSourcePort){
        this.isSourcePort = isSourcePort;
    }

    public boolean isSourcePort(){
        return isSourcePort;
    }

}
