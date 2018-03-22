package com.perforce.p4java.impl.mapbased.rpc.func.client;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;

import com.perforce.p4java.CharsetDefs;
import com.perforce.p4java.exception.NullPointerError;
import com.perforce.p4java.exception.P4JavaError;

/**
 * Java equivalent of mangle.cc
 * 
 * @author Kevin Sawicki (ksawicki@perforce.com)
 * 
 */
final class Mangle {

	private static final int BPB = 8;

	private int[] o = new int[8];
	private int[] pr = new int[8];
	private int[] s0 = new int[16];
	private int[] s1 = new int[16];
	private int[] s2 = new int[8];

	/**
	 * Create a new mangle
	 */
	protected Mangle() {
		// diffusion pattern

		o[0] = 7;
		o[1] = 6;
		o[2] = 2;
		o[3] = 1;
		o[4] = 5;
		o[5] = 0;
		o[6] = 3;
		o[7] = 4;

		// inverse of fixed permutation

		pr[0] = 2;
		pr[1] = 5;
		pr[2] = 4;
		pr[3] = 0;
		pr[4] = 3;
		pr[5] = 1;
		pr[6] = 7;
		pr[7] = 6;

		// S-box permutations

		s0[0] = 12;
		s0[1] = 15;
		s0[2] = 7;
		s0[3] = 10;
		s0[4] = 14;
		s0[5] = 13;
		s0[6] = 11;
		s0[7] = 0;
		s0[8] = 2;
		s0[9] = 6;
		s0[10] = 3;
		s0[11] = 1;
		s0[12] = 9;
		s0[13] = 4;
		s0[14] = 5;
		s0[15] = 8;

		s1[0] = 7;
		s1[1] = 2;
		s1[2] = 14;
		s1[3] = 9;
		s1[4] = 3;
		s1[5] = 11;
		s1[6] = 0;
		s1[7] = 4;
		s1[8] = 12;
		s1[9] = 13;
		s1[10] = 1;
		s1[11] = 10;
		s1[12] = 6;
		s1[13] = 15;
		s1[14] = 8;
		s1[15] = 5;

		// S-box mix
		s2[0] = 10;
		s2[1] = 1;
		s2[2] = 13;
		s2[3] = 12;
		s2[4] = 4;
		s2[5] = 0;
		s2[6] = 11;
		s2[7] = 3;
	}

	/**
	 * Copy of strops::OtoX
	 * 
	 * @param o
	 * @return - char
	 */
	private static char OtoX(char o) {
		return (char) (o >= 10 ? o - 10 + 'A' : o + '0');
	};

	private String OtoX(String octet) {
		StringBuilder hex = new StringBuilder();
		for (int i = 0; i < octet.length(); i++) {
			hex.append(OtoX((char) ((octet.charAt(i) >> 4) & 0x0f)));
			hex.append(OtoX((char) ((octet.charAt(i) >> 0) & 0x0f)));
		}
		return hex.toString();
	}
	
	/**
	 * Take a bunch of bytes and translate it into a hex string. No real
	 * error checking done.
	 */
	protected String OtoX(byte[] octets) {
		if (octets == null) {
			throw new NullPointerError("null octets byte array in Mangle.OtoX");
		}
		
		StringBuilder strBldr = new StringBuilder();
		
		for (int i = 0; i < octets.length; i++) {
			strBldr.append(OtoX((char) ((octets[i] >> 4) & 0x0F)));
			strBldr.append(OtoX((char) ((octets[i] >> 0) & 0x0F)));
		}
		
		return strBldr.toString();
	}
	
