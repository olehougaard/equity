package dk.olehougaard.poker;

public class Test {
	public static void main(String[] args) {
		String[] hash = new String[16];
		for(int i = 0; i < 16; i++) {
			hash[((Evaluator.DE_BRUIJN_SEQUENCE << i) & Evaluator.SHORT_MASK) >> 12] = String.valueOf(i);
		}
		System.out.printf("{%s}", String.join(", ", hash));
	}

}
