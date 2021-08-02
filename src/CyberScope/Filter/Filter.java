package CyberScope.Filter;

import CyberScope.Packet;

import java.util.ArrayList;

/**
 * Filter interface used for applying filters to network traffic data
 *
 * @author Charlie Jones - 100234961
 */
public interface Filter {

    enum FilterType{
        MAC,IP,Protocol,Port,Size,Flagged
    }

    void filterPackets();

    ArrayList<Packet> getFilteredPackets();

}
