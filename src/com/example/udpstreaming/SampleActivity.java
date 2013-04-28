package com.example.udpstreaming;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

import android.app.Activity;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;

/** UdpStream activity sends and recv audio data through udp */
public class SampleActivity extends Activity {
	
	private DatagramSocket socket;
	private static InetAddress serverAddress;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		Thread initThread = new Thread(new Runnable() {

			@Override
			public void run() {
				try{
					serverAddress = InetAddress.getByName("sandile.me");
					socket = new DatagramSocket(3233);
					startThreads();
				}catch(Exception e){
					e.printStackTrace();
				}
			}
		});
		
		initThread.start();
	}

	private void startThreads() {
		sendAudio();
		recvAudio();
	}

	static final String LOG_TAG = "UdpStream";
	static final String AUDIO_FILE_PATH =  Environment.getExternalStorageDirectory().getPath() + "/tinytim.wav";
	static final int AUDIO_PORT = 2048;
	static final int SAMPLE_RATE = 8000;
	static final int SAMPLE_INTERVAL = 20; // milliseconds
	static final int SAMPLE_SIZE = 2; // bytes per sample
	static final int BUF_SIZE = SAMPLE_INTERVAL * SAMPLE_INTERVAL * SAMPLE_SIZE * 2;
	
	public void sendAudio() {
		Thread thrd = new Thread(new Runnable() {
			@Override
			public void run() {
				Log.e(LOG_TAG, "start send thread, thread id: " + Thread.currentThread().getId());
				long file_size = 0;
				int bytes_read = 0;
				int bytes_count = 0;
				File audio = new File(AUDIO_FILE_PATH);
				FileInputStream audio_stream = null;
				file_size = audio.length();
				byte[] buf = new byte[BUF_SIZE];
				try {
					audio_stream = new FileInputStream(audio);

					while (bytes_count < file_size) {
						bytes_read = audio_stream.read(buf, 0, BUF_SIZE);
						DatagramPacket pack = new DatagramPacket(buf, bytes_read, serverAddress, AUDIO_PORT);
						socket.send(pack);
						bytes_count += bytes_read;
						Log.d(LOG_TAG, "bytes_count : " + bytes_count);
						Thread.sleep(SAMPLE_INTERVAL, 0);
					}
				} catch (InterruptedException ie) {
					Log.e(LOG_TAG, "InterruptedException");
				} catch (FileNotFoundException fnfe) {
					Log.e(LOG_TAG, "FileNotFoundException");
				} catch (SocketException se) {
					Log.e(LOG_TAG, "SocketException");
				} catch (UnknownHostException uhe) {
					Log.e(LOG_TAG, "UnknownHostException");
				} catch (IOException ie) {
					Log.e(LOG_TAG, "IOException");
				}
			} // end run
		});
		thrd.start();
	}

	public void recvAudio() {
		Thread thrd = new Thread(new Runnable() {
			@Override
			public void run() {
				Log.e(LOG_TAG, "start recv thread, thread id: " + Thread.currentThread().getId());
				AudioTrack track = new AudioTrack(AudioManager.STREAM_MUSIC, SAMPLE_RATE, AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT, BUF_SIZE, AudioTrack.MODE_STREAM);
				track.play();
				try {
					byte[] buf = new byte[BUF_SIZE];

					while (true) {
						DatagramPacket pack = new DatagramPacket(buf, BUF_SIZE);
						socket.receive(pack);
						Log.d(LOG_TAG, "recv pack: " + pack.getLength());
						track.write(pack.getData(), 0, pack.getLength());
					}
				} catch (SocketException se) {
					Log.e(LOG_TAG, "SocketException: " + se.toString());
				} catch (IOException ie) {
					Log.e(LOG_TAG, "IOException" + ie.toString());
				}
			}
		});
		thrd.start();
	}
}