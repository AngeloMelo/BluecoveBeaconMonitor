package com.example.das.ufsc.beaconmonitorbluecove.utils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

import com.example.das.ufsc.beaconmonitorbluecove.ConnectionPerformanceInfo;

public class BeaconDefaults 
{
	public static final String NAME = "beacon";
	
	//UUID for beacon service
	public static final UUID MY_UUID = UUID.fromString("061d3751-78df-472c-8957-07df79497e71");
	public static final String STR_UUID = "061d375178df472c895707df79497e71";
	
	//JSON KEYS
	public static final String MAC_KEY = "MAC";
	public static final String OPP_MODE_KEY = "OPP_MODE";
	public static final String ACK_KEY = "ACK";
	public static final String TIC_KEY = "TIC";
	public static final String TIC_LINEID_KEY = "LINEID";
	public static final String TIC_LINENM_KEY = "LINENM";
	public static final String TIC_LASTSTOPNM_KEY = "LASTSTOPNM";
	
	public static final String DISCOVERYDATE_KEY = "DISCTS";
	public static final String BEACON_FOUND_TS_KEY = "BEACONFOUNDTS";
	public static final String FIRST_CONN_ACCEPTANCE_TS_KEY = "FIRSTCONNACCEPTANCETS";
	public static final String LAST_CONN_REQUEST_TS_KEY = "LASTCONNREQTS";
	public static final String LAST_AUTHENTIC_CONN_REQUEST_TS_KEY = "LASTAUTHENTICCONNREQTS";
	public static final String LAST_CONN_ACCEPTANCE_TS_KEY = "LASTCONNACCEPTANCETS";
	public static final String LAST_TIC_RECEIVED_TS_KEY = "LASTTICRECEIVEDTS";
	public static final String LAST_ACK_SENT_TS_KEY = "LASTACKSENTTS";
	public static final String MISSED_CALLS_KEY = "MISSEDCALLS";
	
	//OPP MODES
	public static final int OPP_MODE_AUTHENTIC = 0;
	public static final int OPP_MODE_DUBIOUS = 1;
	
	//INTERRUPT MODES
	public static final int INT_CLOSE_CONNECTION = -1;
	public static final int INT_NO_RECALL = -2;


	public static String getStrDate(Date dt)
	{
		SimpleDateFormat formater = new SimpleDateFormat("yyyyMMdd.HHmmss.SSS");
		return formater.format(dt);
	}

	public static String formatJson(String address, int oppMode, ConnectionPerformanceInfo connPerformanceInfo) 
	{
		Date startDiscoveryTs = connPerformanceInfo.getStartDiscoveryTS();
		Date beaconFoundTs = connPerformanceInfo.getBeaconFoundTS();
		Date firstConnAcceptanceTs = connPerformanceInfo.getFirstConnAcceptanceTS();
		
		Date lastConnRequest = connPerformanceInfo.getLastConnRequestTs();
		Date lastAuthenticConnRequestTs = connPerformanceInfo.getLastAuthenticConnRequestTs();

		Date lastConnAcceptanceTs = connPerformanceInfo.getLastConnAcceptanceTs();
		
		Date lastTickReceivedTs = connPerformanceInfo.getLastTicReceivedTs();
		Date lastAckSentTs = connPerformanceInfo.getLastAckSentTs();
		
		String jsonString = "{";
		jsonString = jsonString + BeaconDefaults.MAC_KEY + ":'" + address;
		jsonString = jsonString + "'," + BeaconDefaults.OPP_MODE_KEY + ":" + oppMode;
		jsonString = jsonString + "," + BeaconDefaults.ACK_KEY + ":'true'"; 
		jsonString = jsonString + "," + BeaconDefaults.DISCOVERYDATE_KEY + ":" + getStrDate(startDiscoveryTs); 
		jsonString = jsonString + "," + BeaconDefaults.BEACON_FOUND_TS_KEY + ":" + getStrDate(beaconFoundTs);
		jsonString = jsonString + "," + BeaconDefaults.FIRST_CONN_ACCEPTANCE_TS_KEY + ":" + getStrDate(firstConnAcceptanceTs);
		jsonString = jsonString + "," + BeaconDefaults.MISSED_CALLS_KEY + ":" + connPerformanceInfo.getMissedCalls();
		
		if(lastConnRequest != null)
		{
			jsonString = jsonString + "," + BeaconDefaults.LAST_CONN_REQUEST_TS_KEY + ":" + getStrDate(lastConnRequest);
		}

		if(lastAuthenticConnRequestTs != null)
		{
			jsonString = jsonString + "," + BeaconDefaults.LAST_AUTHENTIC_CONN_REQUEST_TS_KEY + ":" + getStrDate(lastAuthenticConnRequestTs);
		}

		if(lastConnAcceptanceTs != null)
		{
			jsonString = jsonString + "," + BeaconDefaults.LAST_CONN_ACCEPTANCE_TS_KEY + ":" + getStrDate(lastConnAcceptanceTs);
		}
		
		if(lastTickReceivedTs != null)
		{
			jsonString = jsonString + "," + BeaconDefaults.LAST_TIC_RECEIVED_TS_KEY + ":" + getStrDate(lastTickReceivedTs);
		}
		
		if(lastAckSentTs != null)
		{
			jsonString = jsonString + "," + BeaconDefaults.LAST_ACK_SENT_TS_KEY + ":" + getStrDate(lastAckSentTs);
		}
		jsonString = jsonString + "}";

		return jsonString;

	}
	
}
