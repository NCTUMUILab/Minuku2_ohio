package edu.ohio.minuku.streamgenerator;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Handler;

import org.greenrobot.eventbus.EventBus;

import edu.ohio.minuku.config.Constants;
import edu.ohio.minuku.dao.ConnectivityDataRecordDAO;
import edu.ohio.minuku.logger.Log;
import edu.ohio.minuku.manager.MinukuDAOManager;
import edu.ohio.minuku.manager.MinukuStreamManager;
import edu.ohio.minuku.manager.SessionManager;
import edu.ohio.minuku.model.DataRecord.ConnectivityDataRecord;
import edu.ohio.minuku.stream.ConnectivityStream;
import edu.ohio.minukucore.dao.DAOException;
import edu.ohio.minukucore.exception.StreamAlreadyExistsException;
import edu.ohio.minukucore.exception.StreamNotFoundException;
import edu.ohio.minukucore.stream.Stream;

/**
 * Created by Lawrence on 2017/8/22.
 */

public class ConnectivityStreamGenerator extends AndroidStreamGenerator<ConnectivityDataRecord> {

    private final String TAG = "ConnectivityStreamGenerator";

    private Context mContext;

    public static String NETWORK_TYPE_WIFI = "Wifi";
    public static String NETWORK_TYPE_MOBILE = "Mobile";

    private static boolean mIsNetworkAvailable = false;
    private static boolean mIsConnected = false;
    private static boolean mIsWifiAvailable = false;
    private static boolean mIsMobileAvailable = false;
    public static boolean mIsWifiConnected = false;
    public static boolean mIsMobileConnected = false;
    public static String mNetworkType = "NA";

    public static int mainThreadUpdateFrequencyInSeconds = 5;
    public static long mainThreadUpdateFrequencyInMilliseconds = mainThreadUpdateFrequencyInSeconds *Constants.MILLISECONDS_PER_SECOND;

    private static Handler mMainThread;

    private static ConnectivityManager mConnectivityManager;

    private ConnectivityStream mStream;
    private ConnectivityDataRecordDAO mDAO;

    public static ConnectivityDataRecord toOtherconnectDataRecord;

    public ConnectivityStreamGenerator(){

        mConnectivityManager = (ConnectivityManager)mContext.getSystemService(mContext.CONNECTIVITY_SERVICE);

    }

    public ConnectivityStreamGenerator(Context applicationContext){
        super(applicationContext);

        mContext = applicationContext;

        this.mStream = new ConnectivityStream(Constants.DEFAULT_QUEUE_SIZE);
        this.mDAO = MinukuDAOManager.getInstance().getDaoFor(ConnectivityDataRecord.class);

        mConnectivityManager = (ConnectivityManager)mContext.getSystemService(mContext.CONNECTIVITY_SERVICE);


        this.register();
    }

    @Override
    public void register() {
        Log.d(TAG, "Registering with StreamManager.");
        try {
            MinukuStreamManager.getInstance().register(mStream, ConnectivityDataRecord.class, this);
        } catch (StreamNotFoundException streamNotFoundException) {
            Log.e(TAG, "One of the streams on which ConnectivityDataRecord depends in not found.");
        } catch (StreamAlreadyExistsException streamAlreadyExistsException) {
            Log.e(TAG, "Another stream which provides ConnectivityDataRecord is already registered.");
        }
    }

    @Override
    public Stream<ConnectivityDataRecord> generateNewStream() {
        return mStream;
    }

    @Override
    public boolean updateStream() {

        Log.d(TAG, "updateStream called");

        int session_id = 0;

        int countOfOngoingSession = SessionManager.getInstance().getOngoingSessionIdList().size();

        //if there exists an ongoing session
        if (countOfOngoingSession>0){
            session_id = SessionManager.getInstance().getOngoingSessionIdList().get(0);
        }

        ConnectivityDataRecord connectivityDataRecord =
                new ConnectivityDataRecord(mNetworkType,mIsNetworkAvailable, mIsConnected, mIsWifiAvailable,
                        mIsMobileAvailable, mIsWifiConnected, mIsMobileConnected, String.valueOf(session_id));

        toOtherconnectDataRecord = connectivityDataRecord;

        mStream.add(connectivityDataRecord);
        Log.d(TAG, "Connectivity to be sent to event bus" + connectivityDataRecord);
        // also post an event.
        EventBus.getDefault().post(connectivityDataRecord);
        try {

            mDAO.add(connectivityDataRecord);
        } catch (DAOException e) {
            e.printStackTrace();
            return false;
        }catch (NullPointerException e){ //Sometimes no data is normal
            e.printStackTrace();
            return false;
        }

        return true;
    }

