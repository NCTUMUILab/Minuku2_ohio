/*
 * Copyright (c) 2016.
 *
 * DReflect and Minuku Libraries by Shriti Raj (shritir@umich.edu) and Neeraj Kumar(neerajk@uci.edu) is licensed under a Creative Commons Attribution-NonCommercial-ShareAlike 4.0 International License.
 * Based on a work at https://github.com/Shriti-UCI/Minuku-2.
 *
 *
 * You are free to (only if you meet the terms mentioned below) :
 *
 * Share — copy and redistribute the material in any medium or format
 * Adapt — remix, transform, and build upon the material
 *
 * The licensor cannot revoke these freedoms as long as you follow the license terms.
 *
 * Under the following terms:
 *
 * Attribution — You must give appropriate credit, provide a link to the license, and indicate if changes were made. You may do so in any reasonable manner, but not in any way that suggests the licensor endorses you or your use.
 * NonCommercial — You may not use the material for commercial purposes.
 * ShareAlike — If you remix, transform, or build upon the material, you must distribute your contributions under the same license as the original.
 * No additional restrictions — You may not apply legal terms or technological measures that legally restrict others from doing anything the license permits.
 */

package edu.ohio.minuku.config;

/**
 * Created by shriti on 7/17/16.
 */
public class Constants {

    public static final long MILLISECONDS_PER_SECOND = 1000;
    public static final int SECONDS_PER_MINUTE = 60;
    public static final int MINUTES_PER_HOUR = 60;
    public static final int HOURS_PER_DAY = 24;
    public static final long MILLISECONDS_PER_DAY = HOURS_PER_DAY *MINUTES_PER_HOUR*SECONDS_PER_MINUTE*MILLISECONDS_PER_SECOND;
    public static final long MILLISECONDS_PER_HOUR = MINUTES_PER_HOUR*SECONDS_PER_MINUTE*MILLISECONDS_PER_SECOND;
    public static final long MILLISECONDS_PER_MINUTE = SECONDS_PER_MINUTE*MILLISECONDS_PER_SECOND;
    public final static String DATE_FORMAT_NOW_Dash = "yyyy-MM-dd HH:mm:ss Z";
    public final static String DATE_FORMAT_NOW_SLASH = "yyyy/MM/dd HH:mm:ss Z";
    public final static String DATE_FORMAT_NOW_MINUTE_SLASH = "yyyy/MM/dd HH:mm";
    public static final String DATE_FORMAT_NOW_NO_ZONE_Slash = "yyyy/MM/dd HH:mm:ss";
    public static final String DATE_FORMAT_NOW_DAY_Slash = "yyyy/MM/dd";
    public static final String DATE_FORMAT_NOW_NO_ZONE = "yyyy-MM-dd HH:mm:ss";
    public final static String DATE_FORMAT_NOW_HOUR_MIN_AMPM = "yyyy-MM-dd hh:mm a";
    public static final String DATE_FORMAT_NOW_DAY = "yyyy-MM-dd";
    public static final String DATE_FORMAT_NOW_HOUR = "yyyy-MM-dd HH";
    public static final String DATE_FORMAT_NOW_HOUR_MIN = "yyyy-MM-dd HH:mm";
    public static final String DATE_FORMAT_HOUR_MIN_SECOND = "HH:mm:ss";
    public static final String DATE_FORMAT_FOR_ID = "yyyyMMddHHmmss";
    public static final String DATE_FORMAT_HOUR_MIN = "HH:mm";
    public static final String DATE_FORMAT_HOUR = "HH";
    public static final String DATE_FORMAT_HOUR_small = "hh";
    public static final String DATE_FORMAT_MIN = "mm";
    public static final String DATE_FORMAT_HOUR_MIN_AMPM = "hh:mm a";
    public static final String DATE_FORMAT_DATE_TEXT = "MMM dd";
    public static final String DATE_FORMAT_DATE_TEXT_HOUR_MIN = "MMM dd HH:mm";
    public static final String DATE_FORMAT_DATE_TEXT_HOUR_MIN_AMPM = "MMM dd hh:mm a";
    public static final String DATE_FORMAT_AMPM = "a";

    public static final String DATE_FORMAT_DATE_TEXT_HOUR_MIN_SEC = "MMM dd  HH:mm:ss";
    public static final int DATA_FORMAT_TYPE_NOW=0;
    public static final int DATA_FORMAT_TYPE_DAY=1;
    public static final int DATA_FORMAT_TYPE_HOUR=2;

    public static final int SURVEYLINK_SHOULDNT_BEEN_SENT_FLAG = -1;
    public static final int SURVEYLINK_SHOULD_BE_SENT_FLAG = 0;
    public static final int SURVEYLINK_IS_ALREADY_SENT_FLAG = 1;

    public static final int SESSION_SHOULDNT_BEEN_SENT_FLAG = -1;
    public static final int SESSION_SHOULD_BE_SENT_FLAG = 0;
    public static final int SESSION_IS_ALREADY_SENT_FLAG = 1;

    public static final int SESSION_NEVER_GET_COMBINED_FLAG = 0;
    public static final int SESSION_IS_COMBINED_FLAG = 1;
    public static final int SESSION_SUBJECTIVELY_COMBINE_FLAG = 2;

    public static final String YES = "YES";
    public static final String NO = "NO";

