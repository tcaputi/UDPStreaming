package com.example.udpstreaming;
import java.nio.ByteBuffer;

public class AudioPacketFactory {

	public static void fillMetaData(int sessionId, long userId, int index, byte[] preloadedDest) {
		ByteBuffer buffer = ByteBuffer.wrap(preloadedDest);
		buffer.put((byte) 0);
		buffer.putInt(sessionId);
		buffer.putLong(userId);
		buffer.putInt(index);
	}
}
