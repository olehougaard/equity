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
		print(evaluate(hand));
	}

	// STRAIGHT FLUSH
	@Test
	void aStraightFlushIsAStraightFlush() {
		long hand = Hand.createHand("Ac", "Js", "Ts", "2h", "9s", "8s", "7s");
		assertNotEquals(0L, evaluate(hand) & SF_MASK);
	}

	@Test
	void biggerStraightFlushIsBigger() {
		long winner = Hand.createHand("Ac", "Js", "Ts", "2h", "9s", "8s", "7s");
		long sucker = Hand.createHand("Ac", "6s", "Ts", "2h", "9s", "8s", "7s");
		assertTrue(evaluate(winner) > evaluate(sucker));
	}

	@Test
	void aStraightFlushDrawIsNotAStraightFlush() {
		long hand = Hand.createHand("Ac", "4d", "Ts", "2h", "9s", "8s", "7s");
		assertEquals(0L, evaluate(hand) & SF_MASK);
	}

	@Test
	void aStraightIsNotAStraightFlush() {
		long hand = Hand.createHand("Ac", "Jd", "Ts", "2h", "9s", "8s", "7s");
		assertEquals(0L, evaluate(hand) & SF_MASK);
	}

	@Test
	void aWheelFlushIsAStraightFlush() {
		long hand = Hand.createHand("Ac", "Js", "4c", "2c", "9s", "3c", "5c");
		assertNotEquals(0L, evaluate(hand) & SF_MASK);
	}

	// QUADS
	@Test
	void fourOfAKindIsAQuad() {
		long hand = Hand.createHand("Ac", "Js", "Jd", "Jh", "Jc", "3c", "5c");
		assertNotEquals(0L, evaluate(hand) & QUAD_MASK);
	}

	@Test
	void quadIsTheMostSignificantPair() {
		long hand = Hand.createHand("Ac", "Js", "Jd", "Jh", "Jc", "3c", "5c");
		assertEquals(1L << Hand.JACK_INDEX, (evaluate(hand) & MSP_MASK) >> MSP_INDEX);
	}

	@Test
	void quadHasAKicker() {
		long hand = Hand.createHand("Ac", "Js", "Jd", "Jh", "Jc", "3c", "5c");
		assertEquals(1L << Hand.ACE_INDEX, evaluate(hand) & UNPAIRED_MASK);
	}

	@Test
	void theQuadIsNotAKicker() {
		long hand = Hand.createHand("Ac", "Js", "Jd", "Jh", "Jc", "3c", "5c");
		assertEquals(0L, evaluate(hand) & (1L << Hand.JACK_INDEX));
	}

	@Test
	void theLowCardsAreNotKickers() {
		long hand = Hand.createHand("Ac", "Js", "Jd", "Jh", "Jc", "3c", "5c");
		assertEquals(0L, evaluate(hand) & (1L << Hand.TREY_INDEX));
		assertEquals(0L, evaluate(hand) & (1L << Hand.FIVE_INDEX));
	}

	@Test
	void quadIsSmallerThanStraightFlush() {
		long quad = Hand.createHand("Ac", "Js", "Jd", "Jh", "Jc", "3c", "5c");
		long sf = Hand.createHand("Ac", "Js", "4c", "2c", "9s", "3c", "5c");
		assertTrue(evaluate(quad) < evaluate(sf));
	}

	@Test
	void HighQuadIsBiggerThanLowQuad() {
		long high = Hand.createHand("Js", "Jd", "Jh", "Jc", "Ac", "3c", "3d");
		long low = Hand.createHand("3s", "3h", "Jh", "Jc", "Ac", "3c", "3d");
		assertTrue(evaluate(high) > evaluate(low));
	}

	@Test
	void fourOfAKindAndThreeOfAKindIsAQuad() {
		long hand = Hand.createHand("Ac", "Js", "Jd", "Jh", "Jc", "Ad", "Ah");
		assertNotEquals(0L, evaluate(hand) & QUAD_MASK);
	}

	// BOAT
	@Test
	void threeOfAKindAndTwoOfAKindIsABoat() {
		long boat = Hand.createHand("Qs", "Jd", "Jh", "Jc", "Ac", "3c", "Ad");
		assertNotEquals(0L, evaluate(boat) & BOAT_MASK);
	}

	@Test
	void boatHasThreeOfAKindAsTheMSP() {
		long boat = Hand.createHand("Qs", "Jd", "Jh", "Jc", "Ac", "3c", "Ad");
		print(1l << Hand.JACK_INDEX);
		printEval(boat >> MSP_INDEX);
		assertEquals(1L << Hand.JACK_INDEX, (evaluate(boat) & MSP_MASK) >> MSP_INDEX);
	}

	@Test
	void boatHasTwoOfAKindAsTheLSP() {
		long boat = Hand.createHand("Qs", "Jd", "Jh", "Jc", "Ac", "3c", "Ad");
		assertEquals(1L << Hand.ACE_INDEX, (evaluate(boat) & LSP_MASK) >> LSP_INDEX);
	}

	@Test
	void boatHasNoKicker() {
		long boat = Hand.createHand("Qs", "Jd", "Jh", "Jc", "Ac", "3c", "Ad");
		assertEquals(0, evaluate(boat) & UNPAIRED_MASK);
	}

	@Test
	void aBoatIsNotAQuad() {
		long boat = Hand.createHand("Qs", "Jd", "Jh", "Jc", "Ac", "3c", "Ad");
		assertEquals(0L, evaluate(boat) & (QUAD_MASK | SF_MASK));
	}

	@Test
	void aBoatIsSmallerThanAQuad() {
		long boat = Hand.createHand("Qs", "Jd", "Jh", "Jc", "Ac", "3c", "Ad");
		long quad = Hand.createHand("2c", "Ts", "Td", "Th", "Tc", "3d", "4h");
		assertTrue(evaluate(boat) < evaluate(quad));
	}

	@Test
	void boatsAreComparedByTheTrips() {
		long jacksFull = Hand.createHand("Qs", "Jd", "Jh", "Jc", "Ac", "3c", "Ad");
		long queensFull = Hand.createHand("Qs", "Qd", "Qh", "Jc", "2c", "3c", "2d");
		assertTrue(evaluate(jacksFull) < evaluate(queensFull));
	}

	@Test
	void boatsAreComparedByThePairsWhenTheTripsAreEqual() {
		long fullOfAces = Hand.createHand("Qs", "Jd", "Jh", "Jc", "Ac", "3c", "Ad");
		long fullOfDeuces = Hand.createHand("Qs", "Jd", "Jh", "Jc", "2c", "3c", "2d");
		assertTrue(evaluate(fullOfAces) > evaluate(fullOfDeuces));
	}

	@Test
	void twoTripsIsABoat() {
		long boat = Hand.createHand("As", "Td", "Jh", "Jc", "Ac", "3c", "Ad");
		long twoTrips = Hand.createHand("Ah", "Jd", "Jh", "Jc", "Ac", "3c", "Ad");
		assertTrue(evaluate(boat) == evaluate(twoTrips));
	}

	@Test
	void TwoPairsIsNotABoat() {
		long boat = Hand.createHand("Qs", "Jd", "Jh", "Tc", "Ac", "3c", "Ad");
		assertEquals(0L, evaluate(boat) & BOAT_MASK);
	}

	@Test
	void highestPairCountInBoat() {
		long fullOfEightsNotDeuces = Hand.createHand("8s", "Jd", "Jh", "Jc", "2c", "8c", "2d");
		long fullOfEights = Hand.createHand("8s", "Jd", "Jh", "Jc", "4c", "8c", "2d");
		assertTrue(evaluate(fullOfEights) == evaluate(fullOfEightsNotDeuces));
	}

	@Test
	void overpairCountsInBoat() {
		long fullOfAcesNotDeuces = Hand.createHand("As", "Jd", "Jh", "Jc", "2c", "Ac", "2d");
		long fullOfAces = Hand.createHand("As", "Jd", "Jh", "Jc", "4c", "Ac", "2d");
		assertTrue(evaluate(fullOfAces) == evaluate(fullOfAcesNotDeuces));
	}

	// FLUSH
	@Test
	void fourOfAsuitIsNotAFlush() {
		long draw = Hand.createHand("Qs", "Jd", "Tc", "Jc", "Ac", "3c", "Ad");
		assertEquals(0L, evaluate(draw) & FLUSH_MASK);
	}
	
	@Test
	void fiveOfASuitIsAFlush() {
		long flush = Hand.createHand("Qc", "Jd", "Tc", "Jc", "Ac", "3c", "Ad");
		assertNotEquals(0L, evaluate(flush) & FLUSH_MASK);
	}
	
	@Test
	void sixOfASuitIsAFlush() {
		long flush = Hand.createHand("Qc", "8c", "Tc", "Jc", "Ac", "3c", "Ad");
		assertNotEquals(0L, evaluate(flush) & FLUSH_MASK);
	}
	
	@Test
	void aStraightFlushIsNotAFlush() {
		long flush = Hand.createHand("Qc", "Jd", "Tc", "Jc", "Ac", "Kc", "Ad");
		assertEquals(0L, evaluate(flush) & FLUSH_MASK);
	}
	
	@Test
	void aFlushIsLessThanABoat() {
		long boat = Hand.createHand("As", "Td", "Jh", "Jc", "Ac", "3c", "Ad");
		long flush = Hand.createHand("Qc", "Jd", "Tc", "Jc", "Ac", "3c", "Ad");
		assertTrue(evaluate(flush) <evaluate(boat));
	}
	
	@Test
	void flushOrderIsDeterminedByHighCard() {
		long high = Hand.createHand("Qc", "Jd", "Tc", "Jc", "Ac", "3c", "Ad");
		long low = Hand.createHand("Qc", "Jd", "Tc", "Jc", "4c", "3c", "Ad");
		assertTrue(evaluate(low) <evaluate(high));
	}
	
	@Test
	void flushOrderIsDeterminedByHighestDifferentCard() {
		long high = Hand.createHand("Qc", "Jd", "Tc", "Jc", "Ac", "3c", "Ad");
		long low = Hand.createHand("Qc", "Jd", "Tc", "8c", "Ac", "3c", "Ad");
		assertTrue(evaluate(low) <evaluate(high));
	}
	
	@Test
	void fiveHighestCardsCountInSixCardFlush() {
		long fiveFlush = Hand.createHand("Qc", "Jd", "Tc", "Jc", "Ac", "3c", "Ad");
		long sixFlush = Hand.createHand("Qc", "Jd", "Tc", "Jc", "Ac", "3c", "2c");
		assertTrue(evaluate(fiveFlush) == evaluate(sixFlush));
	}
	
	//STRAIGHT
	@Test
	void aStraightDrawIsNotAStraight() {
		long draw = Hand.createHand("Qs", "Jd", "Tc", "Jc", "Kc", "3h", "4d");
		assertEquals(0L, evaluate(draw) & STRAIGHT_MASK);
	}
	
	@Test
	void fiveInARowIsAStraight() {
		long straight = Hand.createHand("Qs", "Jd", "Tc", "Jc", "Kc", "9h", "4d");
		assertNotEquals(0L, evaluate(straight) & STRAIGHT_MASK);
	}
	
	@Test
	void aStraightIsNotABiggerHand() {
		long straight = Hand.createHand("Qs", "Jd", "Tc", "Jc", "Kc", "9h", "4d");
		assertEquals(0L, evaluate(straight) & (FLUSH_MASK | BOAT_MASK | QUAD_MASK | SF_MASK));
	}
	
	@Test
	void aBroadwayIsAStraight() {
		long broadway = Hand.createHand("Qs", "Jd", "Tc", "Jc", "Kc", "Ah", "4d");
		assertNotEquals(0L, evaluate(broadway) & STRAIGHT_MASK);
	}
	
	@Test
	void aWheelIsAStraight() {
		long wheel = Hand.createHand("3s", "Jd", "5c", "Jc", "2c", "Ah", "4d");
		assertNotEquals(0L, evaluate(wheel) & STRAIGHT_MASK);
	}
	
	@Test
	void aStraightFlushIsNotAStraight() {
		long hand = Hand.createHand("Ac", "Js", "Ts", "2h", "9s", "8s", "7s");
		assertEquals(0L, evaluate(hand) & STRAIGHT_MASK);
	}
	
	@Test
	void aStraightIsDecidedByHighestCard() {
		long straight = Hand.createHand("Qs", "Jd", "Tc", "Jc", "Kc", "9h", "4d");
		long broadway = Hand.createHand("Qs", "Jd", "Tc", "Jc", "Kc", "Ah", "4d");
		assertTrue(straight < broadway);
	}
	
	@Test
	void aWheelIsTheLowestStraight() {
		long straight = Hand.createHand("Qs", "Jd", "Tc", "Jc", "Kc", "9h", "4d");
		long wheel = Hand.createHand("3s", "Jd", "4c", "Jc", "2c", "Ah", "4d");
		assertTrue(straight > wheel);
	}
	
	// TRIPS
	@Test
	void onePairIsNotTrips() {
		long hand = Hand.createHand("Ac", "Js", "Ts", "2h", "2d", "8s", "7s");
		assertEquals(0L, evaluate(hand) & TRIP_MASK);
	}
	
	@Test
	void threePairsIsNotTrips() {
		long hand = Hand.createHand("Jc", "Js", "Ts", "2h", "2d", "Td", "7s");
		assertEquals(0L, evaluate(hand) & TRIP_MASK);
	}
	
	@Test
	void threeOfAKindIsTrips() {
		long hand = Hand.createHand("Ac", "Js", "Ts", "2h", "2d", "2s", "7s");
		assertNotEquals(0L, evaluate(hand) & TRIP_MASK);
	}

	@Test
	void tripsAreEvaluatedOnTheRankOfThe3() {
		long hand = Hand.createHand("Ac", "Js", "Ts", "2h", "2d", "2s", "7s");
		long bigger = Hand.createHand("7c", "5s", "4s", "3h", "3d", "3s", "2s");
		assertTrue(evaluate(bigger)> evaluate(hand));
	}
	
	@Test
	void equalTripsAreEvaluatedOnKickers() {
		long hand = Hand.createHand("Ac", "Js", "Ts", "3h", "3d", "3s", "7s");
		long outkicked = Hand.createHand("7c", "5s", "4s", "3h", "3d", "3s", "2s");
		assertTrue(evaluate(outkicked) < evaluate(hand));
	}
	
	@Test
	void onlyTwoKickersCountInEvaluation() {
		long hand = Hand.createHand("Ac", "Js", "Ts", "3h", "3d", "3s", "7s");
		long equal = Hand.createHand("Ac", "Js", "4s", "3h", "3d", "3s", "2s");
		assertTrue(evaluate(hand) == evaluate(equal));
	}
	
	@Test
	void aBoatIsNotTrips() {
		long boat = Hand.createHand("Qs", "Jd", "Jh", "Jc", "Ac", "3c", "Ad");
		assertEquals(0L, evaluate(boat) & TRIP_MASK);
	}
	
	@Test
	void tripsAreNotAnyOfTheHigher() {
		long hand = Hand.createHand("Ac", "Js", "Ts", "2h", "2d", "2s", "7s");
		assertEquals(0L, evaluate(hand) & (STRAIGHT_MASK | FLUSH_MASK | BOAT_MASK | QUAD_MASK | SF_MASK));
	}

	@Test
	void aStraightIsNotTrips() {
		long hand = Hand.createHand("Ac", "3s", "4s", "2h", "2d", "2s", "5s");
		assertEquals(0L, evaluate(hand) & TRIP_MASK);
	}
	
	@Test
	void theTripsAreInTheLSP() {
		long hand = Hand.createHand("Ac", "Js", "Ts", "3h", "3d", "3s", "7s");
		assertEquals(1L << Hand.TREY_INDEX, (evaluate(hand) & MSP_MASK) >> MSP_INDEX);
	}

	// TWO PAIRS
	@Test
	void onePairIsNotTwoPairs() {
		long hand = Hand.createHand("Ac", "Js", "Ts", "2h", "2d", "8s", "7s");
		assertEquals(0L, evaluate(hand) & TWO_PAIR_MASK);
	}
	
	@Test
	void threePairsAreTwoPairs() {
		long hand = Hand.createHand("Jc", "Js", "Ts", "2h", "2d", "Td", "7s");
		assertNotEquals(0L, evaluate(hand) & TWO_PAIR_MASK);
	}
	
	@Test
	void twoPairsAreTwoPairs() {
		long hand = Hand.createHand("Ac", "Js", "Ts", "7h", "2d", "2s", "7s");
		assertNotEquals(0L, evaluate(hand) & TWO_PAIR_MASK);
	}

	@Test
	void twoPairsAreEvaluatedOnTheHighestRank() {
		long hand = Hand.createHand("Ac", "Js", "Ts", "7h", "2d", "2s", "7s");
		long bigger = Hand.createHand("8c", "5s", "4s", "8h", "3d", "3s", "2s");
		assertTrue(evaluate(bigger)> evaluate(hand));
	}
	
	@Test
	void twoPairsAreEvaluatedOnTheLowestRankAfterTheHighest() {
		long hand = Hand.createHand("Ac", "Js", "Ts", "7h", "2d", "2s", "7s");
		long bigger = Hand.createHand("7c", "5s", "4s", "7h", "3d", "3s", "2s");
		assertTrue(evaluate(bigger)> evaluate(hand));
	}
	
	@Test
	void equalTwoPairsAreEvaluatedOnKickers() {
		long hand = Hand.createHand("Ac", "Js", "Ts", "7h", "3d", "3s", "7s");
		long outkicked = Hand.createHand("7c", "5s", "4s", "7h", "3d", "3s", "2s");
		assertTrue(evaluate(outkicked) < evaluate(hand));
	}
	
	@Test
	void thirdPairIsCounterfeited() {
		long threePairs = Hand.createHand("Jc", "Js", "8s", "7h", "2d", "8d", "7s");
		long twoPairs = Hand.createHand("Jc", "Js", "8s", "7h", "2d", "8d", "3s");
		assertTrue(evaluate(threePairs) == evaluate(twoPairs));
	}
	
	@Test
	void onlyOneKickerCountsInTwoPair() {
		long twoPairs = Hand.createHand("Jc", "Js", "8s", "3h", "2d", "8d", "9s");
		long equal = Hand.createHand("Jc", "Js", "8s", "7h", "2d", "8d", "9s");
		assertTrue(evaluate(equal) == evaluate(twoPairs));
	}
	
	@Test
	void twoPairsAreNoneOfTheAbove() {
		long twoPairs = Hand.createHand("Jc", "Js", "8s", "3h", "2d", "8d", "9s");
		assertEquals(0, evaluate(twoPairs) & (TRIP_MASK | STRAIGHT_MASK | FLUSH_MASK | BOAT_MASK | QUAD_MASK | SF_MASK));
	}
	
	// ONE PAIR
	@Test
	void onePairIsInMSP() {
		long hand = Hand.createHand("Ac", "Js", "Ts", "2h", "2d", "8s", "7s");
		assertEquals(1L << Hand.DEUCE_INDEX, (evaluate(hand) & MSP_MASK) >> MSP_INDEX);
	}
	
	@Test
	void onePairIsNotInLSP() {
		long hand = Hand.createHand("Ac", "Js", "Ts", "2h", "2d", "8s", "7s");
		assertEquals(0L, evaluate(hand) & LSP_MASK);
	}
	
	@Test
	void onePairSetsNoneOfTheFlags() {
		long hand = Hand.createHand("Ac", "Js", "Ts", "2h", "2d", "8s", "7s");
		assertEquals(0, evaluate(hand) & (TWO_PAIR_MASK | TRIP_MASK | STRAIGHT_MASK | FLUSH_MASK | BOAT_MASK | QUAD_MASK | SF_MASK));
	}

	@Test
	void samePairIsDecidedOnHighestKicker() {
		long hand = Hand.createHand("Ac", "Js", "Ts", "2h", "2d", "8s", "7s");
		long higher = Hand.createHand("Ac", "Qs", "Ts", "2h", "2d", "8s", "7s");
		assertTrue(evaluate(higher) > evaluate(hand));
	}

	@Test
	void onlyThreeKickersCountInPair() {
		long hand = Hand.createHand("Ac", "Js", "Ts", "2h", "2d", "8s", "7s");
		long equal = Hand.createHand("Ac", "Js", "Ts", "2h", "2d", "9s", "7s");
		assertTrue(evaluate(equal) == evaluate(hand));
	}
	
	//NOTHING
	@Test
	void nothingHandSetsNeitherPair() {
		long hand = Hand.createHand("Ac", "Js", "Ts", "3h", "2d", "8s", "7s");
		assertEquals(0L, evaluate(hand) & (MSP_MASK | LSP_MASK));
	}
	
	@Test
	void nothingHandSetsNoneOfTheFlags() {
		long hand = Hand.createHand("Ac", "Js", "Ts", "3h", "2d", "8s", "7s");
		assertEquals(0, evaluate(hand) & (TWO_PAIR_MASK | TRIP_MASK | STRAIGHT_MASK | FLUSH_MASK | BOAT_MASK | QUAD_MASK | SF_MASK));
	}
	
	@Test
	void nothingHandIsDecidedOnHighestKicker() {
		long hand = Hand.createHand("Ac", "Js", "Ts", "3h", "2d", "8s", "7s");
		long higher = Hand.createHand("Ac", "Qs", "Ts", "3h", "2d", "8s", "7s");
		assertTrue(evaluate(higher) > evaluate(hand));
	}
	
	@Test
	void onlyFiveKickersCountInNothingHand() {
		long hand = Hand.createHand("Ac", "Js", "Ts", "3h", "2d", "8s", "7s");
		long equal = Hand.createHand("Ac", "Js", "Ts", "4h", "2d", "8s", "7s");
		assertTrue(evaluate(equal) == evaluate(hand));
	}
}
