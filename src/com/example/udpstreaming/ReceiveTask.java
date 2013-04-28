package com.example.udpstreaming;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.net.UnknownHostException;

import android.os.AsyncTask;
import android.util.Log;

public class ReceiveTask extends AsyncTask<Void, Void, Void>{

	private DatagramSocket socket;
	
	public ReceiveTask(DatagramSocket socket){
		this.socket = socket;
	}
	
	@Override
	protected Void doInBackground(Void... params) {
		try {
			byte[] receiveData = new byte[1024];
			while(true){
				DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
				socket.receive(receivePacket);
//				String modifiedSentence = new String(receivePacket.getData());
				Log.d("UDPStreaming", "packet receieved");
			}
		} catch (SocketException e) {
			e.printStackTrace();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}
}
