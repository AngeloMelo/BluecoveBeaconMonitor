package com.example.das.ufsc.beaconmonitorbluecove;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.bluetooth.BluetoothStateException;
import javax.bluetooth.DeviceClass;
import javax.bluetooth.DiscoveryAgent;
import javax.bluetooth.DiscoveryListener;
import javax.bluetooth.LocalDevice;
import javax.bluetooth.RemoteDevice;
import javax.bluetooth.ServiceRecord;
import javax.bluetooth.UUID;

import org.json.JSONException;
import org.json.JSONObject;

import android.bluetooth.BluetoothAdapter;
import android.os.Handler;
import android.os.Message;

import com.example.das.ufsc.beaconmonitorbluecove.utils.BeaconDefaults;


public class Manager 
{
	private CommunicationService comunicationService;
	private Main ui;
	
	private String beaconConnURL;
	private RemoteDevice beaconDevice;
	
	private Date startDiscoveryTS;
	private Map<String, ConnectionPerformanceInfo> currentConnectedBeacons;
	
	private int operationMode;
	private CallWaiter callWaiterThread;
	private int attempts = 0;
	private int missedCalls = 0;
	private DiscoveryListener discoveryListener;
	
	private final Handler mHandler = new Handler() 
	{
		@Override
		public synchronized void handleMessage(Message msg) 
		{
			switch(msg.what)
			{
			case CommunicationService.MSG_TYPE_MESSAGE_READ:
			{
				byte[] readBuf = (byte[]) msg.obj;
				
				// construct a string from the valid bytes in the buffer
				String readMessage = new String(readBuf, 0, msg.arg1);
				
				readTic(readMessage);
				break;
			}
			case CommunicationService.MSG_TYPE_CONNECTED_TO_BEACON:
			{
				String beaconMac =(String) msg.obj;
				setConnectionDate(beaconMac);
				
				break;
			}
			case CommunicationService.MSG_TYPE_EXCEPTION:
			{
				Exception errorMessage =(Exception) msg.obj;
				ui.showError(errorMessage);
				break;
			}
			case CommunicationService.MSG_TYPE_CONNECT_EXCEPTION:
			{
				onConnectionException(msg);

				break;
			}
			
			case CommunicationService.MSG_TYPE_CONNECTION_CLOSED:
			{
				//ui.showError(new Exception("Connection reset by peer"));
				
				break;
			}


			}
		}

	};
	
	private class MyDiscoveryListener implements DiscoveryListener
	{
		@Override
		public void deviceDiscovered(RemoteDevice remoteDevice, DeviceClass arg1) 
		{
			onDeviceDiscovered(remoteDevice);
		}

		@Override
		public void inquiryCompleted(int arg0) 
		{
			// TODO Auto-generated method stub
		}

		@Override
		public void serviceSearchCompleted(int arg0, int arg1) 
		{
			// TODO Auto-generated method stub
		}
		
		@Override
		public void servicesDiscovered(int transID, ServiceRecord[] servRecord) 
		{
		    if(servRecord!=null && servRecord.length>0)
		    {
		        onServiceDiscovered(servRecord);
		    }
		}
	}
	
	public Manager(Main uiRef)
	{
		super();
		
		this.ui = uiRef;
		
		comunicationService = new CommunicationService(mHandler);
		init();
	}
	
	
	
