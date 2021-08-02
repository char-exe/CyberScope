package CyberScope.Filter;

import CyberScope.Main;
import CyberScope.Packet;

import java.util.ArrayList;

/**
 * This class is used for applying a flagged filter on network traffic data.
 *
 * @author Charlie Jones - 100234961
 */
public class FlaggedFilter extends SpecifiedFilter {

    public FlaggedFilter(boolean isNegated) {
        super(isNegated);

        filterPackets();

    }

    @Override
    public void filterPackets() {
        ArrayList<Packet> result = new ArrayList<>();

        // If filter is not negated, get any packet that is flagged
        if(!this.isNegated()){

            for(Packet packet : Main.getDb().getPackets()){

                if(packet.isFlagged()){

                    result.add(packet);

                }

            }

        }
        // Else, filter is negated, get any packet that is NOT flagged
        else {

            for(Packet packet : Main.getDb().getPackets()){

                if(!packet.isFlagged()){

                    result.add(packet);

                }

            }

        }

        addPackets(result);

    }

}
