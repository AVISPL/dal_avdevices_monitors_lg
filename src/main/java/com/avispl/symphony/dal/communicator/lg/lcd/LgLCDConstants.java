package com.avispl.symphony.dal.communicator.lg.lcd;

import java.util.HashMap;
import java.util.Map;

public class LgLCDConstants {
    enum powerStatusNames {ON,OFF}
    final static Map<powerStatusNames,byte[]> powerStatus = new HashMap<powerStatusNames,byte[]>(){{
        put(powerStatusNames.ON, new byte[] {'0','0'});
        put(powerStatusNames.OFF, new byte[] {'0','1'});
    }};

    enum replyStatusNames {OK,NG}
    final static Map<replyStatusNames,byte[]> replyStatusCodes = new HashMap<replyStatusNames,byte[]>(){{
        put(replyStatusNames.OK, new byte[] {'O','K'});
        put(replyStatusNames.NG, new byte[] {'N','G'});
    }};

    enum fanStatusNames {FAULTY,NORMAL,NO_FAN,NOT_SUPPORTED}
    final static Map<fanStatusNames,byte[]> fanStatusCodes = new HashMap<fanStatusNames,byte[]>(){{
        put(fanStatusNames.FAULTY, new byte[] {'0','0'});
        put(fanStatusNames.NORMAL, new byte[] {'0','1'});
        put(fanStatusNames.NO_FAN, new byte[] {'0','2'});
    }};

    enum commandNames{POWER,INPUT,TEMPERATURE,FANSTATUS,STATUS,GET}
    final static Map<commandNames, byte[]> commands = new HashMap<commandNames,byte[]>(){{
        put(commandNames.POWER, new byte[] {'k','d'});
        put(commandNames.INPUT, new byte[] {'x','b'});
        put(commandNames.TEMPERATURE, new byte[] {'d','n'});
        put(commandNames.FANSTATUS, new byte[] {'d','w'});
        put(commandNames.STATUS, new byte[] {'s','v'});
        put(commandNames.GET, new byte[] {'F','F'});
    }};

    final static byte signalStatus[] = {'0','2',' ','F','F'};

    enum syncStatusNames {NO_SYNC,SYNC}
    final static Map<syncStatusNames,byte[]> syncStatusCodes = new HashMap<syncStatusNames,byte[]>(){{
        put(syncStatusNames.NO_SYNC, new byte[] {'0','2','0','0'});
        put(syncStatusNames.SYNC, new byte[] {'0','2','0','1'});
    }};

    enum inputNames {AV,COMPONENT,RGB,DVI_D_PC,DVI_D_DTV,HDMI1_DTV,HDMI1_PC,HDMI2_OPS_DTV,HDMI2_OPS_PC,HDMI3_OPS_DVID_DTV,HDMI3_OPS_DVID_PC,OPS_DVID_DTV,OPS_DVID_PC,HDMI3_DVID_DTV,HDMI3_DVID_PC,OPS_DTV,OPS_PC,DISPLAYPORT_DTV,DISPLAYPORT_PC,SUPERSIGN_PLAYER,OTHERS,MULTI_SCREEN,OFF}
    final static Map<inputNames, byte[]> inputs = new HashMap<inputNames, byte[]>() {{
        put(inputNames.AV, new byte[] {'2','0'});
        put(inputNames.COMPONENT, new byte[] {'4','0'});
        put(inputNames.RGB, new byte[] {'6','0'});
        put(inputNames.DVI_D_PC, new byte[] {'7','0'});
        put(inputNames.DVI_D_DTV, new byte[] {'8','0'});
        put(inputNames.HDMI1_DTV, new byte[] {'9','0'});
        put(inputNames.HDMI1_PC, new byte[] {'a','0'});
        put(inputNames.HDMI2_OPS_DTV, new byte[] {'9','1'});
        put(inputNames.HDMI2_OPS_PC, new byte[] {'a','1'});
        put(inputNames.HDMI3_OPS_DVID_DTV, new byte[] {'9','2'});
        put(inputNames.HDMI3_OPS_DVID_PC, new byte[] {'a','2'});
        put(inputNames.OPS_DVID_DTV, new byte[] {'9','5'});
        put(inputNames.OPS_DVID_PC, new byte[] {'a','5'});
        put(inputNames.HDMI3_DVID_DTV, new byte[] {'9','6'});
        put(inputNames.HDMI3_DVID_PC, new byte[] {'a','6'});
        put(inputNames.OPS_DTV, new byte[] {'9','8'});
        put(inputNames.OPS_PC, new byte[] {'a','8'});
        put(inputNames.DISPLAYPORT_DTV, new byte[] {'c','0'});
        put(inputNames.DISPLAYPORT_PC, new byte[] {'d','0'});
        put(inputNames.SUPERSIGN_PLAYER, new byte[] {'e','0'});
        put(inputNames.OTHERS, new byte[] {'e','1'});
        put(inputNames.MULTI_SCREEN, new byte[] {'e','2'});
    }};

    enum controlProperties {power,input}
    enum statisticsProperties {power,fan,input,temperature,signal}
}