	private void init()
	{
		this.beaconDevice = null;
		this.beaconConnURL = null;
		this.startDiscoveryTS = null;
		this.attempts = 0;
		this.missedCalls = 0;
		this.currentConnectedBeacons = new HashMap<String, ConnectionPerformanceInfo>();
		this.operationMode = BeaconDefaults.OPP_MODE_AUTHENTIC;

		this.discoveryListener = new MyDiscoveryListener();
	}


	
	private void readTic(String msgRead) 
	{
		if(msgRead != null)
		{						
			try 
			{
				JSONObject json = new JSONObject(msgRead);
				if(json.has(BeaconDefaults.TIC_KEY))
				{
					ConnectionPerformanceInfo connPerformanceInfo = this.currentConnectedBeacons.get(beaconDevice.getBluetoothAddress());
					connPerformanceInfo.setLastTicReceivedTs(new Date());
					
					int tic = json.getInt(BeaconDefaults.TIC_KEY);
					
					if(tic == BeaconDefaults.INT_CLOSE_CONNECTION)
					{
						comunicationService.shutDown();
					}
					else 
					{
						int lineId = json.getInt(BeaconDefaults.TIC_LINEID_KEY);
						String lineName = json.getString(BeaconDefaults.TIC_LINENM_KEY);
						String lastStop = json.getString(BeaconDefaults.TIC_LASTSTOPNM_KEY);
						
						this.ui.showBeaconInfo("Following Beacon for line " + lineName +"(" + lineId + ")");
						this.ui.showStopInfo(lastStop);
						this.ui.showWarning("");
					
						if(tic == BeaconDefaults.INT_NO_RECALL)
						{
							ui.showNextCallInfo("Final stop");
						}
						else
						{
							ui.showNextCallInfo("Next call in " + tic + "s");
							prepareNewCall(tic);
						}
					}
					
					//send command to close this connection on the peer
					sendAckMessage();
				}
			} 
			catch (JSONException e) 
			{
				ui.showError(e);
			} 
			catch (IOException e) 
			{
				ui.showError(e);
			}
		}
	}
	
	
	private void sendAckMessage() 
	{
		try 
		{
			ConnectionPerformanceInfo connPerformanceInfo = this.currentConnectedBeacons.get(beaconDevice.getBluetoothAddress());
			connPerformanceInfo.setLastAckSentTs(new Date());
			connPerformanceInfo.setMissedCalls(missedCalls);
			
			String jsonString = BeaconDefaults.formatJson(LocalDevice.getLocalDevice().getBluetoothAddress(), getOppMode(), connPerformanceInfo);
			
			comunicationService.sendMessage(jsonString);
		} 
		catch (Exception e) 
		{
			ui.showError(e);
		}

	}


	private int getOppMode() 
	{
		return this.operationMode;
	}


	private void prepareNewCall(int tic)
	{
		if(this.callWaiterThread != null)
		{
			this.callWaiterThread.cancel();
		}
		
		this.callWaiterThread = new CallWaiter(tic * 1000, false);
		this.callWaiterThread.start();
		this.attempts = 0;
	}
	
	
	private void callBeacon(boolean recovery)
	{
		if(this.beaconConnURL == null) 
		{
			String error = "Error: beaconConnURL is null ";
			ui.showError(error);
			return;
		}
		
		ConnectionPerformanceInfo connPerformanceInfo = this.currentConnectedBeacons.get(beaconDevice.getBluetoothAddress());
		connPerformanceInfo.setLastConnRequestTs(new Date());
		
		if(!recovery)
		{
			connPerformanceInfo.setLastAuthenticConnRequestTs(new Date());
		}
		
        // Cancel discovery because it will slow down the connection
        cancelDiscovery();
		this.comunicationService.connect(this.beaconConnURL);
	}
	
	
	
	private void startDiscovery() 
	{
		//record start discovery
		this.startDiscoveryTS = new Date();
		
		//start discovery
		try 
		{
			//TODO try to change discovery agent type
			LocalDevice.getLocalDevice().getDiscoveryAgent().startInquiry(DiscoveryAgent.GIAC, this.discoveryListener);
		} 
		catch (BluetoothStateException e) 
		{
			ui.showError(e);
		}
	}



	public void turnOnBluetooth() 
	{
		//turnon bluetooth
		if (BluetoothAdapter.getDefaultAdapter().isEnabled()) 
		{
			BluetoothAdapter.getDefaultAdapter().disable(); 
		}
		BluetoothAdapter.getDefaultAdapter().enable();
		
		//erase local variables and wait for bluetooth stack on
		init();
	}

	

	public void stopBeacon() 
	{
		try
		{
			//cancel call waiter thread
			if(this.callWaiterThread != null)
			{
				this.callWaiterThread.cancel();
			}
			
			//stop communication service
			comunicationService.shutDown();
			
			//turn off bluetooth
			BluetoothAdapter.getDefaultAdapter().disable();
			
			init();
		}
		catch(Exception e)
		{
			ui.showError(e);
		}
	}


