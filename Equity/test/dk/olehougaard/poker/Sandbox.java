package dk.olehougaard.poker;

public class Sandbox {
	public static void main(String[] args) {
		short bitPattern = 0b0110001110100101;
		while (bitPattern != 0) {
			final short lsb = (short)(bitPattern & (-bitPattern));
			final int lsb_index = Evaluator.DE_BRUIJN_HASH[((lsb * Evaluator.DE_BRUIJN_SEQUENCE) & Evaluator.SHORT_MASK) >>> 12];
			System.out.println(lsb_index);
			bitPattern &= bitPattern - 1;
		}
	}
}
