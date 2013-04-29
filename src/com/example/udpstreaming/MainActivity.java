package com.example.udpstreaming;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.ByteBuffer;

import android.app.Activity;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.widget.TextView;

public class MainActivity extends Activity {

	private static final int PACKET_SIZE = 1024;
	private static final int TRACK_BUFFER_SIZE = PACKET_SIZE;
	private DatagramSocket socket;
	private InetAddress serverAddress;
	private ByteBuffer buffer;
	private int readPointer = 0;

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
					buffer = ByteBuffer.allocate(PACKET_SIZE*512);
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

				byte[] bytes = new byte[PACKET_SIZE];

				while (true) {
					DatagramPacket inPacket = new DatagramPacket(bytes, PACKET_SIZE);
					try {
						socket.receive(inPacket);
						int remaining = buffer.capacity() - buffer.position();
						if (remaining >= bytes.length) {
							buffer.put(bytes);
						} else {
							buffer.put(bytes, 0, remaining);
							buffer.position(0);
							buffer.put(bytes, remaining, bytes.length - remaining);
						}
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
				boolean test = false;
				at.play();
				try{
					while (true) {
						int remaining = buffer.capacity() - readPointer;
						if(buffer.position() >= readPointer) Log.d("UDPStreaming", "Size: " + (buffer.position() - readPointer));
						else Log.d("UDPStreaming", "Size: " + (remaining + buffer.position()));
						
						if(buffer.position() >= 1024*510) test = true;
						
						if(test){
							if (TRACK_BUFFER_SIZE > buffer.capacity())
								continue;
							if (buffer.position() <= readPointer && ((readPointer + TRACK_BUFFER_SIZE) % buffer.capacity() >= buffer.position() && (readPointer + TRACK_BUFFER_SIZE) % buffer.capacity() < readPointer))
								continue;
							if (buffer.position() > readPointer && ((readPointer + TRACK_BUFFER_SIZE) % buffer.capacity() >= buffer.position() || (readPointer + TRACK_BUFFER_SIZE) % buffer.capacity() <= readPointer))
								continue;

							if (remaining >= TRACK_BUFFER_SIZE) {
								at.write(buffer.array(), readPointer, TRACK_BUFFER_SIZE);
								readPointer += TRACK_BUFFER_SIZE;
							} else {
								at.write(buffer.array(), readPointer, remaining);
								at.write(buffer.array(), 0, TRACK_BUFFER_SIZE - remaining);
								readPointer = TRACK_BUFFER_SIZE - remaining;
							}
						}
						Thread.sleep(10);
					}
				}catch(Exception e){
					e.printStackTrace();
				}
			}
		});

		Thread senderThread = new Thread(new Runnable() {

			@Override
			public void run() {
				try{
					int count = PACKET_SIZE;
					byte[] sendData = new byte[count];

					File file = new File(Environment.getExternalStorageDirectory(), "tinytim.wav");
					FileInputStream in = new FileInputStream(file);

					int bytesread = 0, ret = 0;
					int size = (int) file.length();
					while (bytesread < size) {
						ret = in.read(sendData, 0, count);
						socket.send(new DatagramPacket(sendData, sendData.length, serverAddress, serverPort));
						bytesread += ret;
						//						Thread.sleep(5);
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