	public void cancelDiscovery()
	{
		try 
		{
			LocalDevice.getLocalDevice().getDiscoveryAgent().cancelInquiry(discoveryListener);
		} 
		catch (BluetoothStateException e) 
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	

	public void onBluetoothOn() 
	{
		try 
		{
			//get  friendly name from bluecove to show that bluetooth is on
			String name = BluetoothAdapter.getDefaultAdapter().getName();
			String statusText = "Device name: " + name;		
			ui.showBluetoothProperties(statusText);	
			
			//bluetooth is on, can start discovery
			startDiscovery();
		} 
		catch (Exception e) 
		{
			ui.showError(e);
		}
	}

	
	public void onDeviceDiscovered(RemoteDevice remoteDevice) 
	{
		this.ui.showBeaconInfo("Device found");
		//if already found a beacon, give up 
		if(beaconDevice != null) return;
		
		UUID[] uuids  = new UUID[1];
        uuids[0]      = new UUID(BeaconDefaults.STR_UUID, false);
		try 
		{
			LocalDevice.getLocalDevice().getDiscoveryAgent().searchServices(null, uuids, remoteDevice, discoveryListener);
		} 
		catch (BluetoothStateException e)
		{
			ui.showError(e);
		}
	}
	
	
	public void onServiceDiscovered(ServiceRecord[] servRecord) 
	{
		this.ui.showBeaconInfo("Beacon found, waiting for connection...");
		this.beaconDevice = servRecord[0].getHostDevice();
		
		String connectionURL = servRecord[0].getConnectionURL(0,false);
		this.beaconConnURL = connectionURL;
		
        comunicationService.connect(connectionURL);
        
		
		//records the first connection initial date
		if(!this.currentConnectedBeacons.containsKey(this.beaconDevice.getBluetoothAddress()))
		{
			ConnectionPerformanceInfo connPerformanceInfo = new ConnectionPerformanceInfo();
			connPerformanceInfo.setStartDiscoveryTS(this.getStartDiscoveryTS());
			connPerformanceInfo.setBeaconFoundTS(new Date());
			connPerformanceInfo.setLastConnRequestTs(new Date());
			connPerformanceInfo.setLastAuthenticConnRequestTs(new Date());
			
			this.currentConnectedBeacons.put(this.beaconDevice.getBluetoothAddress(), connPerformanceInfo);
		}
		else
		{
			ConnectionPerformanceInfo connPerformanceInfo = this.currentConnectedBeacons.get(this.beaconDevice.getBluetoothAddress());
			connPerformanceInfo.setStartDiscoveryTS(this.getStartDiscoveryTS());
			connPerformanceInfo.setBeaconFoundTS(new Date());
			connPerformanceInfo.setLastConnRequestTs(new Date());
			connPerformanceInfo.setLastAuthenticConnRequestTs(new Date());
		}
	}


	private void setConnectionDate(String beaconMac)
	{
		ConnectionPerformanceInfo connPerformanceInfo = this.currentConnectedBeacons.get(beaconMac);
		if (connPerformanceInfo == null) return;
		
		//keep the connection time
		if(connPerformanceInfo.isFirstConnection())
		{
			connPerformanceInfo.setFirstConnAcceptanceTS(new Date());
		}
		else
		{
			connPerformanceInfo.setLastConnAcceptanceTs(new Date());
		}
	}
	
	public Date getStartDiscoveryTS() 
	{
		return startDiscoveryTS;
	}
	
	
	public void setDubiousMode(boolean dubious) 
	{
		if(dubious)
		{
			this.operationMode = BeaconDefaults.OPP_MODE_DUBIOUS;
		}
		else
		{
			this.operationMode = BeaconDefaults.OPP_MODE_AUTHENTIC;
		}
	}
	
	
	private void onConnectionException(Message msg) 
	{
		Exception errorMessage = (Exception) msg.obj;
		missedCalls++;
		attempts++;
		
		String wrnMessage = "Conn refused. " + errorMessage + "\nAttempt: " + (attempts);
		ui.showWarning(wrnMessage);
		
		ui.showMissedCalls(missedCalls);
		
		if(callWaiterThread != null)
		{
			callWaiterThread.cancel();
		}
		
		//prepare a new call in 5 seconds
		callWaiterThread = new CallWaiter(5000, true);
		callWaiterThread.start();
	}

	
	private class CallWaiter extends Thread
	{
		private long timeToWait;
		private boolean running = true;
		private boolean recovery = false;
		
		public CallWaiter(long time, boolean recovery)
		{
			super();
			this.timeToWait = time;
			this.recovery = recovery;
		}
		
		public synchronized void cancel()
		{
			this.running = false;
		}
		
		public void run()
		{
			while(timeToWait > 0 && this.running)
			{
				//ui.de
				ui.showNextCallInfo("Next call in " + timeToWait/1000 + "s");
				
				try 
				{
					sleep(1000);
				} 
				catch (InterruptedException e) 
				{
					e.printStackTrace();
				}
				timeToWait = timeToWait - 1000;
			}
			
		
			ui.showNextCallInfo("");
			if(this.running)
			{
				callBeacon(this.recovery);
			}
		}
	}

}
