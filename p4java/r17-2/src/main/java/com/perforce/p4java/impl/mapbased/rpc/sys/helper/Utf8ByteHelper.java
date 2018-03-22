package com.perforce.p4java.impl.mapbased.rpc.sys.helper;

import com.perforce.p4java.exception.FileDecoderException;

import java.nio.ByteBuffer;

public enum Utf8ByteHelper {

	SINGLE, START, MULTI, UNKNOWN;

	public static Utf8ByteHelper parse(byte value) {

		int b = (int) value & 0xff;

		if ((b & 0x80) == 0) {
			return SINGLE;
		}

		if (((b & 0x80) != 0) && ((b | 0xBF) == 0xBF)) {
			return MULTI;
		}

		if (length(value) > 0) {
			return START;
		}

		return UNKNOWN;
	}

	public static int length(byte value) {
		int b = (int) value & 0xff;

		if (((b & 0xC0) == 0xC0) && ((b | 0xDF) == 0xDF)) {
			return 1;
		}

		if (((b & 0xE0) == 0xE0) && ((b | 0xEF) == 0xEF)) {
			return 2;
		}

		if (((b & 0xF0) == 0xF0) && ((b | 0xF7) == 0xF7)) {
			return 3;
		}

		return 0;
	}

	public static int findBufferLimit(ByteBuffer buffer) throws FileDecoderException {
		// find start of multi byte
		int r = buffer.remaining();
		for (int i = 1; i <= 4; i++) {
			int pos = r - i;
			byte b = buffer.get(pos);
			Utf8ByteHelper t = Utf8ByteHelper.parse(b);
			switch (t) {
				case START:
					return pos;
				case SINGLE:
					throw new FileDecoderException("Corrupt UTF8; single byte in multi byte sequence.");
				case MULTI:
					continue;
				default:
					throw new FileDecoderException("Corrupt UTF8; Unknown byte type. " + String.format("0x%02X", b));
			}
		}
		throw new FileDecoderException("Corrupt UTF8; no start byte found.");
	}
}
