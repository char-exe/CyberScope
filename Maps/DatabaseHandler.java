package CyberScope.Maps;

import CyberScope.NetworkPacket.TrafficDirection;

import java.sql.*;
import java.util.HashMap;

/**
 * This class is the database handler for the geo-ip database, which resolves geographical information about a given
 * IP address
 *
 * @author Charlie Jones - 100234961
 */
public class DatabaseHandler {

    private Connection conn = null;

    // Cache used to stored looked up addresses with their result to save time when a duplicate address has been
    //  searched
    private static final HashMap<String, ResultSet> addressCache = new HashMap<>();

    public DatabaseHandler(){
        try {
            Connection conn = DriverManager.getConnection("jdbc:sqlite:geo_ip.db");
            if (conn != null) {
                this.conn = conn;
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * This method looks up geographical information about an IP address and returns a Polyline object storing the
     * result set
     * @param ip the address being looked up
     * @param direction the direction of the packet
     * @return Polyline object containing the geographical information about the IP address
     */
    public Polyline createPolylineCandidate(String ip, TrafficDirection direction){

        // If address has been looked up before
        if(addressCache.containsKey(ip)){

            // Get result set from cache
            ResultSet rs = addressCache.get(ip);

            try {
                return new Polyline(ip, direction, rs.getString("country"), rs.getString("city"),
                        rs.getFloat("latitude"), rs.getFloat("longitude"));
            } catch (Exception e) {
                e.printStackTrace();
            }

        }

        // Else, IP address has not been looked up before, so the database must be searched

        // Convert IP address to IP number
        long ipNum = ipAddrToNumberConverter(ip);

        // Select the row from the table where the IP number is between the lower bound and upper bound
        String query = "SELECT * FROM geo_ip_table WHERE lower_bound <= "+ ipNum +
                " AND upper_bound >= "+ ipNum;

        try {
            Statement stmt  = conn.createStatement();
            ResultSet rs = stmt.executeQuery(query);

            // If result is found
            if(rs.next()){

                // If latitude and longitude are valid values
                if(rs.getFloat("latitude") != 0.0f && rs.getFloat("longitude") != 0.0f) {

                    // Add result to cache
                    addressCache.put(ip, rs);

                    return new Polyline(ip, direction, rs.getString("country"), rs.getString("city"),
                            rs.getFloat("latitude"), rs.getFloat("longitude"));

                }

            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    private static long ipAddrToNumberConverter(String ipString){
        String[] splitIP = ipString.split("\\.");
        long result = 0L;
        for (int i = 0; i < splitIP.length; i++) {
            int power = 3 - i;
            int ip = Integer.parseInt(splitIP[i]);
            result += ip * Math.pow(256, power);
        }
        return result;
    }

}
