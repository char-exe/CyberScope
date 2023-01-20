package CyberScope.Filter;

import CyberScope.Packet;

import java.util.ArrayList;

/**
 * This class is used for applying MAC/IP filters to network traffic data
 *
 * @author Charlie Jones - 100234961
 */
public abstract class AddressFilter implements Filter {

    private final String addr;
    private boolean isNegated;
    private boolean isSourceAddress;

    private final ArrayList<Packet> filteredPackets = new ArrayList<>();

    public AddressFilter(String addr, boolean isSourceAddress, boolean isNegated) {
        this.addr = addr;
        this.isSourceAddress = isSourceAddress;
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

    public void setIsSourceAddress(boolean isSourceAddress){
        this.isSourceAddress = isSourceAddress;
    }

    public boolean getIsNegated(){
        return isNegated;
    }

    public boolean getIsSourceAddress(){
        return isSourceAddress;
    }

    public String getAddr(){
        return addr;
    }

}
