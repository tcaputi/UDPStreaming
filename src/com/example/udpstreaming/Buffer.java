package com.example.udpstreaming;

import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;

public class Buffer {
	
	private int inPointer = 0;
	private int outPointer = 0;
	private byte[] buffer;
	
	public Buffer(int cap){
		this.buffer = new byte[cap];
	}
	
	public void write(byte[] b){
		if(b.length > buffer.length) throw new BufferOverflowException();
		if(inPointer <= outPointer && ((inPointer + b.length)%buffer.length > outPointer || (inPointer + b.length)%buffer.length <= inPointer)) throw new BufferOverflowException();
		if(inPointer > outPointer && (inPointer + b.length)%buffer.length > outPointer && (inPointer + b.length)%buffer.length <= inPointer) throw new BufferOverflowException();
		
		int remaining = buffer.length - inPointer;
		if(remaining >= b.length){
			System.arraycopy(b, 0, buffer, inPointer, b.length);
			inPointer += b.length;
		}else{
			System.arraycopy(b, 0, buffer, inPointer, remaining);
			System.arraycopy(b, remaining, buffer, 0, b.length - remaining);
			inPointer = b.length - remaining;
		}
	}
	
	public void read(byte[] b){
		if(b.length > buffer.length) throw new BufferUnderflowException();
		if(inPointer <= outPointer && ((outPointer + b.length)%buffer.length >= inPointer && (outPointer + b.length)%buffer.length < outPointer)) throw new BufferUnderflowException();
		if(inPointer > outPointer && ((outPointer + b.length)%buffer.length >= inPointer || (outPointer + b.length)%buffer.length <= outPointer)) throw new BufferUnderflowException();
		
		int remaining = buffer.length - outPointer;
		if(remaining >= b.length){
			System.arraycopy(buffer, outPointer, b, 0, b.length);
			outPointer += b.length;
		}else{
			System.arraycopy(buffer, outPointer, b, 0, remaining);
			System.arraycopy(buffer, 0, b, remaining, b.length - remaining);
			outPointer = b.length - remaining;
		}
	}
}
