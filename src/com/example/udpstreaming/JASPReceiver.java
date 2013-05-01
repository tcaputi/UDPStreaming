package com.example.udpstreaming;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;

import android.util.Log;

public abstract class JASPReceiver {
	private static final long HEART_BEAT_INTERVAL = 5000;
	private static final int HYPER_HEART_BEAT_INTERVAL = 200;
	private static final int HEART_BEAT_DATA_SIZE = 13;
	private static final int INDEX_SIZE_IN_BYTES = 4;

	private final DatagramSocket socket;
	private final DatagramPacket audioPacket;
	private final Thread audioThread;
	private final DatagramPacket heartbeatPacket;
	private final Thread heartbeatThread;
	private final byte[] data;

	private boolean alive = true; //threads are active
	private boolean hyper = true; //send packets faster to establish server connection

	public JASPReceiver(String serverUrl, int serverPort, DatagramSocket socket, int sessionId, long userId, int packetSize) throws UnknownHostException, SocketException {
		assert (serverUrl != null) : "The JASP server url was null.";
		assert (socket != null) : "The datagram socket was null.";
		// Initialization
		this.data = new byte[packetSize];
		this.socket = socket;
		this.audioPacket = new DatagramPacket(this.data, this.data.length);
		// Create the heart beat buffer
		ByteBuffer buffer = ByteBuffer.allocate(HEART_BEAT_DATA_SIZE);
		buffer.put((byte) 1);
		buffer.putInt(sessionId);
		buffer.putLong(userId);
		// Resume initialization
		this.heartbeatPacket = new DatagramPacket(buffer.array(), HEART_BEAT_DATA_SIZE, InetAddress.getByName(serverUrl), serverPort);
		// Create the threads
		this.heartbeatThread = new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					while (JASPReceiver.this.alive) {
						// Send the heart beat if we ain't paused
						try {
							JASPReceiver.this.socket.send(JASPReceiver.this.heartbeatPacket);
							Log.d("UDPStreaming", "heartbeat");
							Thread.sleep(JASPReceiver.this.hyper ? HYPER_HEART_BEAT_INTERVAL : HEART_BEAT_INTERVAL);
						} catch (InterruptedException e) {
							e.printStackTrace(); // Don't care really
						}
					}
				} catch (IOException e) {
					JASPReceiver.this.alive = false;
					throw new HeartBeatException(e);
				}
			}
		});
		this.audioThread = new Thread(new Runnable() {
			@Override
			public void run() {
				if (JASPReceiver.this.hyper) {
					JASPReceiver.this.hyper = false;
				}
				// Time to receive stuff
				int index;
				byte[] audioData = new byte[data.length - 4];
				try {
					while (JASPReceiver.this.alive) {
						JASPReceiver.this.socket.receive(JASPReceiver.this.audioPacket);
						// We received successfully, so we can stop hyper heart beating
						if (JASPReceiver.this.hyper) JASPReceiver.this.hyper = false;
						
						index = data[0] << 24 | (data[1] & 0xFF) << 16 | (data[2] & 0xFF) << 8 | (data[3] & 0xFF);
						System.arraycopy(data, INDEX_SIZE_IN_BYTES, audioData, 0, audioData.length);
						
						// Inform the listener method
						onReceive(index, audioData);
					}
				} catch (IOException e) {
					JASPReceiver.this.alive = false;
					throw new ReceiverException(e);
				}
			}
		});
	}

	/**
	 * Starts receiving audio from the JASP server indicated in the constructor.
	 */
	public void start() {
		if (!this.alive) {
			throw new ReceiverAlreadyKilledException();
		} else {
			this.audioThread.start();
			this.heartbeatThread.start();
		}
	}

	/**
	 * Stops receiving audio from the JASP server indicated in the constructor.
	 * Once a <code>JASPReceiver</code> is killed, it cannot be restarted.
	 */
	public void kill() {
		if (this.alive) {
			this.alive = false;
		} else {
			throw new ReceiverAlreadyKilledException();
		}
	}

	/**
	 * Called whenever audio is received from the JASP server. The contents of
	 * this method <i>must be as optimal as possible</i> since this method is
	 * blocking.
	 * 
	 * @param data
	 *            new audio data from the JASP server
	 */
	public abstract void onReceive(int index, byte[] data);

	public class ReceiverException extends RuntimeException {
		private static final long serialVersionUID = -5455213495051801401L;

		public ReceiverException(IOException cause) {
			super("Could not receive audio packet from JASP server", cause);
		}
	}
	
	public class HeartBeatException extends RuntimeException {
		private static final long serialVersionUID = -5455213495051801402L;

		public HeartBeatException(IOException cause) {
			super("Could not send heart beat to JASP server", cause);
		}
	}
	
	public class ReceiverAlreadyKilledException extends RuntimeException {
		private static final long serialVersionUID = -5455213495051801403L;

		public ReceiverAlreadyKilledException() {
			super("This JASPReceiver has been killed. Therefore, it cannot be started or killed again.");
		}
	}
}
