package dk.olehougaard.poker;

import java.util.Random;

public class PerformanceTest {
	public static void main(String[] args) {
		Random rand = new Random();
		long[] hands = new long[100_000_000];
		for(int i = 0; i < hands.length; i++) {
			hands[i] = rand.nextLong() & Hand.HAND_MASK;
		}
		for(long hand: hands) Evaluator.evaluate(hand);
		long start = System.currentTimeMillis();
		for(long hand: hands) Evaluator.evaluate(hand);
		System.out.println((System.currentTimeMillis() - start) / 100.0);
	}
}
