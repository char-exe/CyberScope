package CyberScope;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

/**
 * This class is used in TCPPacket and UDPPacket to create a map of application layer protocols so that a packet's used
 * ports can be mapped to a protocol. It reads csv files from the PortMappingFiles package and creates a port number as
 * the key and the protocol as the value. Then when a TCP or UDP packet object is created, it's protocol can be worked
 * out from the ports that it uses.
 *
 * @author Charlie Jones - 100234961
 */
public class PortMapping implements Serializable {

    private static final String path = "src/CyberScope/PortMappingFiles";

    private final Map<Integer, String> map = new HashMap<>();

    public PortMapping(String fileName, FileReader reader){

        String line;
        try{

            BufferedReader r = new BufferedReader(reader);

            // For each port range in file
            while((line = r.readLine()) != null){

                String[] portInfo = line.split(",");

                if(portInfo.length > 1) {

                    // If, row consists of just one port
                    if (!portInfo[1].contains("-")) {

                        map.put(Integer.parseInt(portInfo[1]), portInfo[0]);

                    }
                    // Else, Port is a range e.g. "6000-6050" so a key for each number in range must be created
                    else {

                        String[] temp = portInfo[1].split("-");

                        int lowerBound = Integer.parseInt(temp[0]);
                        int upperBound = Integer.parseInt(temp[1]);

                        for (int i = lowerBound; i <= upperBound; i++)
                            map.put(i, portInfo[0]);

                    }

                }

            }

            this.writeObject(fileName);

        }

        catch(IOException e){

            e.printStackTrace();

        }

    }

    public Map<Integer, String> getMap(){
        return map;
    }

    public void writeObject(String fileName){
        try{
            ObjectOutputStream os = new ObjectOutputStream(new FileOutputStream(path+'/'+fileName));
            os.writeObject(this);
        }catch(IOException ignored){
        }
    }

    public static PortMapping readObject(String fileName) throws IOException, ClassNotFoundException {
        ObjectInputStream is = new ObjectInputStream(new FileInputStream(path+'/'+fileName));
        return (PortMapping) is.readObject();
    }

    public static void main(String[] args) throws IOException {
        new PortMapping("tcp.txt", new FileReader("src/CyberScope/PortMappingFiles/tcpPorts.csv"));
        new PortMapping("udp.txt", new FileReader("src/CyberScope/PortMappingFiles/udpPorts.csv"));
    }
}