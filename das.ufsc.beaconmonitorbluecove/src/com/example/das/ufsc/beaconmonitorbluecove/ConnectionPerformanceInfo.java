package com.example.das.ufsc.beaconmonitorbluecove;

import java.util.Date;

public class ConnectionPerformanceInfo 
{
	private Date startDiscoveryTS;
	private Date beaconFoundTS;
	private Date firstConnAcceptanceTS;	
	private Date lastAuthenticConnRequestTs;
	private Date lastConnRequestTs;
	private Date lastConnAcceptanceTs;
	private Date lastTicReceivedTs;
	private Date lastAckSentTs;

	private boolean firstConnection;
	private int missedCalls;
	
	public ConnectionPerformanceInfo()
	{
		super();
		this.firstConnection = true;
	}
	
	public Date getLastTicReceivedTs() {
		return lastTicReceivedTs;
	}

	public void setLastTicReceivedTs(Date lastTicReceivedTs) {
		this.lastTicReceivedTs = lastTicReceivedTs;
	}

	public Date getLastAckSentTs() {
		return lastAckSentTs;
	}

	public void setLastAckSentTs(Date lastAckSentTs) {
		this.lastAckSentTs = lastAckSentTs;
	}

	public Date getStartDiscoveryTS() {
		return startDiscoveryTS;
	}
	public void setStartDiscoveryTS(Date startDiscoveryTS) {
		this.startDiscoveryTS = startDiscoveryTS;
	}
	public Date getBeaconFoundTS() {
		return beaconFoundTS;
	}
	public void setBeaconFoundTS(Date beaconFoundTS) 
	{
		this.beaconFoundTS = beaconFoundTS;
		this.lastConnRequestTs = beaconFoundTS;
	}
	
	public Date getFirstConnAcceptanceTS() 
	{
		return firstConnAcceptanceTS;
	}
	
	public void setFirstConnAcceptanceTS(Date connDate) 
	{
		this.firstConnAcceptanceTS = connDate;
		this.lastConnAcceptanceTs = connDate;
		setFirstConnection(false);
	}

	public boolean isFirstConnection() 
	{
		return firstConnection;
	}

	private void setFirstConnection(boolean firstConnection) 
	{
		this.firstConnection = firstConnection;
	}

	public void setLastConnAcceptanceTs(Date date) 
	{
		this.lastConnAcceptanceTs = date;
	} 
	
	public Date getLastConnAcceptanceTs() 
	{
		return this.lastConnAcceptanceTs;
	}

	public Date getLastConnRequestTs() 
	{
		return this.lastConnRequestTs;
	}

	public void setLastConnRequestTs(Date date) 
	{
		this.lastConnRequestTs = date;
	}

	public void setLastAuthenticConnRequestTs(Date date) 
	{
		this.lastAuthenticConnRequestTs = date;
	} 
	
	public Date getLastAuthenticConnRequestTs() 
	{
		return this.lastAuthenticConnRequestTs;
	}

	public void setMissedCalls(int missedCalls) 
	{
		this.missedCalls = missedCalls;
	}

	public int getMissedCalls() 
	{
		return this.missedCalls;
	}

}