    public static final int sleepTime_LowerBound = 4;
    public static final int sleepTime_UpperBound = 10;

    public static final String DELIMITER = ";;;";
    public static final String SESSION_DELIMITER = ",";
    public static final String ACTIVITY_DELIMITER = ";;";
    public static final String CONTEXT_SOURCE_DELIMITER = ":";
    public static final String DELIMITER_IN_COLUMN = "::";

    public static final String ONGOING_CHANNEL_NAME = "OSU";
    public static final String ONGOING_CHANNEL_ID = "Ongoing_id";
    public static final String SURVEY_CHANNEL_NAME = "OSU";
    public static final String SURVEY_CHANNEL_ID = "Survey_id";
    public static final String PERMIT_CHANNEL_NAME = "OSU";
    public static final String PERMIT_CHANNEL_ID = "Permit_id";
    public static final String SLEEPTIME_CHANNEL_NAME = "OSU";
    public static final String SLEEPTIME_CHANNEL_ID = "Sleeptime_id";

    public static final String sharedPrefString = "edu.umich.minuku_2";
    public static final String appNameString = "edu.ohio.minuku_2";

    //file path
    public static final String PACKAGE_DIRECTORY_PATH="/Android/data/edu.ohio.minuku_2/";


    public static final String ANNOTATION_TAG_DETECTED_TRANSPORTATOIN_ACTIVITY = "detected-transportation";

    public static final String CHECK_SERVICE_ACTION = "checkService";

    public static final String CONNECTIVITY_CHANGE = "CONNECTIVITY_CHANGE";

    // Prompt service related constants
    public static final long PROMPT_SERVICE_REPEAT_MILLISECONDS = MILLISECONDS_PER_MINUTE * 30;
    public static final int STREAM_UPDATE_DELAY = 10;
    public static final int STREAM_UPDATE_FREQUENCY = 10;
    public static final int STREAM_UPDATE_THREAD_SIZE = 1;

    //default queue size
    public static final int DEFAULT_QUEUE_SIZE = 20;

    //specific queue sizes
    public static final int LOCATION_QUEUE_SIZE = 50;


    public static final String APP_NAME = "DMS";
    public static final String APP_FULL_NAME = "Daily Mobility Study";

    public static final String NOTIFICATION_TEXT_DAY_0 = "Part B begins at 12am and continues for 2 weeks.";
    public static final String NOTIFICATION_TEXT_LOCATION = "Please check your location permission.";
    public static final String NOTIFICATION_TEXT_APPUSAGE = "Please check your usage access permission.";
    public static final String NOTIFICATION_TEXT_NEW_TRIP = " New Trip";
    public static final String NOTIFICATION_TEXT_NEW_TRIPS = " New Trips";
    public static final String NOTIFICATION_TEXT_FINAL_DAY_PLUS_1_WITH_TRIPS = "Your final trips are available now.";
    public static final String NOTIFICATION_TEXT_FINAL_DAY_PLUS_1_WITHOUT_TRIPS = "Part C will become available tomorrow.";
    public static final String NOTIFICATION_TEXT_AFTER_FINAL_DAY_PLUS_1 = "Part C is available today and lasts 20 mins.";
    public static final String NOTIFICATION_TEXT_AFTER_FINAL_DAY_PLUS_1_WAIT_DATA_TRANSFER
            = "Connect your phone to WiFi to finish the study.";

    public static final long DATA_TRANSFER_TIMEOUT = 10 * MILLISECONDS_PER_SECOND; // 1 * 10 * 1000

    public static final long INTERNAL_LOCATION_UPDATE_FREQUENCY = 1 * 10 * MILLISECONDS_PER_SECOND; // 1 * 10 * 1000

    public static final String NOT_A_NUMBER = "NA";

    public static final int INVALID_IN_INT = -1;
    public static final long INVALID_IN_LONG = -999;
    public static final String INVALID_IN_STRING = "NA";

    public static final String SURVEY_INCOMPLETE_FLAG = "0";
    public static final String SURVEY_COMPLETE_FLAG = "1";
    public static final String SURVEY_ERROR_FLAG = "2";

    public static final String TEXT_TO_SERVER_SURVEY_INCOMPLETE = "incomplete";
    public static final String TEXT_TO_SERVER_SURVEY_COMPLETE = "complete";
    public static final String TEXT_TO_SERVER_SURVEY_ERROR = "error";

    public static final int FINALDAY = 14; //real: 14, test: 3


    public static final String CHECK_IN_URL = "http://mcog.asc.ohio-state.edu/apps/servicerec?";
    public static final String CHECK_IN_URL_USER_INFORM = "http://mcog.asc.ohio-state.edu/apps/useridcheck?";

    public static final String FINAL_SURVEY_URL_NCTU = "https://nctucommunication.qualtrics.com/jfe/form/SV_aVS9WRCNnfgRNGd";
    public static final String FINAL_SURVEY_URL_OHIO = "https://osu.az1.qualtrics.com/jfe/form/SV_2bPukwuNSojU4Sx";
    public static final String FINAL_SURVEY_URL = FINAL_SURVEY_URL_OHIO;

    public static final String HELP_URL = "http://u.osu.edu/dailymobilitystudy/";


    public static final String INTERVAL_SAMPLE = "INTERVAL_SAMPLE";

    public static final String ACTIVITY_CONFIDENCE_CONNECTOR = ":";

    public static final long invalidLong = -999;

}
