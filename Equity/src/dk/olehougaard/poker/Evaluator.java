package dk.olehougaard.poker;

import static dk.olehougaard.poker.Hand.ACE_INDEX;
import static dk.olehougaard.poker.Hand.ALL_SUIT_POSITIONS;
import static dk.olehougaard.poker.Hand.BITS_PER_SUIT;
import static dk.olehougaard.poker.Hand.CLUB_INDEX;
import static dk.olehougaard.poker.Hand.CLUB_MASK;
import static dk.olehougaard.poker.Hand.DEUCE_INDEX;
import static dk.olehougaard.poker.Hand.DIAMOND_INDEX;
import static dk.olehougaard.poker.Hand.DIAMOND_MASK;
import static dk.olehougaard.poker.Hand.FIVE_INDEX;
import static dk.olehougaard.poker.Hand.HEART_INDEX;
import static dk.olehougaard.poker.Hand.HEART_MASK;
import static dk.olehougaard.poker.Hand.LOW_ACE_INDEX;
import static dk.olehougaard.poker.Hand.SPADE_INDEX;
import static dk.olehougaard.poker.Hand.SPADE_MASK;

public class Evaluator {
	private static final long WHEEL_PATTERN = (1L << 5) - 1;
	private static final long BROADWAY_PATTERN = WHEEL_PATTERN << (ACE_INDEX - FIVE_INDEX);
	private static final long ACE_MASK = (1L << ACE_INDEX) * ALL_SUIT_POSITIONS;
	
	public static final int UNPAIRED_INDEX = 0;
	public static final int LSP_INDEX = UNPAIRED_INDEX + ACE_INDEX + 1;
	public static final int MSP_INDEX = LSP_INDEX + 4;
	public static final int TWO_PAIR_INDEX = MSP_INDEX + 4;
	public static final int TRIP_INDEX = TWO_PAIR_INDEX + 1;
	public static final int STRAIGHT_INDEX = TRIP_INDEX + 1;
	public static final int FLUSH_INDEX = STRAIGHT_INDEX + 1;
	public static final int BOAT_INDEX = FLUSH_INDEX + 1;
	public static final int QUAD_INDEX = BOAT_INDEX + 1;
	public static final int SF_INDEX = QUAD_INDEX + 1;

	public static final long UNPAIRED_MASK = (1L << LSP_INDEX) - 1; 
	public static final long LSP_MASK = 0xF << LSP_INDEX;
	public static final long MSP_MASK = 0xF << MSP_INDEX;
	public static final long TWO_PAIR_MASK = 1L << TWO_PAIR_INDEX;
	public static final long TRIP_MASK = 1L << TRIP_INDEX;
	public static final long STRAIGHT_MASK = 1L << STRAIGHT_INDEX;
	public static final long FLUSH_MASK = 1L << FLUSH_INDEX;
	public static final long BOAT_MASK = 1L << BOAT_INDEX;
	public static final long QUAD_MASK = 1L << QUAD_INDEX;
	public static final long SF_MASK = 1L << SF_INDEX;
	
	private static long evaluateStraightFlush(long hand) {
		hand |= (hand & ACE_MASK) >> (ACE_INDEX - LOW_ACE_INDEX);
		for(long pattern = BROADWAY_PATTERN; pattern >= WHEEL_PATTERN; pattern >>= 1) {
			final long club_pattern = pattern << CLUB_INDEX;
			if ((club_pattern & hand) == club_pattern) return SF_MASK | pattern;
			final long diamond_pattern = pattern << DIAMOND_INDEX;
			if ((diamond_pattern & hand) == diamond_pattern) return SF_MASK | pattern;
			final long heart_pattern = pattern << HEART_INDEX;
			if ((heart_pattern & hand) == heart_pattern) return SF_MASK | pattern;
			final long spade_pattern = pattern << SPADE_INDEX;
			if ((spade_pattern & hand) == spade_pattern) return SF_MASK | pattern;
		}
		return 0L;
	}
	
	public static final int SHORT_MASK = 0xffff;
	public static final short DE_BRUIJN_SEQUENCE = 0b0000111101100101;
	public static final int[] DE_BRUIJN_HASH = {0, 1, 11, 2, 14, 12, 8, 3, 15, 10, 13, 7, 9, 6, 5, 4};
	
