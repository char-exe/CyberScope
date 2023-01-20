package CyberScope.Filter;

import CyberScope.Packet;

import java.util.ArrayList;

/**
 * This class is used for applying specific filters (protocol and flagged) to network traffic data
 *
 * @author Charlie Jones - 100234961
 */
public abstract class SpecifiedFilter implements Filter {

    private boolean isNegated;

    private final ArrayList<Packet> filteredPackets = new ArrayList<>();

    public SpecifiedFilter(boolean isNegated) {
        this.isNegated = isNegated;
    }

    @Override
    public abstract void filterPackets();

    public ArrayList<Packet> getFilteredPackets(){
        return filteredPackets;
    }

    public void addPackets(ArrayList<Packet> packets){
        filteredPackets.addAll(packets);
    }

    public void setIsNegated(boolean isNegated){
        this.isNegated = isNegated;
    }

    public boolean isNegated() {
        return isNegated;
    }
}
