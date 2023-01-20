/**
 * This file is responsible for initialising GoogleMaps API, and drawing polyline objects on it.
 */

// Home icon used for user's current public IP location
const homeIcon = 'homeIcon.png'

/**
 * This class is the JS version of the Polyline.java class. This class also draws the polyline on the map once it has
 * finished being constructed.
 */
class Poly {

    constructor(ip, country, city, lat, lng, direction) {

        // Creates Google maps polyline and sets some options
        const line = new google.maps.Polyline({
            geodesic: true,
            strokeOpacity: 1.0,
            strokeWeight: 4
        });

        // If polyline should be an outgoing stream
        if(direction === 'OUTGOING') {

            this.baseIP = document.hostIP;
            this.baseLatLng = document.hostLatLng;

            this.baseCity = document.hostCity;
            this.baseCountry = document.hostCountry;

            this.destIP = ip;

            this.destLatLng = new google.maps.LatLng(lat, lng);
            this.destCountry = country;
            this.destCity = city;

            this.direction = direction;

            // Set line colour to red
            line.setOptions({
                strokeColor: "#c43131"
            });

        }
        // Else if polyline should be an incoming stream
        else if (direction === 'INCOMING'){

            this.destIP = document.hostIP;
            this.destLatLng = document.hostLatLng;

            this.destCity = document.hostCity;
            this.destCountry = document.hostCountry;

            this.baseIP = ip;

            this.baseLatLng = new google.maps.LatLng(lat, lng);
            this.baseCountry = country;
            this.baseCity = city;

            this.direction = direction;

            // Set line colour to blue
            line.setOptions({
                strokeColor: "#329ccf"
            });

        }
        // Else, polyline should be a bi-directional stream
        else {

            this.baseIP = document.hostIP;
            this.baseLatLng = document.hostLatLng;

            this.baseCity = document.hostCity;
            this.baseCountry = document.hostCountry;

            this.destIP = ip;

            this.destLatLng = new google.maps.LatLng(lat, lng);
            this.destCountry = country;
            this.destCity = city;

            this.direction = direction;

            // Set line colour to purple
            line.setOptions({
                strokeColor: "#9d316e"
            });

        }

        // Set the start point and end point of the polyline
        line.setOptions({
            path: [this.baseLatLng, this.destLatLng]
        });

        // Increase width of line and show detailed view of stream if hovered over
        line.addListener('mouseover', () => {
            line.setOptions({
                strokeWeight: 10
            });
            setFocusedLine(this);
        });

        // Restore original width of line if mouse if no longer hovered over
        line.addListener('mouseout', () => {
            line.setOptions({
                strokeWeight: 4
            });
        });

        // Add polyline to the map
        line.setMap(map);

    }

}

/**
 * This method is called when the user hovers their mouse over a polyline on the map. It displays more detailed
 * information about the packet stream in a box below the map.
 * @param line: the polyline that is being hovered over by the user's mouse
 */
function setFocusedLine(line){

    // Get HTML element which shows the information and clear what is currently inside it
    let content = $('#content');
    content.empty();

    // If line is a bi-directional packet stream
    if(line.direction === 'BIDIRECTIONAL'){

        // Set title
        document.getElementById("detailedTitle").innerText = 'Bi-directional packet stream';

        // Add detailed information as HTML
        content.append("<p>Host (you): <b>"+line.baseIP+"</b> --- "+line.baseCity+", "+line.baseCountry+".</p>" +
            "<p>Destination address(es): <b>"+line.destIP+"</b> --- "+line.destCity+", "+line.destCountry+".</p>" +
            "<p>Coordinates: "+line.baseLatLng+" --> "+line.destLatLng+"</p>" +
            "<p>A two-way connection between host and these public addresses have been found in the same location" +
            "</p>"
        );

    }
    // Else, line is a uni-directional packet stream
    else {

        // If line is an outgoing packet stream
        if(line.direction === 'OUTGOING') {
            document.getElementById("detailedTitle").innerText = 'Outgoing packet stream';
        }
        // Else if line is an incoming packet stream
        else if(line.direction === 'INCOMING') {
            document.getElementById("detailedTitle").innerText = 'Incoming packet stream';
        }

        // Add detailed information as HTML
        content.append("<p>Source: <b>"+line.baseIP+"</b> --- "+line.baseCity+", "+line.baseCountry+".</p>" +
            "<p>Destination(s): <b>"+line.destIP+"</b> --- "+line.destCity+", "+line.destCountry+".</p>" +
            "<p>Coordinates: "+line.baseLatLng+" --> "+line.destLatLng+"</p>"
        );

    }

}

