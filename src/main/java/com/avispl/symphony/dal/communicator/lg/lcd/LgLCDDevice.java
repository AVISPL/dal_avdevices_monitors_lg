package com.avispl.symphony.dal.communicator.lg.lcd;

import com.avispl.symphony.api.dal.control.Controller;
import com.avispl.symphony.api.dal.dto.control.ControllableProperty;
import com.avispl.symphony.api.dal.dto.monitor.ExtendedStatistics;
import com.avispl.symphony.api.dal.dto.monitor.Statistics;
import com.avispl.symphony.api.dal.monitor.Monitorable;
import com.avispl.symphony.dal.communicator.SocketCommunicator;

import java.util.*;

import static com.avispl.symphony.dal.communicator.lg.lcd.LgLCDConstants.*;

public class LgLCDDevice extends SocketCommunicator implements Controller, Monitorable {

    private int monitorID;

    /**
     * Constructor set the TCP/IP port to be used as well the default monitor ID
     */
    public LgLCDDevice(){
        super();

        this.setPort(9761);
        this.monitorID = 1;

        // set list of command success strings (included at the end of response when command succeeds, typically ending with command prompt)
        this.setCommandSuccessList(Collections.singletonList("OK"));
        // set list of error response strings (included at the end of response when command fails, typically ending with command prompt)
        this.setCommandErrorList(Collections.singletonList("NG"));
    }

    /**
     * This method is recalled by Symphony to control specific property
     * @param controllableProperty This is the property to be controled
     */
    @Override
    public void controlProperty(ControllableProperty controllableProperty) throws Exception {
        if (controllableProperty.getProperty().equals(controlProperties.power.name())){
            if(controllableProperty.getValue().toString().equals("1")){
                powerON();
            }else if(controllableProperty.getValue().toString().equals("0")){
                powerOFF();
            }
        }
    }

