package com.example.udpstreaming;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

import android.app.Activity;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.widget.TextView;

public class MainActivity extends Activity {

	private DatagramSocket socket;
	private InetAddress serverAddress;
	private Buffer buffer;

	private TextView tv;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		tv = new TextView(this);
		setContentView(tv);

		Thread initThread = new Thread(new Runnable() {

			@Override
			public void run() {
				try{
					serverAddress = InetAddress.getByName("sandile.me");
					socket = new DatagramSocket(8991);
					buffer = new Buffer(1024*512);
					startThreads();
				}catch(Exception e){
					e.printStackTrace();
				}
			}
		});

		initThread.start();
	}

	private void startThreads(){
		final int serverPort = 4000;

		Thread receiverThread = new Thread(new Runnable() {

			@Override
			public void run() {
				while (true) {
					DatagramPacket inPacket = new DatagramPacket(new byte[1024], 1024);
					try {
						socket.receive(inPacket);
						Log.d("UDPStreaming", "Receiver: " + buffer.write(inPacket.getData()));
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		});

		Thread playerThread = new Thread(new Runnable() {

			@Override
			public void run() {

				int intSize = android.media.AudioTrack.getMinBufferSize(44100, AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT); 
				AudioTrack at = new AudioTrack(AudioManager.STREAM_MUSIC, 44100, AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT, intSize, AudioTrack.MODE_STREAM); 
				byte[] bytes = new byte[1024];

				at.play();
				while (true) {
					try{
						if(buffer.read(bytes)){
							at.write(bytes, 0, 1024);
						}else{
							Log.d("UDPStreaming", "BufferEmpty");
						}
//						Thread.sleep(4);
					}catch(Exception e){
						e.printStackTrace();
					}
				}
			}
		});

		Thread senderThread = new Thread(new Runnable() {

			@Override
			public void run() {
				try{
					int count = 1024;
					byte[] sendData = new byte[count];

					File file = new File(Environment.getExternalStorageDirectory(), "tinytim.wav");
					FileInputStream in = new FileInputStream(file);

					int bytesread = 0, ret = 0;
					int size = (int) file.length();
					while (bytesread < size) {
						ret = in.read(sendData, 0, count);
						socket.send(new DatagramPacket(sendData, sendData.length, serverAddress, serverPort));
						bytesread += ret;
						Thread.sleep(5);
					} 
					in.close();
					socket.close();
				}catch(Exception e){
					e.printStackTrace();
				}
			}
		});

		receiverThread.start();
		senderThread.start();
		try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		playerThread.start();
	}
}

