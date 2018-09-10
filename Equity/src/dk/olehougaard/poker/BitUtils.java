package dk.olehougaard.poker;

import java.util.HashSet;

public class BitUtils {
	public static final int SHORT_MASK = 0xffff;
	public static final short DE_BRUIJN_SEQUENCE = 0b0000111101100101;
	public static final short[] DE_BRUIJN_HASH = {0, 1, 11, 2, 14, 12, 8, 3, 15, 10, 13, 7, 9, 6, 5, 4};

	public static short indexOfBit(short numberWithOneBitSet) {
		return DE_BRUIJN_HASH[((numberWithOneBitSet * DE_BRUIJN_SEQUENCE) & SHORT_MASK) >>> 12];
	}

	public static HashSet<Short> toSet(short bitVector) {
		HashSet<Short> set = new HashSet<>();
		while (bitVector != 0) {
			final short lsb = (short)(bitVector & (-bitVector));
			final short lsb_index = DE_BRUIJN_HASH[((lsb * DE_BRUIJN_SEQUENCE) & SHORT_MASK) >>> 12];
			set.add(lsb_index);
			bitVector &= bitVector - 1;
		}
		return set;
	}
}