    /**
     * This method is recalled by Symphony to control a list of properties
     * @param controllableProperties This is the list of properties to be controlled
     * @return byte This returns the calculated xor checksum.
     */
    @Override
    public void controlProperties(List<ControllableProperty> controllableProperties) throws Exception {
        controllableProperties.stream().forEach(p -> {
            try {
                controlProperty(p);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    /**
     * This method is recalled by Symphony to get the list of statistics to be displayed
     * @return List<Statistics> This return the list of statistics.
     */
    @Override
    public List<Statistics> getMultipleStatistics() throws Exception {
        ExtendedStatistics extendedStatistics = new ExtendedStatistics();

        //controllable statistics
        Map<String, String> controllable = new HashMap<String, String>(){{
            put(controlProperties.power.name(),"Toggle");
        }};

        //statistics
        Map<String, String> statistics = new HashMap<String, String>();

        //getting power status from device
        String power;

        try {
            power = getPower().name();
            if(power.compareTo("ON") == 0) {
                statistics.put(statisticsProperties.power.name(), "1");
            }else if(power.compareTo("OFF") == 0)
            {
                statistics.put(statisticsProperties.power.name(), "0");
            }
        }catch (Exception e) {
            if (this.logger.isDebugEnabled()) {
                this.logger.debug("error during getPower", e);
            }
            throw e;
        }

        //getting input status from device
        try {
            statistics.put(statisticsProperties.input.name(), getInput().name());
        }catch (Exception e) {
            if (this.logger.isDebugEnabled()) {
                this.logger.debug("error during getInput", e);
            }
            throw e;
        }

        //getting temperature status from device
        try {
            statistics.put(statisticsProperties.temperature.name(), String.valueOf(getTemperature()));
        }catch (Exception e) {
            if (this.logger.isDebugEnabled()) {
                this.logger.debug("error during getInput", e);
            }
            throw e;
        }

        //getting fan status from device
        try {
            statistics.put(statisticsProperties.fan.name(), getFanStatus().name());
        }catch (Exception e) {
            if (this.logger.isDebugEnabled()) {
                this.logger.debug("error during getFanStatus", e);
            }
            throw e;
        }

        //getting sync status from device
        try {
            statistics.put(statisticsProperties.signal.name(), getSyncStatus().name());
        }catch (Exception e) {
            if (this.logger.isDebugEnabled()) {
                this.logger.debug("error during getSyncStatus", e);
            }
            throw e;
        }

        destroyChannel();

        extendedStatistics.setControl(controllable);
        extendedStatistics.setStatistics(statistics);

        return new ArrayList<Statistics>(Collections.singleton(extendedStatistics));
    }

    /**
     * This method is recalled by Symphony to get the current monitor ID (Future purpose)
     * @return int This returns the current monitor ID.
     */
    public int getMonitorID() {
        return monitorID;
    }

    /**
     * This method is is used by Symphony to set the monitor ID (FUture purpose)
     * @param monitorID This is the monitor ID to be set
     */
    public void setMonitorID(int monitorID) {
        this.monitorID = monitorID;
    }

    /**
     * This method is used to get the current display power status
     * @return powerStatus This returns the calculated xor checksum.
     */
    private powerStatusNames getPower(){

        try {
            byte[]  response = send(LgLCDUtils.buildSendString((byte) monitorID, commands.get(commandNames.POWER), commands.get(commandNames.GET)));

            powerStatusNames power = (powerStatusNames) digestResponse(response, commandNames.POWER);

            if (power == null) {
                return powerStatusNames.OFF;
            } else {
                return power;
            }

        }
        catch (Exception e){
            if (this.logger.isErrorEnabled()) {
                this.logger.error("error during get power send", e);
            }
        }
        return null;
    }

   private void powerON(){
        try {
            byte[]  response = send(LgLCDUtils.buildSendString((byte) monitorID, commands.get(commandNames.POWER), powerStatus.get(powerStatusNames.ON)));

            digestResponse(response,commandNames.POWER);
        } catch (Exception e) {
            if (this.logger.isDebugEnabled()) {
                this.logger.debug("error during power OFF send", e);
            }
        }
    }

    private void powerOFF(){
        try {
            byte[]  response = send(LgLCDUtils.buildSendString((byte) monitorID, commands.get(commandNames.POWER), powerStatus.get(powerStatusNames.OFF)));

            digestResponse(response,commandNames.POWER);
        } catch (Exception e) {
            if (this.logger.isDebugEnabled()) {
                this.logger.debug("error during power ON send", e);
            }
        }
    }

    /**
     * This method is used to get the current display input
     * @return inputNames This returns the current input.
     */
    private inputNames getInput(){
        try {
            byte[]  response = send(LgLCDUtils.buildSendString((byte) monitorID, commands.get(commandNames.INPUT), commands.get(commandNames.GET)));

            inputNames input = (inputNames) digestResponse(response, commandNames.INPUT);

            return input;
        }catch (Exception e){
            System.out.println("Connect exception");
            return inputNames.OFF;
        }
    }

    /**
     * This method is used to get the current fan status
     * @return fanStatusNames This returns the current display fan status.
     */
    private fanStatusNames getFanStatus(){
        try {
            byte[]  response = send(LgLCDUtils.buildSendString((byte) monitorID, commands.get(commandNames.FANSTATUS), commands.get(commandNames.GET)));

            fanStatusNames fanStatus = (fanStatusNames) digestResponse(response, commandNames.FANSTATUS);

            return fanStatus;
        }catch (Exception e){
            System.out.println("Connect exception");
            return fanStatusNames.NO_FAN;
        }
    }

    /**
     * This method is used to get the current display temperature
     * @return int This returns the current display temperature.
     */
    private Integer getTemperature(){
        try {
            byte[]  response = send(LgLCDUtils.buildSendString((byte) monitorID, commands.get(commandNames.TEMPERATURE), commands.get(commandNames.GET)));

            Integer temperature = (Integer) digestResponse(response, commandNames.TEMPERATURE);

            return temperature;
        }catch (Exception e){
            System.out.println("Connect exception");
            return 0;
        }
    }

    /**
     * This method is used to get the current display sync status
     * @return syncStatusNames This returns the current display sync status.
     */
    private syncStatusNames getSyncStatus(){
        try {
            byte[]  response = send(LgLCDUtils.buildSendString((byte) monitorID, commands.get(commandNames.STATUS), signalStatus));

            syncStatusNames status = (syncStatusNames) digestResponse(response, commandNames.STATUS);

            return status;
        }catch (Exception e){
            System.out.println("Connect exception");
            return syncStatusNames.NO_SYNC;
        }
    }

    /**
     * This method is used to digest the response received from the device
     * @param response This is the response to be digested
     * @param expectedResponse This is the expected response type to be compared with received
     * @return Object This returns the result digested from the response.
     */
    private Object digestResponse(byte[] response, commandNames expectedResponse){

        if(response[0] == commands.get(expectedResponse)[1])
        {
            byte[] responseStatus = Arrays.copyOfRange(response,5,7);

            if(Arrays.equals(responseStatus,replyStatusCodes.get(replyStatusNames.OK))) {

                byte[] reply = Arrays.copyOfRange(response,7,9);

                switch (expectedResponse) {
                    case POWER: {

                        for(Map.Entry<powerStatusNames,byte[]> entry: powerStatus.entrySet())
                        {
                            if(Arrays.equals(reply,entry.getValue()))
                            {
                                powerStatusNames power = entry.getKey();
                                return power;
                            }
                        }
                    }
                    case INPUT: {
                        for(Map.Entry<inputNames,byte[]> entry: inputs.entrySet())
                        {
                            if(Arrays.equals(reply,entry.getValue()))
                            {
                                inputNames input = entry.getKey();
                                return input;
                            }
                        }
                    }
                    case TEMPERATURE: {
                        return Integer.parseInt(new String(reply),16);
                    }
                    case FANSTATUS: {
                        for(Map.Entry<fanStatusNames,byte[]> entry: fanStatusCodes.entrySet())
                        {
                            if(Arrays.equals(reply,entry.getValue()))
                            {
                                fanStatusNames fanStatus = entry.getKey();
                                return fanStatus;
                            }
                        }
                    }
                    case STATUS:
                    {
                        reply = Arrays.copyOfRange(response,7,11);
                        for(Map.Entry<syncStatusNames,byte[]> entry: syncStatusCodes.entrySet())
                        {
                            if(Arrays.equals(reply,entry.getValue()))
                            {
                                syncStatusNames syncStatus = entry.getKey();
                                return syncStatus;
                            }
                        }
                    }
                }
            }else if(Arrays.equals(responseStatus,replyStatusCodes.get(replyStatusNames.NG)))
            {
                switch (expectedResponse) {
                    case FANSTATUS: {
                        return  fanStatusNames.NOT_SUPPORTED;
                    }
                    default:
                    {
                        if (this.logger.isErrorEnabled()) {
                            this.logger.error("error: NG reply: " + this.host + " port: " + this.getPort());
                        }
                        throw new RuntimeException("NG reply");
                    }
                }
            }
        }else
        {
            if (this.logger.isErrorEnabled()) {
                this.logger.error("error: Unexpected reply: " + this.host + " port: " + this.getPort());
            }
            throw new RuntimeException("Unexpected reply");
        }

        return null;
    }
}