	/**
	 * Turn a hex string into an array of bytes (octets). Little to no
	 * useful error checking.
	 * 
	 * @param hex the hex-encoded string to be converted
	 */
	protected static byte[] XtoO(String hex) {
		if ((hex == null) || ((hex.length() / 2) == 0)) {
			throw new P4JavaError("bad XtoO conversion parameters");
		}
		
		byte[] octets = new byte[hex.length() / 2];
		
		int i = 0;
		int j = 0;
		while ((i < hex.length()) && (j < octets.length)) {
			octets[j] = (byte) ((byte) ((XtoO(hex.charAt(i)) &0X0F) << 4) | ((byte) (XtoO(hex.charAt(i + 1)) &0X0F)));
			i += 2;
			j++;
		}
		
		return octets;
	}

	protected static byte XtoO(char c) {
		return (byte) (c - ( c > '9' ? ( c >= 'a' ? 'a' - 10 : 'A' - 10 ) : '0'));
	}
	
	/**
	 * Do an XOR on the passed-in 32 char hex strings (e.g. typical MD5 hash outputs);
	 * return the result as a 32 char hex string. Little to no real error
	 * checking performed.
	 */
	protected String xor32(String data, String key) {
		
		if ((data == null) || (key == null) || (data.length() != 32) || (key.length() != 32)) {
			throw new P4JavaError("bad parameters to Mangle.xor32");
		}
		
		byte[] dataBytes = XtoO(data);
		byte[] keyBytes = XtoO(key);
		byte[] resultBytes = new byte[16];
		
		for (int i = 0; i < resultBytes.length; i++) {
			resultBytes[i] = (byte) (dataBytes[i] ^ keyBytes[i]); 
		}
		
		return OtoX(resultBytes);
	}

	/**
	 * Loop over the data string, mangle it a chunk at a time, and thus can now
	 * handle passwords longer than 16 characters.
	 * 
	 * @param data
	 * @param key
	 * @param digest
	 * @return enrypted string
	 * @throws UnsupportedEncodingException 
	 */
	protected String encrypt(String data, String key) throws UnsupportedEncodingException {
		StringBuffer mangledValue = new StringBuffer();
		// Proper handling of unicode characters
		byte[] dataBytes = data.getBytes(CharsetDefs.UTF8.name());
		int inputLen = dataBytes.length;
		int offset = 0;
		
		char[] dataChars = new char[inputLen];
		for (int i=0; i<dataBytes.length; i++) {
			dataChars[i] = (char)dataBytes[i];
		}

		while (offset < inputLen) {
			int chunkSize = inputLen - offset;
			if (chunkSize > 16) {
				chunkSize = 16;
			}
			String chunkData = new String(dataChars, offset, chunkSize);
			String mangledChunk = mangle(chunkData, key, false);
			mangledValue.append(mangledChunk);
			offset += chunkSize;
		}

		return mangledValue.toString();
	}
	
