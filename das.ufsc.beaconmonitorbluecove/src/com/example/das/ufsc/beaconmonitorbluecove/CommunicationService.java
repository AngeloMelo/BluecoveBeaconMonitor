package com.example.das.ufsc.beaconmonitorbluecove;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.bluetooth.RemoteDevice;
import javax.microedition.io.Connector;
import javax.microedition.io.StreamConnection;

import android.os.Handler;


public class CommunicationService
{
	public static final int MSG_TYPE_MESSAGE_READ = 0;
	public static final int MSG_TYPE_REFRESH_SLAVELIST = 1;
	public static final int MSG_TYPE_STOP_DISCOVERY = 2;
	public static final int MSG_TYPE_CONNECTED_TO_BEACON = 3;
	public static final int MSG_TYPE_EXCEPTION = 4;
	public static final int MSG_TYPE_CONNECT_EXCEPTION = 5;
	public static final int MSG_TYPE_CONNECTION_CLOSED = 6;
	
	private ReadWriteThread mReadWriteThread;
	private ConnectThread mConnectThread;
	private final Handler mHandler;
	
	
	public CommunicationService(Handler handler) 
	{
        mHandler = handler;
    }
	
	/**/
	public void shutDown() throws IOException
	{
		if (mConnectThread != null) {mConnectThread.cancel(); mConnectThread = null;}
		if (mReadWriteThread != null) {mReadWriteThread.cancel(); mReadWriteThread = null;}
	}

	
	/**
     * Start the ConnectedThread to begin managing a Bluetooth connection
     * @param socket  The BluetoothSocket on which the connection was made
     * @param device  The BluetoothDevice that has been connected
	 * @throws IOException 
     */
	private synchronized void startTransmission(StreamConnection streamConnection) throws IOException 
    {
        // Cancel any thread currently running a connection
        if (mReadWriteThread != null) {mReadWriteThread.cancel(); mReadWriteThread = null;}
        
        // Start the thread to manage the connection and perform transmissions
        mReadWriteThread = new ReadWriteThread(streamConnection);
        mReadWriteThread.start();
        
        RemoteDevice dev = RemoteDevice.getRemoteDevice(streamConnection);
        String remoteAdress = dev.getBluetoothAddress();
		mHandler.obtainMessage(MSG_TYPE_CONNECTED_TO_BEACON, remoteAdress ).sendToTarget();
    }    
    
	
    public void connect(String connectionURL) 
    {   	
        // Cancel any thread currently running a connection
        if (mReadWriteThread != null) {mReadWriteThread.cancel(); mReadWriteThread = null;}


        
        // Start the thread to connect with the given device
        try 
        {
			mConnectThread = new ConnectThread(connectionURL);
			mConnectThread.start();
		} 
        catch (Exception e) 
        {
        	//TODO
        	mConnectThread = null;
        	mHandler.obtainMessage(MSG_TYPE_CONNECT_EXCEPTION, e).sendToTarget();
		} 
    }
    

	public void sendMessage(String msg) throws IOException
    {
		if(mReadWriteThread != null)
    	{
			mReadWriteThread.write(msg.getBytes());
    	}
    }
    
	
	private class ConnectThread extends Thread 
	{
		private StreamConnection streamConnection;
	    private boolean running = true;
		private String connectionUrl;
	    
		public ConnectThread(String connectionURL) 
	    {
			this.connectionUrl = connectionURL;
	    }
	 
	    public void run() 
	    {
	    	if(!running) return;
	 
	        try 
	        {
	            // Connect the device 
	        	streamConnection = (StreamConnection)Connector.open(connectionUrl);
	        } 
	        catch (IOException connectException) 
	        {
	            // Unable to connect; close the socket and get out
	            try 
	            {
	                mHandler.obtainMessage(MSG_TYPE_CONNECT_EXCEPTION, connectException).sendToTarget();
	                streamConnection.close();
	            } 
	            catch (IOException closeException) 
	            { 
	            	mHandler.obtainMessage(MSG_TYPE_EXCEPTION, closeException).sendToTarget();
	            }
	            return;
	        }
	 
	        try
	        {
				// Do work to manage the connection (in a separate thread)
	        	startTransmission(streamConnection);
	        }
	        catch(IOException e)
	        {
	        	mHandler.obtainMessage(MSG_TYPE_EXCEPTION, e).sendToTarget();
	        	
	        }
	    }
	    
	    public synchronized void cancel()
	    {
	        this.running = false;
	    }
	}
	
	
	
	private class ReadWriteThread extends Thread 
	{
		private final StreamConnection streamConnection;
	    private final InputStream mmInStream;
	    private final OutputStream mmOutStream;
	    private volatile boolean running = true;
	    private String remoteAddress;

	 
	    public ReadWriteThread(StreamConnection streamConnection) 
	    {
	    	this.streamConnection = streamConnection;
	    	try {
				this.remoteAddress = RemoteDevice.getRemoteDevice(streamConnection).getBluetoothAddress();
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}

	    	InputStream tmpIn = null;
	        OutputStream tmpOut = null;
	 
	        // Get the input and output streams, using temp objects because member streams are final
	        try 
	        {
	            tmpIn = streamConnection.openInputStream();
	            tmpOut = streamConnection.openOutputStream();
	        } 
	        catch (IOException e) 
	        { 
	        	mHandler.obtainMessage(MSG_TYPE_EXCEPTION, e).sendToTarget();
	        }
	 
	        mmInStream = tmpIn;
	        mmOutStream = tmpOut;
	    }
	 
	    public void run() 
	    {
	    	// buffer store for the stream
	        byte[] buffer = new byte[2048];  

	        // bytes returned from read()
	        int bytes; 
	 
	        // Keep listening to the InputStream until an exception occurs
	        while (this.running) 
	        {
	            try 
	            {
	                // Read from the InputStream
	                bytes = mmInStream.read(buffer);
	                
	                if(bytes > 0)
	                {
	                	// Send the obtained bytes to the UI activity
	                	this.running = false;
	                	mHandler.obtainMessage(MSG_TYPE_MESSAGE_READ, bytes, -1, buffer).sendToTarget();	
	                }
	            } 
	            catch (IOException e) 
	            {
	            	mHandler.obtainMessage(MSG_TYPE_CONNECTION_CLOSED, null).sendToTarget();
	            	
	            	try 
	            	{
	            		shutDown();
					} 
	            	catch (IOException e1) 
					{
						e1.printStackTrace();
					}
	                break;
	            }
	        }
	    }
	 
	    /* Call this from the main activity to send data to the remote device */
	    public void write(byte[] bytes) throws IOException 
	    {
            mmOutStream.write(bytes);
	    }
	 
	    /* Call this from the main activity to shutdown the connection */
	    public void cancel() 
	    {
	        try 
	        { 
	        	mmInStream.close(); 
	        } 
	        catch (IOException e) 
	        { 
	        	e.printStackTrace();
	        }
	        
	        try 
	        { 
	        	mmOutStream.close(); 
	        } 
	        catch (IOException e) 
	        { 
	        	e.printStackTrace();
	        }
	        
	        try 
	        {
	            streamConnection.close();
	        } 
	        catch (IOException e) 
	        { 
	        	e.printStackTrace();
	        }
	        finally
	        {
	        	this.running = false;
	        }
	    }
	}

}