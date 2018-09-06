package dk.olehougaard.poker;

public class Sandbox {
	public static void main(String[] args) {
		short paired = 0b0110001110100101;
		while (paired != 0) {
			final short lsb = (short)(paired & (-paired));
			final int lsb_index = Evaluator.DE_BRUIJN_HASH[((lsb * Evaluator.DE_BRUIJN_SEQUENCE) & Evaluator.SHORT_MASK) >>> 12];
			System.out.println(lsb_index);
			paired &= paired - 1;
		}
	}
}
