package CyberScope.Maps;

import CyberScope.NetworkPacket.TrafficDirection;

/**
 * This class is used to represent an IP address and it's geographic data. It is called polyline because that is what
 * will be drawn on the GoogleMaps API. It will be passed in a JSON object to construct a JS version of the object in
 * the webview engine.
 *
 * @author Charlie Jones - 100234961
 */
public class Polyline {

    private final String ip;
    private TrafficDirection direction;
    private final String country;
    private final String city;
    private final float latitude;
    private final float longitude;

    public Polyline(String ip, TrafficDirection direction, String country, String city, float latitude,
                    float longitude){
        this.ip = ip;
        this.direction = direction;
        this.country = country;
        this.city = city;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public String getIp(){
        return ip;
    }

    public TrafficDirection getDirection() {
        return direction;
    }

    public String getCountry() {
        return country;
    }

    public String getCity() {
        return city;
    }

    public float getLatitude() {
        return latitude;
    }

    public float getLongitude() {
        return longitude;
    }

    public void setDirection(TrafficDirection bidirectional) {
        direction = bidirectional;
    }

    @Override
    public String toString() {
        return "Polyline{" +
                "ip='" + ip + '\'' +
                ", direction=" + direction +
                ", country='" + country + '\'' +
                ", city='" + city + '\'' +
                ", latitude=" + latitude +
                ", longitude=" + longitude +
                '}';
    }
}