/**
 * This function is used to bridge the gap between Java and JS. It can be run by Java, which adds all of the polylines
 * as a JSON string into the function's arguments. The function then unpacks the JSON data and uses it to create Poly
 * objects out of it
 */
document.createPolys = function(javaJson){

    let json = JSON.parse(javaJson);
    let addrList = json.ipAddresses;

    // If there is more than one IP address for the packet stream
    if(addrList.length > 1){

        // Removes any possible duplicates when merging bi-directional streams of packets
        addrList = [...new Set(addrList)];

        // Create incoming Poly with a list of IP addresses if direction is incoming
        if(json.direction === 'INCOMING')
            new Poly(addrList.toString().replace(/,/g, ', '),
                json.country, json.city, json.latitude, json.longitude, 'INCOMING');

        // Else, create outgoing Poly with a list of IP addresses if direction is outgoing
        else if (json.direction === 'OUTGOING')
            new Poly(addrList.toString().replace(/,/g, ', '),
                json.country, json.city, json.latitude, json.longitude, 'OUTGOING');

        // Else, create bi-directional Poly with a list of IP addresses
        else
            new Poly(addrList.toString().replace(/,/g, ', '),
                json.country, json.city, json.latitude, json.longitude, 'BIDIRECTIONAL');

    }
    // Else, there is only one IP address associated with the packet stream
    else {

        // Create incoming Poly if direction is incoming
        if(json.direction === 'INCOMING')
            new Poly(addrList[0], json.country, json.city, json.latitude, json.longitude, 'INCOMING');

        // Else, create outgoing Poly if direction is outgoing
        else if (json.direction === 'OUTGOING')
            new Poly(addrList[0], json.country, json.city, json.latitude, json.longitude, 'OUTGOING');

        // Else, create a bi-directional Poly
        else
            new Poly(addrList[0], json.country, json.city, json.latitude, json.longitude, 'BIDIRECTIONAL');

    }

}

/**
 * This method gets the location of the user's public IP address and converts it into latitude and longitude data
 * @param addr
 * @returns {(number|*|string)[]}
 */
function queryGeoLocator(addr) {

    let json;

    // Request external API
    $.ajax({
        async: false,
        dataType: 'json',
        url: 'https://freegeoip.app/json/'+addr,
        success: function (data){
            json = data;
        }
    });

    return [parseFloat(json.latitude), parseFloat(json.longitude), json.country_name, json.city];
}

/**
 * Sets the map focus and zoom onto the location of where the user's public IP address is located
 */
function setMapFocus(){
    map.setCenter(document.hostLatLng);
    map.setZoom(7);
}

/**
 * Initialiser method for GoogleMapsAPI, configuring options and adding 'home' marker to the map
 */
let map;
function initGoogleMaps() {
    map = new google.maps.Map(document.getElementById("map"), {
        zoomControl: true,
        mapTypeControl: false,
        scaleControl: false,
        streetViewControl: false,
        rotateControl: false,
        fullscreenControl: false
    });

    setMapFocus();

    new google.maps.Marker({
        position: document.hostLatLng,
        map: map,
        icon: homeIcon,
    });
}

/**
 * Main controller function, called when webview is created
 */
function main(){

    let geoInfo = queryGeoLocator(document.hostIP);
    document.hostLatLng = new google.maps.LatLng(geoInfo[0], geoInfo[1]);

    document.hostCountry = geoInfo[2];

    if(geoInfo[3] !== ""){
        document.hostCity = geoInfo[3];
    } else{
        document.hostCity = 'Unknown city';
    }

    initGoogleMaps();

}
