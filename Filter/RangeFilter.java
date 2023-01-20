package CyberScope.Filter;

import CyberScope.Packet;

import java.util.ArrayList;

/**
 * This class is used for applying range filters (packet size and ports) to network traffic data
 *
 * @author Charlie Jones - 100234961
 */
public abstract class RangeFilter implements Filter {

    private int lowerBound;
    private int upperBound;

    private final ArrayList<Packet> filteredPackets = new ArrayList<>();

    public RangeFilter(int lowerBound, int upperBound) {
        this.lowerBound = lowerBound;
        this.upperBound = upperBound;
    }

    public void setLowerBound(int lowerBound){
        this.lowerBound = lowerBound;
    }

    public void setUpperBound(int upperBound){
        this.upperBound = upperBound;
    }

    @Override
    public abstract void filterPackets();

    public ArrayList<Packet> getFilteredPackets(){
        return filteredPackets;
    }

    public void addPackets(ArrayList<Packet> packets){
        filteredPackets.addAll(packets);
    }

    public int getLowerBound(){
        return lowerBound;
    }

    public int getUpperBound(){
        return upperBound;
    }


}
