package dk.olehougaard.poker;

import static dk.olehougaard.poker.Hand.ACE_INDEX;
import static dk.olehougaard.poker.Hand.ALL_SUIT_POSITIONS;
import static dk.olehougaard.poker.Hand.BITS_PER_SUIT;
import static dk.olehougaard.poker.Hand.CLUB_INDEX;
import static dk.olehougaard.poker.Hand.CLUB_MASK;
import static dk.olehougaard.poker.Hand.CLUB_POSITION;
import static dk.olehougaard.poker.Hand.DEUCE_INDEX;
import static dk.olehougaard.poker.Hand.DIAMOND_INDEX;
import static dk.olehougaard.poker.Hand.DIAMOND_MASK;
import static dk.olehougaard.poker.Hand.DIAMOND_POSITION;
import static dk.olehougaard.poker.Hand.FIVE_INDEX;
import static dk.olehougaard.poker.Hand.HEART_INDEX;
import static dk.olehougaard.poker.Hand.HEART_MASK;
import static dk.olehougaard.poker.Hand.HEART_POSITION;
import static dk.olehougaard.poker.Hand.LOW_ACE_INDEX;
import static dk.olehougaard.poker.Hand.SPADE_INDEX;
import static dk.olehougaard.poker.Hand.SPADE_MASK;
import static dk.olehougaard.poker.Hand.SPADE_POSITION;

public class Evaluator {
	private static final long WHEEL_PATTERN = (1L << 5) - 1;
	private static final long BROADWAY_PATTERN = WHEEL_PATTERN << (ACE_INDEX - FIVE_INDEX);
	private static final long ACE_MASK = (1L << ACE_INDEX) * ALL_SUIT_POSITIONS;
	
	private static final long NARROW_PAIR_MASK = DIAMOND_POSITION | CLUB_POSITION;
	private static final long MIDDLE_PAIR_MASK = HEART_POSITION | CLUB_POSITION;
	private static final long WIDE_PAIR_MASK = SPADE_POSITION | CLUB_POSITION;
	
	public static final int UNPAIRED_INDEX = 0;
	public static final int LSP_INDEX = UNPAIRED_INDEX + ACE_INDEX + 1;
	private static final long LSP_POS = 1L << LSP_INDEX;
	public static final int MSP_INDEX = LSP_INDEX + 4;
	private static final long MSP_POS = 1L << MSP_INDEX;
	public static final int TWO_PAIR_INDEX = MSP_INDEX + 4;
	public static final int TRIP_INDEX = TWO_PAIR_INDEX + 1;
	public static final int STRAIGHT_INDEX = TRIP_INDEX + 1;
	public static final int FLUSH_INDEX = STRAIGHT_INDEX + 1;
	public static final int BOAT_INDEX = FLUSH_INDEX + 1;
	public static final int QUAD_INDEX = BOAT_INDEX + 1;
	public static final int SF_INDEX = QUAD_INDEX + 1;

	public static final long UNPAIRED_MASK = LSP_POS - 1; 
	public static final long LEAST_SIGNIFICANT_PAIR_MASK = 0xF << LSP_INDEX;
	public static final long MOST_SIGNIFICANT_PAIR_MASK = 0xF << MSP_INDEX;
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
		int[] pairs = new int[ACE_INDEX + 1];
		int[] pair_signature = new int[3];
		final int PAIRS = 0, TRIPS = 1, QUADS = 2;
		final int PAIRS_IN_PAIR = 1, PAIRS_IN_TRIPS = 3, PAIRS_IN_QUADS = 6;
		final long[] masks = { WIDE_PAIR_MASK, MIDDLE_PAIR_MASK, NARROW_PAIR_MASK };
		for (int k = 0; k < masks.length; k++) {
			long mask = masks[k];
			for(int i = 0; i <= k * BITS_PER_SUIT + ACE_INDEX; mask <<= 1, i++) {
				if ((mask & hand) == mask) {
					pairs[i % BITS_PER_SUIT]++;
					switch(pairs[i % BITS_PER_SUIT]) {
					case PAIRS_IN_PAIR:
						pair_signature[PAIRS]++;
						break;
					case PAIRS_IN_TRIPS:
						pair_signature[PAIRS]--;
						pair_signature[TRIPS]++;
						break;
					case PAIRS_IN_QUADS:
						pair_signature[TRIPS]--;
						pair_signature[QUADS]++;
						break;
					default:
					}
				}
			}
		}
		if (pair_signature[QUADS] >= 1) {
			for(int i = ACE_INDEX; i >= DEUCE_INDEX; i--) {
				if (pairs[i] == PAIRS_IN_QUADS) {
					long rest = valuesOnly(hand) & ~(1L << i);
					long kicker = 1L << ACE_INDEX;
					while((kicker & rest) == 0) kicker >>= 1;
					return QUAD_MASK | ((i - DEUCE_INDEX + 2) * MSP_POS) | kicker;
				}
			}
		} else if (pair_signature[TRIPS] >= 1 && pair_signature[PAIRS] >= 1 || pair_signature[TRIPS] >= 2) {
			for(int i = ACE_INDEX; i >= DEUCE_INDEX; i--) {
				if (pairs[i] == PAIRS_IN_TRIPS) {
					pairs[i] = 0;
					for(int j = ACE_INDEX; j >= DEUCE_INDEX; j--) {
						if (pairs[j] >= PAIRS_IN_PAIR) {
							return BOAT_MASK | ((i - DEUCE_INDEX + 2) * MSP_POS) | ((j - DEUCE_INDEX + 2) * LSP_POS);
						}
					}
				}
			}
		}
		return 0L;
	}
	
	private static final long HAMMING8  = (1L << 8) - 1;
	private static final long HAMMING4  = HAMMING8 ^ (HAMMING8 << 4);
	private static final long HAMMING2  = HAMMING4 ^ (HAMMING4 << 2);
	private static final long HAMMING1  = HAMMING2 ^ (HAMMING2 << 1);
	
	private static long evaluateFlush(long hand) {
		for(long mask = SPADE_MASK, index = SPADE_INDEX; mask != 0; mask >>>= BITS_PER_SUIT, index >>>= BITS_PER_SUIT) {
			long cardsInSuit = (hand & mask) >> index;
			long bits = cardsInSuit;
			bits = (bits & HAMMING1) + ((bits >> 1) & HAMMING1);
			bits = (bits & HAMMING2) + ((bits >> 2) & HAMMING2);
			bits = (bits & HAMMING4) + ((bits >> 4) & HAMMING4);
			bits = (bits & HAMMING8) + ((bits >> 8) & HAMMING8);
			if (bits >= 5) {
				while(bits > 5) {
					cardsInSuit &= cardsInSuit - 1;
					bits--;
				}
				return FLUSH_MASK | cardsInSuit;
			}
		}
		return 0L;
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
		return valuesOnly(hand);
	}
}
