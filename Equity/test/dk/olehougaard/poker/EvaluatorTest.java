package dk.olehougaard.poker;

import static dk.olehougaard.poker.Evaluator.*;
import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class EvaluatorTest {
	private static void print(long x) {
		final String s = Long.toBinaryString(x);
		final String padded = "0000000000000000000000000000000000000000000000000000000000000000" + s;
		System.out.println(padded.substring(padded.length() - 64));
	}

	@SuppressWarnings("unused")
	private static void printEval(long hand) {
		print(Evaluator.evaluate(hand));
	}

	// STRAIGHT FLUSH
	@Test
	void aStraightFlushIsAStraightFlush() {
		long hand = Hand.createHand("Ac", "Js", "Ts", "2h", "9s", "8s", "7s");
		assertNotEquals(0L, Evaluator.evaluate(hand) & SF_MASK);
	}

	@Test
	void biggerStraightFlushIsBigger() {
		long winner = Hand.createHand("Ac", "Js", "Ts", "2h", "9s", "8s", "7s");
		long sucker = Hand.createHand("Ac", "6s", "Ts", "2h", "9s", "8s", "7s");
		assertTrue(Evaluator.evaluate(winner) > Evaluator.evaluate(sucker));
	}

	@Test
	void aStraightFlushDrawIsNotAStraightFlush() {
		long hand = Hand.createHand("Ac", "4d", "Ts", "2h", "9s", "8s", "7s");
		assertEquals(0L, Evaluator.evaluate(hand) & SF_MASK);
	}

	@Test
	void aStraightIsNotAStraightFlush() {
		long hand = Hand.createHand("Ac", "Jd", "Ts", "2h", "9s", "8s", "7s");
		assertEquals(0L, Evaluator.evaluate(hand) & SF_MASK);
	}

	@Test
	void aWheelFlushIsAStraightFlush() {
		long hand = Hand.createHand("Ac", "Js", "4c", "2c", "9s", "3c", "5c");
		assertNotEquals(0L, Evaluator.evaluate(hand) & SF_MASK);
	}

	// QUADS
	@Test
	void fourOfAKindIsAQuad() {
		long hand = Hand.createHand("Ac", "Js", "Jd", "Jh", "Jc", "3c", "5c");
		assertNotEquals(0L, Evaluator.evaluate(hand) & QUAD_MASK);
	}

	@Test
	void quadIsTheMostSignificantPair() {
		long hand = Hand.createHand("Ac", "Js", "Jd", "Jh", "Jc", "3c", "5c");
		assertEquals(11L, (Evaluator.evaluate(hand) & MOST_SIGNIFICANT_PAIR_MASK) >> MSP_INDEX);
	}

	@Test
	void quadHasAKicker() {
		long hand = Hand.createHand("Ac", "Js", "Jd", "Jh", "Jc", "3c", "5c");
		assertEquals(1L << Hand.ACE_INDEX, Evaluator.evaluate(hand) & UNPAIRED_MASK);
	}

	@Test
	void theQuadIsNotAKicker() {
		long hand = Hand.createHand("Ac", "Js", "Jd", "Jh", "Jc", "3c", "5c");
		assertEquals(0L, Evaluator.evaluate(hand) & (1L << Hand.JACK_INDEX));
	}

	@Test
	void theLowCardsAreNotKickers() {
		long hand = Hand.createHand("Ac", "Js", "Jd", "Jh", "Jc", "3c", "5c");
		assertEquals(0L, Evaluator.evaluate(hand) & (1L << Hand.TREY_INDEX));
		assertEquals(0L, Evaluator.evaluate(hand) & (1L << Hand.FIVE_INDEX));
	}

	@Test
	void quadIsSmallerThanStraightFlush() {
		long quad = Hand.createHand("Ac", "Js", "Jd", "Jh", "Jc", "3c", "5c");
		long sf = Hand.createHand("Ac", "Js", "4c", "2c", "9s", "3c", "5c");
		assertTrue(Evaluator.evaluate(quad) < Evaluator.evaluate(sf));
	}

	@Test
	void HighQuadIsBiggerThanLowQuad() {
		long high = Hand.createHand("Js", "Jd", "Jh", "Jc", "Ac", "3c", "3d");
		long low = Hand.createHand("3s", "3h", "Jh", "Jc", "Ac", "3c", "3d");
		assertTrue(Evaluator.evaluate(high) > Evaluator.evaluate(low));
	}

	@Test
	void fourOfAKindAndThreeOfAKindIsAQuad() {
		long hand = Hand.createHand("Ac", "Js", "Jd", "Jh", "Jc", "Ad", "Ah");
		assertNotEquals(0L, Evaluator.evaluate(hand) & QUAD_MASK);
	}

	// BOAT
	@Test
	void threeOfAKindAndTwoOfAKindIsABoat() {
		long boat = Hand.createHand("Qs", "Jd", "Jh", "Jc", "Ac", "3c", "Ad");
		assertNotEquals(0L, Evaluator.evaluate(boat) & BOAT_MASK);
	}

	@Test
	void boatHasThreeOfAKindAsTheMSP() {
		long boat = Hand.createHand("Qs", "Jd", "Jh", "Jc", "Ac", "3c", "Ad");
		assertEquals(11L, (Evaluator.evaluate(boat) & MOST_SIGNIFICANT_PAIR_MASK) >> MSP_INDEX);
	}

	@Test
	void boatHasThreeOfAKindAsTheLSP() {
		long boat = Hand.createHand("Qs", "Jd", "Jh", "Jc", "Ac", "3c", "Ad");
		assertEquals(14L, (Evaluator.evaluate(boat) & LEAST_SIGNIFICANT_PAIR_MASK) >> LSP_INDEX);
	}

	@Test
	void boatHasNoKicker() {
		long boat = Hand.createHand("Qs", "Jd", "Jh", "Jc", "Ac", "3c", "Ad");
		assertEquals(0, Evaluator.evaluate(boat) & UNPAIRED_MASK);
	}

	@Test
	void aBoatIsNotAQuad() {
		long boat = Hand.createHand("Qs", "Jd", "Jh", "Jc", "Ac", "3c", "Ad");
		assertEquals(0L, Evaluator.evaluate(boat) & (QUAD_MASK | SF_MASK));
	}

	@Test
	void aBoatIsSmallerThanAQuad() {
		long boat = Hand.createHand("Qs", "Jd", "Jh", "Jc", "Ac", "3c", "Ad");
		long quad = Hand.createHand("2c", "Ts", "Td", "Th", "Tc", "3d", "4h");
		assertTrue(Evaluator.evaluate(boat) < Evaluator.evaluate(quad));
	}

	@Test
	void boatsAreComparedByTheTrips() {
		long jacksFull = Hand.createHand("Qs", "Jd", "Jh", "Jc", "Ac", "3c", "Ad");
		long queensFull = Hand.createHand("Qs", "Qd", "Qh", "Jc", "2c", "3c", "2d");
		assertTrue(Evaluator.evaluate(jacksFull) < Evaluator.evaluate(queensFull));
	}

	@Test
	void boatsAreComparedByThePairsWhenTheTripsAreEqual() {
		long fullOfAces = Hand.createHand("Qs", "Jd", "Jh", "Jc", "Ac", "3c", "Ad");
		long fullOfDeuces = Hand.createHand("Qs", "Jd", "Jh", "Jc", "2c", "3c", "2d");
		assertTrue(Evaluator.evaluate(fullOfAces) > Evaluator.evaluate(fullOfDeuces));
	}

	@Test
	void twoTripsIsABoat() {
		long boat = Hand.createHand("As", "Td", "Jh", "Jc", "Ac", "3c", "Ad");
		long twoTrips = Hand.createHand("Ah", "Jd", "Jh", "Jc", "Ac", "3c", "Ad");
		assertTrue(Evaluator.evaluate(boat) == Evaluator.evaluate(twoTrips));
	}

	@Test
	void TwoPairsIsNotABoat() {
		long boat = Hand.createHand("Qs", "Jd", "Jh", "Tc", "Ac", "3c", "Ad");
		assertEquals(0L, Evaluator.evaluate(boat) & BOAT_MASK);
	}

	// FLUSH
	@Test
	void fourOfAsuitIsNotAFlush() {
		long draw = Hand.createHand("Qs", "Jd", "Tc", "Jc", "Ac", "3c", "Ad");
		assertEquals(0L, Evaluator.evaluate(draw) & FLUSH_MASK);
	}
	
	@Test
	void fiveOfASuitIsAFlush() {
		long flush = Hand.createHand("Qc", "Jd", "Tc", "Jc", "Ac", "3c", "Ad");
		assertNotEquals(0L, Evaluator.evaluate(flush) & FLUSH_MASK);
	}
	
	@Test
	void sixOfASuitIsAFlush() {
		long flush = Hand.createHand("Qc", "8c", "Tc", "Jc", "Ac", "3c", "Ad");
		assertNotEquals(0L, Evaluator.evaluate(flush) & FLUSH_MASK);
	}
	
	@Test
	void aStraightFlushIsNotAFlush() {
		long flush = Hand.createHand("Qc", "Jd", "Tc", "Jc", "Ac", "Kc", "Ad");
		assertEquals(0L, Evaluator.evaluate(flush) & FLUSH_MASK);
	}
	
	@Test
	void aFlushIsLessThanABoat() {
		long boat = Hand.createHand("As", "Td", "Jh", "Jc", "Ac", "3c", "Ad");
		long flush = Hand.createHand("Qc", "Jd", "Tc", "Jc", "Ac", "3c", "Ad");
		assertTrue(Evaluator.evaluate(flush) <Evaluator.evaluate(boat));
	}
	
	@Test
	void flushOrderIsDeterminedByHighCard() {
		long high = Hand.createHand("Qc", "Jd", "Tc", "Jc", "Ac", "3c", "Ad");
		long low = Hand.createHand("Qc", "Jd", "Tc", "Jc", "4c", "3c", "Ad");
		assertTrue(Evaluator.evaluate(low) <Evaluator.evaluate(high));
	}
	
	@Test
	void flushOrderIsDeterminedByHighestDifferentCard() {
		long high = Hand.createHand("Qc", "Jd", "Tc", "Jc", "Ac", "3c", "Ad");
		long low = Hand.createHand("Qc", "Jd", "Tc", "8c", "Ac", "3c", "Ad");
		assertTrue(Evaluator.evaluate(low) <Evaluator.evaluate(high));
	}
	
	@Test
	void fiveHighestCardsCountInSixCardFlush() {
		long fiveFlush = Hand.createHand("Qc", "Jd", "Tc", "Jc", "Ac", "3c", "Ad");
		long sixFlush = Hand.createHand("Qc", "Jd", "Tc", "Jc", "Ac", "3c", "2c");
		assertTrue(Evaluator.evaluate(fiveFlush) == Evaluator.evaluate(sixFlush));
	}
	
	//STRAIGHT
	@Test
	void aStraightDrawIsNotAStraigh() {
		long draw = Hand.createHand("Qs", "Jd", "Tc", "Jc", "Kc", "3h", "4d");
		assertEquals(0L, Evaluator.evaluate(draw) & STRAIGHT_MASK);
	}
	
	
}
