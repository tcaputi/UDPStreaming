package com.example.udpstreaming;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

import android.os.AsyncTask;
import android.util.Log;

public class TestTask extends AsyncTask<Void, Void, Void>{

	@Override
	protected Void doInBackground(Void... params) {
		try {
			String message = "hi";

			InetAddress serverAddress = InetAddress.getByName("69.164.221.75");

			DatagramSocket socket = new DatagramSocket();

			byte[] sendData = message.getBytes();
			byte[] receiveData = new byte[512];

			DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, serverAddress, 3546);
			socket.send(sendPacket);

			Log.d("UDPStreaming", "Listening for response: " + String.valueOf(serverAddress.isReachable(5000)));

			DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
			socket.receive(receivePacket);
			String modifiedSentence = new String(receivePacket.getData());
			Log.d("UDPStreaming", "FROM SERVER:" + modifiedSentence.trim());
			socket.close();
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
