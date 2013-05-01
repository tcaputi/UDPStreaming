package com.example.udpstreaming;

import java.net.DatagramSocket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;

import android.app.Activity;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

public class JaspActivity extends Activity {
	
	private static final long USER_ID = 2323000005434534534L;
	private static final int SESSION_ID = 1;
	private static final int SERVER_PORT = 4000;
	private static final int LOCAL_SOCKET_PORT = 3838;
	private static final String SERVER_ADDRESS = "50.116.60.24";
	
	private static final int AUDIO_PACKET_SIZE = 882;
	private static final int SAMPLE_RATE = 44100;
	private static final int BYTES_PER_SAMPLE = 2;
	private static final int BUFFER_SIZE = 882000; /*PACKET_SIZE * 512 * 2;*/
	private static final int CACHE_THRESHOLD = (int) (BUFFER_SIZE * 0.15f);
	private static final int BYTES_PER_LAPSE = SAMPLE_RATE * BYTES_PER_SAMPLE * 2;
	private static final int LAPSE_PERIOD_MS = 1000;
	
	private ByteBuffer buffer;
	private int readPointer = 0;

	private TextView tv;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		tv = new TextView(this);
		setContentView(tv);

		buffer = ByteBuffer.allocate(BUFFER_SIZE);
		startReceiver();
	}
	
	private void startReceiver() {
		try {
			JASPReceiver receiver = new JASPReceiver(SERVER_ADDRESS, SERVER_PORT, new DatagramSocket(LOCAL_SOCKET_PORT), SESSION_ID, USER_ID, AUDIO_PACKET_SIZE) {
				@Override
				public void onReceive(int index, byte[] data) {
					Log.d("UDPStreaming", String.valueOf(index));
					if (buffer.position() <= readPointer && ((buffer.position() + AUDIO_PACKET_SIZE) % buffer.capacity() > readPointer || (buffer.position() + AUDIO_PACKET_SIZE) % buffer.capacity() <= buffer.position()))
						Log.d("UDPStreaming", "BufferOverflow");
					else if (buffer.position() > readPointer && (buffer.position() + AUDIO_PACKET_SIZE) % buffer.capacity() > readPointer && (buffer.position() + AUDIO_PACKET_SIZE) % buffer.capacity() <= buffer.position())
						Log.d("UDPStreaming", "BufferOverflow");

					int remaining = buffer.capacity() - buffer.position();
					if (remaining >= data.length) {
						buffer.put(data);
					} else {
						buffer.put(data, 0, remaining);
						buffer.position(0);
						buffer.put(data, remaining, data.length - remaining);
					}
				}
			};
			receiver.start();
			startPlayer();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (SocketException e) {
			e.printStackTrace();
		}
	}
	
	private void startPlayer(){
		Thread playerThread = new Thread(new Runnable() {

			@Override
			public void run() {
				int minBufferSize = android.media.AudioTrack.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT);
				AudioTrack audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, SAMPLE_RATE, AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT, minBufferSize, AudioTrack.MODE_STREAM);
				audioTrack.play();
//				Log.d("UDPStreaming", "PlaybackRate: " + audioTrack.getPlaybackRate() + " / " + audioTrack.getSampleRate());
				int remaining;
				Log.d("UDPStreaming", "TimeStampInit: " + (System.currentTimeMillis()));
				long timeStamp = System.currentTimeMillis();
				while (true) {
					// Check if we need to fatten our buffer
					if (bufferSize() < CACHE_THRESHOLD) {
						doCache(0.5f); // Fatten the buffer
						timeStamp = System.currentTimeMillis() - LAPSE_PERIOD_MS;
						Log.d("UDPStreaming", "TimeStampChange: " + (System.currentTimeMillis() - LAPSE_PERIOD_MS));
					}

					// Every LAPSE_PERIOD_MS milliseconds, write BYTES_PER_LAPSE bytes to the audio track
					if (System.currentTimeMillis() - timeStamp >= LAPSE_PERIOD_MS) {
						// Update our recorded time stamp, do it this high to ignore processing time
						timeStamp += LAPSE_PERIOD_MS;
						Log.d("UDPStreaming", "Playing: " +  bufferSize() + " / " + BUFFER_SIZE);
						
						// Audio logic
						remaining = buffer.capacity() - readPointer;
						if (remaining >= BYTES_PER_LAPSE) { // There is enough space left in the buffer without staggering our read
							audioTrack.write(buffer.array(), readPointer, BYTES_PER_LAPSE);
							readPointer += BYTES_PER_LAPSE;
						} else {
							// We need to stagger the read since we have a circular buffer
							audioTrack.write(buffer.array(), readPointer, remaining);
							audioTrack.write(buffer.array(), 0, BYTES_PER_LAPSE - remaining);
							readPointer = BYTES_PER_LAPSE - remaining;
						}
					}
				}
			}
		});
		playerThread.start();
	}
	
	private void doCache(float target) { // Target is in %
		// If the buffer is to frail, grow it up sufficiently
		int size;
		target = target * BUFFER_SIZE;
		Log.d("UDPStreaming", "Buffer is too small, Caching Started");
		while ((size = bufferSize()) < target) {
			Log.d("UDPStreaming", "Caching @ " + ((100.0f * size) / BUFFER_SIZE) + "%");
		}
		Log.d("UDPStreaming", "Caching Ended");
	}

	private int bufferSize() {
		if (buffer.position() >= readPointer) return (buffer.position() - readPointer);
		else return ((buffer.capacity() - readPointer) + buffer.position());
	}
}
