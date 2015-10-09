/* AUTO-GENERATED FILE.  DO NOT MODIFY.
 *
 * This class was automatically generated by the
 * java mavlink generator tool. It should not be modified by hand.
 */

// MESSAGE HIL_GPS PACKING
package com.MAVLinks.common;
import com.MAVLinks.MAVLinkPacket;
import com.MAVLinks.Messages.MAVLinkMessage;
import com.MAVLinks.Messages.MAVLinkPayload;

/**
* The global position, as returned by the Global Positioning System (GPS). This is
                 NOT the global position estimate of the sytem, but rather a RAW sensor value. See message GLOBAL_POSITION for the global position estimate. Coordinate frame is right-handed, Z-axis up (GPS frame).
*/
public class msg_hil_gps extends MAVLinkMessage {

    public static final int MAVLINK_MSG_ID_HIL_GPS = 113;
    public static final int MAVLINK_MSG_LENGTH = 36;
    private static final long serialVersionUID = MAVLINK_MSG_ID_HIL_GPS;


      
    /**
    * Timestamp (microseconds since UNIX epoch or microseconds since system boot)
    */
    public long time_usec;
      
    /**
    * Latitude (WGS84), in degrees * 1E7
    */
    public int lat;
      
    /**
    * Longitude (WGS84), in degrees * 1E7
    */
    public int lon;
      
    /**
    * Altitude (WGS84), in meters * 1000 (positive for up)
    */
    public int alt;
      
    /**
    * GPS HDOP horizontal dilution of position in cm (m*100). If unknown, set to: 65535
    */
    public int eph;
      
    /**
    * GPS VDOP vertical dilution of position in cm (m*100). If unknown, set to: 65535
    */
    public int epv;
      
    /**
    * GPS ground speed (m/s * 100). If unknown, set to: 65535
    */
    public int vel;
      
    /**
    * GPS velocity in cm/s in NORTH direction in earth-fixed NED frame
    */
    public short vn;
      
    /**
    * GPS velocity in cm/s in EAST direction in earth-fixed NED frame
    */
    public short ve;
      
    /**
    * GPS velocity in cm/s in DOWN direction in earth-fixed NED frame
    */
    public short vd;
      
    /**
    * Course over ground (NOT heading, but direction of movement) in degrees * 100, 0.0..359.99 degrees. If unknown, set to: 65535
    */
    public int cog;
      
    /**
    * 0-1: no fix, 2: 2D fix, 3: 3D fix. Some applications will not use the value of this field unless it is at least two, so always correctly fill in the fix.
    */
    public short fix_type;
      
    /**
    * Number of satellites visible. If unknown, set to 255
    */
    public short satellites_visible;
    

    /**
    * Generates the payload for a mavlink message for a message of this type
    * @return
    */
    public MAVLinkPacket pack(){
        MAVLinkPacket packet = new MAVLinkPacket();
        packet.len = MAVLINK_MSG_LENGTH;
        packet.sysid = 255;
        packet.compid = 190;
        packet.msgid = MAVLINK_MSG_ID_HIL_GPS;
              
        packet.payload.putUnsignedLong(time_usec);
              
        packet.payload.putInt(lat);
              
        packet.payload.putInt(lon);
              
        packet.payload.putInt(alt);
              
        packet.payload.putUnsignedShort(eph);
              
        packet.payload.putUnsignedShort(epv);
              
        packet.payload.putUnsignedShort(vel);
              
        packet.payload.putShort(vn);
              
        packet.payload.putShort(ve);
              
        packet.payload.putShort(vd);
              
        packet.payload.putUnsignedShort(cog);
              
        packet.payload.putUnsignedByte(fix_type);
              
        packet.payload.putUnsignedByte(satellites_visible);
        
        return packet;
    }

    /**
    * Decode a hil_gps message into this class fields
    *
    * @param payload The message to decode
    */
    public void unpack(MAVLinkPayload payload) {
        payload.resetIndex();
              
        this.time_usec = payload.getUnsignedLong();
              
        this.lat = payload.getInt();
              
        this.lon = payload.getInt();
              
        this.alt = payload.getInt();
              
        this.eph = payload.getUnsignedShort();
              
        this.epv = payload.getUnsignedShort();
              
        this.vel = payload.getUnsignedShort();
              
        this.vn = payload.getShort();
              
        this.ve = payload.getShort();
              
        this.vd = payload.getShort();
              
        this.cog = payload.getUnsignedShort();
              
        this.fix_type = payload.getUnsignedByte();
              
        this.satellites_visible = payload.getUnsignedByte();
        
    }

    /**
    * Constructor for a new message, just initializes the msgid
    */
    public msg_hil_gps(){
        msgid = MAVLINK_MSG_ID_HIL_GPS;
    }

    /**
    * Constructor for a new message, initializes the message with the payload
    * from a mavlink packet
    *
    */
    public msg_hil_gps(MAVLinkPacket mavLinkPacket){
        this.sysid = mavLinkPacket.sysid;
        this.compid = mavLinkPacket.compid;
        this.msgid = MAVLINK_MSG_ID_HIL_GPS;
        unpack(mavLinkPacket.payload);        
    }

                              
    /**
    * Returns a string with the MSG name and data
    */
    public String toString(){
        return "MAVLINK_MSG_ID_HIL_GPS -"+" time_usec:"+time_usec+" lat:"+lat+" lon:"+lon+" alt:"+alt+" eph:"+eph+" epv:"+epv+" vel:"+vel+" vn:"+vn+" ve:"+ve+" vd:"+vd+" cog:"+cog+" fix_type:"+fix_type+" satellites_visible:"+satellites_visible+"";
    }
}
        