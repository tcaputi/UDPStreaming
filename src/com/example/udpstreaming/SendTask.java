package com.example.udpstreaming;

import java.io.File;
import java.io.FileInputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;

public class SendTask extends AsyncTask<Void, Void, Void>{

	private DatagramSocket socket;
	private InetAddress serverAddress;
	
	public SendTask(DatagramSocket socket, InetAddress serverAddress){
		this.socket = socket;
		this.serverAddress = serverAddress;
	}
	
	@Override
	protected Void doInBackground(Void... params) {
		try{
			Log.d("UDPStreaming", String.valueOf(serverAddress.isReachable(5000)));
			socket = new DatagramSocket();

			int count = 1024;
			byte[] sendData = new byte[count];

			File file = new File(Environment.getExternalStorageDirectory(), "tinytim.wav");
			FileInputStream in = new FileInputStream(file);

			int bytesread = 0, ret = 0;
			int size = (int) file.length();
			while (bytesread < size) {
				ret = in.read(sendData, 0, count);
				if (ret != -1) {
					DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, serverAddress, 3546);
					socket.send(sendPacket);
					Log.d("UDPStreaming", "packet sent");
					bytesread += ret; 
				} else break; 
			} 
			in.close();
			socket.close();
		}catch(Exception e){
			e.printStackTrace();
		}
		return null;
	}
}