    @Override
    public long getUpdateFrequency() {
        return 1;
    }

    @Override
    public void sendStateChangeEvent() {

    }

    @Override
    public void onStreamRegistration() {
        Log.e(TAG,"onStreamRegistration");

        runPhoneStatusMainThread();

    }

    public void runPhoneStatusMainThread(){

        Log.d(TAG, "runPhoneStatusMainThread") ;

        mMainThread = new Handler();

        Runnable runnable = new Runnable() {
            @Override
            public void run() {

                getNetworkConnectivityUpdate();

                mMainThread.postDelayed(this, mainThreadUpdateFrequencyInMilliseconds);
            }
        };

        mMainThread.post(runnable);
    }

    private void getNetworkConnectivityUpdate(){

        mIsNetworkAvailable = false;
        mIsConnected = false;
        mIsWifiAvailable = false;
        mIsMobileAvailable = false;
        mIsWifiConnected = false;
        mIsMobileConnected = false;
        mNetworkType = "NA";

        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {

            Network[] networks = mConnectivityManager.getAllNetworks();

            NetworkInfo activeNetwork;

            for (Network network : networks) {
                activeNetwork = mConnectivityManager.getNetworkInfo(network);

                try {
                    if (activeNetwork.getType() == ConnectivityManager.TYPE_WIFI) {
                        mIsWifiAvailable = activeNetwork.isAvailable();
                        mIsWifiConnected = activeNetwork.isConnected();
                    } else if (activeNetwork.getType() == ConnectivityManager.TYPE_MOBILE) {
                        mIsMobileAvailable = activeNetwork.isAvailable();
                        mIsMobileConnected = activeNetwork.isConnected();
                    }
                }catch (NullPointerException e){
                    e.printStackTrace();
                }

            }

            Log.d(TAG, "activeNetworkWifi : "+ mIsWifiConnected);
            Log.d(TAG, "activeNetworkMobile : "+ mIsMobileConnected);


            if (mIsWifiConnected) {
                mNetworkType = NETWORK_TYPE_WIFI;
            }
            else if (mIsMobileConnected) {
                mNetworkType = NETWORK_TYPE_MOBILE;
            }

            mIsNetworkAvailable = mIsWifiAvailable | mIsMobileAvailable;
            mIsConnected = mIsWifiConnected | mIsMobileConnected;


            Log.d(TAG, "[test save records] connectivity change available? WIFI: available " + mIsWifiAvailable  +
                    "  mIsConnected: " + mIsWifiConnected + " Mobile: available: " + mIsMobileAvailable + " mIs connected: " + mIsMobileConnected
                    +" network type: " + mNetworkType + ",  mIs connected: " + mIsConnected + " mIs network available " + mIsNetworkAvailable);


        } else{

            Log.d(TAG, "[test save records] api under lollipop " );


            if (mConnectivityManager!=null) {

                NetworkInfo activeNetworkWifi = mConnectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
                NetworkInfo activeNetworkMobile = mConnectivityManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);

                boolean isWiFi = activeNetworkWifi.getType() == ConnectivityManager.TYPE_WIFI;
                boolean isMobile = activeNetworkMobile.getType() == ConnectivityManager.TYPE_MOBILE;

                Log.d(TAG, "[test save records] connectivity change available? " + isWiFi);


                if(activeNetworkWifi !=null) {

                    mIsWifiConnected = activeNetworkWifi != null &&
                            activeNetworkWifi.isConnected();
                    mIsMobileConnected = activeNetworkWifi != null &&
                            activeNetworkMobile.isConnected();

                    mIsConnected = mIsWifiConnected | mIsMobileConnected;

                    mIsWifiAvailable = activeNetworkWifi.isAvailable();
                    mIsMobileAvailable = activeNetworkMobile.isAvailable();

                    mIsNetworkAvailable = mIsWifiAvailable | mIsMobileAvailable;


                    if (mIsWifiConnected) {
                        mNetworkType = NETWORK_TYPE_WIFI;
                    }

                    else if (mIsMobileConnected) {
                        mNetworkType = NETWORK_TYPE_MOBILE;
                    }


                    //assign value
//
                    Log.d(TAG, "[test save records] connectivity change available? WIFI: available " + mIsWifiAvailable  +
                            "  mIsConnected: " + mIsWifiConnected + " Mobile: available: " + mIsMobileAvailable + " mIs connected: " + mIsMobileConnected
                            +" network type: " + mNetworkType + ",  mIs connected: " + mIsConnected + " mIs network available " + mIsNetworkAvailable);

                }
            }

        }
    }

    @Override
    public void offer(ConnectivityDataRecord dataRecord) {

    }
}
