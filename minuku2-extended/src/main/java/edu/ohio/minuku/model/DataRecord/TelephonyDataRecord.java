package edu.ohio.minuku.model.DataRecord;

import java.util.Calendar;
import java.util.Date;

import edu.ohio.minukucore.model.DataRecord;

/**
 * Created by Lawrence on 2017/7/22.
 */

public class TelephonyDataRecord implements DataRecord {

    private final String TAG = "TelephonyDataRecord";

    public String base64Data;
    public long creationTime;

    private String carrierName;
    private String callState;

    private int LTESignalStrength_dbm;
    private int LTESignalStrength_asu;
    private int CdmaSignalStrenth;
    private int CdmaSignalStrenthLevel; // 1, 2, 3, 4

    private int taskDayCount;
    private long hour;
    private String NetworkOperatorName = "NA";
    private int CallState = -9999;
    private int PhoneSignalType = -9999;
    private int GsmSignalStrength = -9999;
    private int LTESignalStrength = -9999;
    private int CdmaSignalStrengthLevel = -9999;
    private String sessionid;

    public TelephonyDataRecord(String base64Data) {
        this.base64Data = base64Data;
        this.creationTime = new Date().getTime();
    }

    public TelephonyDataRecord(String NetworkOperatorName, int CallState, int PhoneSignalType
            , int GsmSignalStrength, int LTESignalStrength, int CdmaSignalStrengthLevel) {
        this.creationTime = new Date().getTime();
        this.NetworkOperatorName = NetworkOperatorName;
        this.CallState = CallState;
        this.PhoneSignalType = PhoneSignalType;
        this.GsmSignalStrength = GsmSignalStrength;
        this.LTESignalStrength = LTESignalStrength;
        this.CdmaSignalStrengthLevel = CdmaSignalStrengthLevel;

//        Log.d(TAG,"mNetworkOperatorName : "+ NetworkOperatorName +" mCallState : "+ CallState+" mPhoneSignalType : "+ PhoneSignalType
//                +" mGsmSignalStrength : "+ GsmSignalStrength+" mLTESignalStrength : "+ LTESignalStrength
//                +" mCdmaSignalStrenthLevel : "+ CdmaSignalStrengthLevel);
    }

    public TelephonyDataRecord(String NetworkOperatorName, int CallState, int PhoneSignalType
            , int GsmSignalStrength, int LTESignalStrength, int CdmaSignalStrengthLevel, String sessionid) {
        this.creationTime = new Date().getTime();
        this.NetworkOperatorName = NetworkOperatorName;
        this.CallState = CallState;
        this.PhoneSignalType = PhoneSignalType;
        this.GsmSignalStrength = GsmSignalStrength;
        this.LTESignalStrength = LTESignalStrength;
        this.CdmaSignalStrengthLevel = CdmaSignalStrengthLevel;
        this.sessionid = sessionid;
    }

    public String getSessionid() {
        return sessionid;
    }

    private long getmillisecondToHour(long timeStamp){

        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(timeStamp);

        long mhour = calendar.get(Calendar.HOUR_OF_DAY);

        return mhour;

    }

    @Override
    public long getCreationTime() {

        return creationTime;
    }

    public String getCarrierName(){
        return carrierName;
    }

    public String getCallState(){
        return callState;
    }

    public String getNetworkOperatorName() {
        return NetworkOperatorName;
    }

    public int getLTESignalStrength_dbm(){
        return LTESignalStrength_dbm;
    }

    public int getLTESignalStrength_asu(){
        return LTESignalStrength_asu;
    }

    public int getGsmSignalStrength(){
        return GsmSignalStrength;
    }

    public int getCdmaSignalStrenth(){
        return CdmaSignalStrenth;
    }

    public int getCdmaSignalStrenthLevel(){
        return CdmaSignalStrenthLevel;
    }

    public int getPhoneSignalType() {
        return PhoneSignalType;
    }

    public int getLTESignalStrength() {
        return LTESignalStrength;
    }

    public int getCdmaSignalStrengthLevel() {
        return CdmaSignalStrengthLevel;
    }
}
