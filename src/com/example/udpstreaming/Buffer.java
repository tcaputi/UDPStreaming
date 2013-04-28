package com.example.udpstreaming;

import java.nio.ByteBuffer;

public class Buffer {

	private int inPointer = 0;
	private int outPointer = 0;
	private ByteBuffer buffer;

	public Buffer(int cap) {
		this.buffer = ByteBuffer.allocate(cap);
	}

	public boolean write(byte[] b) {
		if (b.length > buffer.capacity())
			return false;
		if (inPointer <= outPointer && ((inPointer + b.length) % buffer.capacity() > outPointer || (inPointer + b.length) % buffer.capacity() <= inPointer))
			return false;
		if (inPointer > outPointer && (inPointer + b.length) % buffer.capacity() > outPointer && (inPointer + b.length) % buffer.capacity() <= inPointer)
			return false;

		int remaining = buffer.capacity() - inPointer;
		if (remaining >= b.length) {
			buffer.put(b);
			inPointer += b.length;
			buffer.position(inPointer);
		} else {
			buffer.put(b, 0, remaining);
			buffer.position(0);
			buffer.put(b, remaining, b.length - remaining);
			inPointer = b.length - remaining;
			buffer.position(inPointer);
		}
		return true;
	}

	public boolean read(byte[] b) {
		if (b.length > buffer.capacity())
			return false;
		if (inPointer <= outPointer && ((outPointer + b.length) % buffer.capacity() >= inPointer && (outPointer + b.length) % buffer.capacity() < outPointer))
			return false;
		if (inPointer > outPointer && ((outPointer + b.length) % buffer.capacity() >= inPointer || (outPointer + b.length) % buffer.capacity() <= outPointer))
			return false;

		int remaining = buffer.capacity() - outPointer;
		if (remaining >= b.length) {
			System.arraycopy(buffer.array(), outPointer, b, 0, b.length);
			outPointer += b.length;
		} else {
			System.arraycopy(buffer, outPointer, b, 0, remaining);
			System.arraycopy(buffer.array(), 0, b, remaining, b.length - remaining);
			outPointer = b.length - remaining;
		}

		return true;
	}
}
