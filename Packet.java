package CyberScope;

/**
 * Class which represents a packet
 *
 * @author Charlie Jones - 100234961
 */
public class Packet {

    private int orderNo = -1;

    private final String timestamp;
    private final String sourceMAC;
    private final String destinationMAC;
    private final String etherType;
    private final String hex;

    private final int totalLen;
    private boolean flagged = false;
    private String notes = "";


    public Packet(String timestamp, String sourceMAC, String destinationMAC, int byteLen, String etherType, String hex){

        this.timestamp = timestamp;
        this.sourceMAC = sourceMAC;
        this.destinationMAC = destinationMAC;
        this.totalLen = byteLen;
        this.etherType = etherType;
        this.hex = hex;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public String getSourceMAC() {
        return sourceMAC;
    }

    public String getDestinationMAC() {
        return destinationMAC;
    }

    public int getTotalLen() { return totalLen; }

    public String getEtherType() {
        return etherType;
    }

    public boolean isFlagged() { return flagged; }

    public int getOrderNo() { return orderNo; }

    public String getNotes() { return notes; }

    public String getHex() { return hex; }


    public void setFlagged(boolean flag){
        flagged = flag;
    }

    public void setOrderNo(int no) { orderNo = no; }

    public void setNotes(String n) { notes = n; }

    @Override
    public String toString(){
        return timestamp + "\t" + totalLen + "---" + sourceMAC + " ---> " + destinationMAC + "\t" + etherType;
    }

}