	private static long evaluatePaired(long hand) {
		long close_paired = hand & (hand >> BITS_PER_SUIT);
		long mid_paired = hand & (hand >> 2 * BITS_PER_SUIT);
		long distant_paired = hand & (hand >> 3 * BITS_PER_SUIT);
		long paired = (close_paired >> 32) | ((close_paired & DIAMOND_MASK) >> 16) | (close_paired & CLUB_MASK) | (mid_paired >> 16) | (mid_paired & CLUB_MASK) | distant_paired;
		long hand_type = 0L;
		int msp = 0;
		int lsp = 0;
		int kicker_count = 5;
		int distinct_values = 7;
		while (paired != 0) {
			final short lsb = (short)(paired & (-paired));
			final int lsb_index = Evaluator.DE_BRUIJN_HASH[((lsb * Evaluator.DE_BRUIJN_SEQUENCE) & Evaluator.SHORT_MASK) >>> 12];
			final int pair = lsb_index + 2 - DEUCE_INDEX;
			final int pair_count = (int)((lsb & (hand >> SPADE_INDEX)) + (lsb & (hand >> HEART_INDEX)) + (lsb & (hand >> DIAMOND_INDEX)) + (lsb & hand)) >> lsb_index;
			switch (pair_count) {
			case 4:
				distinct_values -= 3;
				hand_type = QUAD_MASK;
				msp = pair;
				lsp = 0;
				kicker_count = 1;
				break;
			case 3:
				distinct_values -= 2;
				if (hand_type != QUAD_MASK) {
					if (msp != 0) {
						hand_type = BOAT_MASK;
						lsp = msp;
						msp = pair;
						kicker_count = 0;
					} else {
						hand_type = TRIP_MASK;
						msp = pair;
						kicker_count = 2;
					}
				}
				break;
			case 2:
				distinct_values--;
				if (hand_type != QUAD_MASK) {
					if (hand_type == BOAT_MASK) {
						lsp = pair;
					} else if (hand_type == TRIP_MASK) {
						hand_type = BOAT_MASK;
						lsp = pair;
						kicker_count = 0;
					} else if (msp != 0) {
						hand_type = TWO_PAIR_MASK;
						lsp = msp;
						msp = pair;
						kicker_count = 1;
					} else {
						msp = pair;
						kicker_count = 3;
					}
				}
			}
			paired &= paired - 1;
		}
		long bit_count = distinct_values;
		long msp_mask = 0;
		if (msp != 0) {
			msp_mask = 1L << (msp - 1);
			bit_count--;
		}
		long lsp_mask = 0;
		if (lsp != 0) {
			lsp_mask = 1L << (lsp - 1);
			bit_count--;
		}
		long unpaired = valuesOnly(hand) & ~(msp_mask | lsp_mask);
		while (bit_count-- > kicker_count) unpaired &= unpaired - 1;
		return hand_type | (msp << MSP_INDEX) | (lsp << LSP_INDEX) | unpaired;
	}
	
	private static final long HAMMING8  = (1L << 8) - 1;
	private static final long HAMMING4  = HAMMING8 ^ (HAMMING8 << 4);
	private static final long HAMMING2  = HAMMING4 ^ (HAMMING4 << 2);
	private static final long HAMMING1  = HAMMING2 ^ (HAMMING2 << 1);
	
	private static long evaluateFlush(long hand) {
		for(long mask = SPADE_MASK, index = SPADE_INDEX; mask != 0; mask >>>= BITS_PER_SUIT, index >>>= BITS_PER_SUIT) {
			long cardsInSuit = (hand & mask) >> index;
			long bits = countBits(cardsInSuit);
			if (bits >= 5) {
				while(bits-- > 5) cardsInSuit &= cardsInSuit - 1;
				return FLUSH_MASK | cardsInSuit;
			}
		}
		return 0L;
	}

	private static long countBits(long cardsInSuit) {
		long bits = cardsInSuit;
		bits = (bits & HAMMING1) + ((bits >> 1) & HAMMING1);
		bits = (bits & HAMMING2) + ((bits >> 2) & HAMMING2);
		bits = (bits & HAMMING4) + ((bits >> 4) & HAMMING4);
		bits = (bits & HAMMING8) + ((bits >> 8) & HAMMING8);
		return bits;
	}
	
	private static long evaluateStraight(long hand) {
		hand |= (hand & ACE_MASK) >> (ACE_INDEX - LOW_ACE_INDEX);
		hand = valuesOnly(hand);
		for(long pattern = BROADWAY_PATTERN; pattern >= WHEEL_PATTERN; pattern >>= 1) {
			if ((pattern & hand) == pattern) return STRAIGHT_MASK | pattern;
		}
		return 0L;
	}
	
	private static long valuesOnly(long hand) {
		return (hand & CLUB_MASK) >> CLUB_INDEX | (hand & DIAMOND_MASK) >> DIAMOND_INDEX | (hand & HEART_MASK) >> HEART_INDEX | (hand & SPADE_MASK) >> SPADE_INDEX;
	}
	
	public static long evaluate(long hand) {
		long sf = evaluateStraightFlush(hand);
		if (sf != 0) return sf;
		long pairs = evaluatePaired(hand);
		if ((pairs & (QUAD_MASK | BOAT_MASK)) != 0) return pairs;
		long flush = evaluateFlush(hand);
		if ((flush & FLUSH_MASK) != 0) return flush;
		long straight = evaluateStraight(hand);
		if ((straight & STRAIGHT_MASK) != 0) return straight;
		return pairs;
	}
}
