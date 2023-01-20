package CyberScope.Filter;

import CyberScope.Main;
import CyberScope.Packet;

import java.util.ArrayList;

/**
 * This class is used for applying a packet size filter on network traffic data.
 *
 * @author Charlie Jones - 100234961
 */
public class SizeFilter extends RangeFilter{

    public SizeFilter(int lowerBound, int upperBound) {
        super(lowerBound, upperBound);

        filterPackets();
    }

    @Override
    public void filterPackets() {
        ArrayList<Packet> result = new ArrayList<>();

        // For every packet in database
        for(Packet packet : Main.getDb().getPackets()){

            // If packet's size is within the filter's boundaries
            if(packet.getTotalLen() >= this.getLowerBound() && packet.getTotalLen() <= this.getUpperBound()){

                result.add(packet);

            }

        }

        addPackets(result);

    }

}