	/**
	 * Mangle data and key and return mangled string
	 * 
	 * @param data
	 * @param key
	 * @return - mangled string
	 */
	protected String mangle(String data, String key, boolean digest) {
		int[] m = new int[128];
		int[] k = new int[128];
		StringBuilder p, q;
		char[] src = new char[16];
		char[] enc = new char[16];
		char[] buf = new char[16];
		int counter;
		int output;
		int c, i, j;

		String mangled = null;
		
		if ((key == null) || (data == null)) {
			throw new NullPointerError("null argument in mangle()");
		}

		if (!digest && data.length() > 16) {
			return mangled;
		}

		Arrays.fill(src, (char) 0);
		Arrays.fill(enc, (char) 0);
		Arrays.fill(buf, (char) 0);
		
		// truncate key to 16 character max
		System.arraycopy(key.toCharArray(), 0, buf, 0, key.length() > 16 ? 16
				: key.length());
		
		if (digest) {
			byte[] srcBytes = XtoO(data);
			for (int z = 0; z < srcBytes.length; z++) { src[z] = (char) srcBytes[z]; };
		} else {
			System.arraycopy(data.toCharArray(), 0, src, 0, data.length());
		}

		p = new StringBuilder();
		p.append(src);
		q = new StringBuilder();
		q.append(enc);

		for (counter = 0; counter < 16; counter += 1) {
			c = buf[counter] & 0xFF;
			for (i = 0; i < BPB; i += 1) {
				k[(BPB * counter) + i] = c & 0x1;
				c = c >> 1;
			}
		}

		counter = 0;

		int qIndex = 0;
		for (j = 0; j < 16; ++j) {
			c = p.charAt(j);
			if (counter == 16) {
				Getdval(m, k);

				for (counter = 0; counter < 16; counter += 1) {
					output = 0;
					for (i = BPB - 1; i >= 0; i -= 1) {
						output = (output << 1) + m[(BPB * counter) + i];
					}
					q.setCharAt(qIndex, (char) output);
					qIndex++;
				}
				counter = 0;
			}
			for (i = 0; i < BPB; i += 1) {
				m[(BPB * counter) + i] = c & 0x1;
				c = c >> 1;
			}
			counter += 1;
		}

		for (; counter < 16; counter += 1) {
			for (i = 0; i < BPB; i += 1) {
				m[(BPB * counter) + i] = 0;
			}
		}

		Getdval(m, k);

		for (counter = 0; counter < 16; counter += 1) {
			output = 0;
			for (i = BPB - 1; i >= 0; i -= 1) {
				output = (output << 1) + m[(BPB * counter) + i];
			}
			q.setCharAt(qIndex, (char) output);
			qIndex++;
		}

		mangled = OtoX(q.toString());
		return mangled;
	}

	private void Getdval(int m[], int k[]) {
		int tcbindex, tcbcontrol; /* transfer control byte indices */
		int round, hi, lo, h_0, h_1;
		int bit, temp1;
		int _byte, index, v;
		int[] tr = new int[BPB];

		h_0 = 0;
		h_1 = 1;

		tcbcontrol = 0;

		for (round = 0; round < 16; round += 1) {
			tcbindex = tcbcontrol;
			for (_byte = 0; _byte < 8; _byte += 1) {
				lo = (m[(h_1 * 64) + (BPB * _byte) + 7]) * 8
						+ (m[(h_1 * 64) + (BPB * _byte) + 6]) * 4
						+ (m[(h_1 * 64) + (BPB * _byte) + 5]) * 2
						+ (m[(h_1 * 64) + (BPB * _byte) + 4]);
				hi = (m[(h_1 * 64) + (BPB * _byte) + 3]) * 8
						+ (m[(h_1 * 64) + (BPB * _byte) + 2]) * 4
						+ (m[(h_1 * 64) + (BPB * _byte) + 1]) * 2
						+ (m[(h_1 * 64) + (BPB * _byte) + 0]);

				v = (s0[lo] + 16 * s1[hi]) * (1 - k[(BPB * tcbindex) + _byte])
						+ (s0[hi] + 16 * s1[lo]) * k[(BPB * tcbindex) + _byte];

				for (temp1 = 0; temp1 < BPB; temp1 += 1) {
					tr[temp1] = v & 0x1;
					v = v >> 1;
				}

				for (bit = 0; bit < BPB; bit += 1) {
					index = (o[bit] + _byte) & 0x7;
					temp1 = m[(h_0 * 64) + (BPB * index) + bit]
							+ k[(BPB * tcbcontrol) + pr[bit]] + tr[pr[bit]];
					m[(h_0 * 64) + (BPB * index) + bit] = temp1 & 0x1;
				}

				if (_byte < 7)
					tcbcontrol = (tcbcontrol + 1) & 0xF;
			}

			temp1 = h_0;
			h_0 = h_1;
			h_1 = temp1;
		}

		/* final swap */
		for (_byte = 0; _byte < 8; _byte += 1) {
			for (bit = 0; bit < BPB; bit += 1) {
				temp1 = m[(BPB * _byte) + bit];
				m[(BPB * _byte) + bit] = m[64 + (BPB * _byte) + bit];
				m[64 + (BPB * _byte) + bit] = temp1;
			}
		}
	}
}
