package dk.olehougaard.poker;

import static dk.olehougaard.poker.Hand.ACE_INDEX;
import static dk.olehougaard.poker.Hand.ALL_SUIT_POSITIONS;
import static dk.olehougaard.poker.Hand.BITS_PER_SUIT;
import static dk.olehougaard.poker.Hand.CLUB_MASK;
import static dk.olehougaard.poker.Hand.FIVE_INDEX;
import static dk.olehougaard.poker.Hand.LOW_ACE_INDEX;

public class Evaluator {
	private static final long WHEEL_PATTERN = (1L << 5) - 1;
	private static final long BROADWAY_PATTERN = WHEEL_PATTERN << (ACE_INDEX - FIVE_INDEX);
	private static final long ACE_MASK = (1L << ACE_INDEX) * ALL_SUIT_POSITIONS;
	
	public static final int UNPAIRED_INDEX = 0;
	public static final int LSP_INDEX = UNPAIRED_INDEX + ACE_INDEX + 1;
	public static final int MSP_INDEX = LSP_INDEX + ACE_INDEX + 1;
	public static final int TWO_PAIR_INDEX = MSP_INDEX + ACE_INDEX + 1;
	public static final int TRIP_INDEX = TWO_PAIR_INDEX + 1;
	public static final int STRAIGHT_INDEX = TRIP_INDEX + 1;
	public static final int FLUSH_INDEX = STRAIGHT_INDEX + 1;
	public static final int BOAT_INDEX = FLUSH_INDEX + 1;
	public static final int QUAD_INDEX = BOAT_INDEX + 1;
	public static final int SF_INDEX = QUAD_INDEX + 1;

	public static final long UNPAIRED_MASK = (1L << LSP_INDEX) - 1; 
	public static final long LSP_MASK = ((1L << MSP_INDEX) - 1) & ~UNPAIRED_MASK;
	public static final long MSP_MASK = ((1L << TWO_PAIR_INDEX) - 1) & ~(LSP_MASK | UNPAIRED_MASK);
	public static final long TWO_PAIR_MASK = 1L << TWO_PAIR_INDEX;
	public static final long TRIP_MASK = 1L << TRIP_INDEX;
	public static final long STRAIGHT_MASK = 1L << STRAIGHT_INDEX;
	public static final long FLUSH_MASK = 1L << FLUSH_INDEX;
	public static final long BOAT_MASK = 1L << BOAT_INDEX;
	public static final long QUAD_MASK = 1L << QUAD_INDEX;
	public static final long SF_MASK = 1L << SF_INDEX;
	
	private static long evaluateStraightFlush(long hand) {
		hand |= (hand & ACE_MASK) >> (ACE_INDEX - LOW_ACE_INDEX);
		final short clubs = (short) hand;
		hand >>= BITS_PER_SUIT;
		final short diamonds = (short) hand;
		hand >>= BITS_PER_SUIT;
		final short hearts = (short) hand;
		hand >>= BITS_PER_SUIT;
		final short spades = (short) hand;
		for(long pattern = BROADWAY_PATTERN; pattern >= WHEEL_PATTERN; pattern >>= 1) {
			if ((pattern & clubs) == pattern || 
				(pattern & diamonds) == pattern || 
				(pattern & hearts) == pattern || 
				(pattern & spades) == pattern) return SF_MASK | pattern;
		}
		return 0L;
	}
	
	private static long evaluatePaired(long hand) {
		long odd_paired = hand & ((hand >>> BITS_PER_SUIT) | (hand << Long.SIZE - BITS_PER_SUIT));
		long even_paired = hand & (hand >> 2 * BITS_PER_SUIT);
		long paired = odd_paired | even_paired;
		paired |= paired >> 2 * BITS_PER_SUIT;
		paired |= paired >> BITS_PER_SUIT;
		paired &= CLUB_MASK;
		long hand_type = 0L;
		long msp_mask = 0L;
		long lsp_mask = 0L;
		int counterfeited = 0;
		while (paired != 0) {
			final short lsb = (short)(paired & (-paired));
			long pair_count = hand & (lsb * ALL_SUIT_POSITIONS);
			pair_count += pair_count >> 2 * BITS_PER_SUIT;
			pair_count += pair_count >> BITS_PER_SUIT;
			switch ((short)pair_count / lsb) {
			case 4:
				if (hand_type == TRIP_MASK) {
					counterfeited = 2;
				} else if (msp_mask != 0) {
					counterfeited = 1;
				}
				hand_type = QUAD_MASK;
				msp_mask = lsb;
				lsp_mask = 0;
				break;
			case 3:
				if (hand_type == QUAD_MASK) {
					counterfeited = 2;
				} else if (msp_mask != 0) {
					hand_type = BOAT_MASK;
					lsp_mask = msp_mask;
					msp_mask = lsb;
				} else {
					hand_type = TRIP_MASK;
					msp_mask = lsb;
				}
				break;
			case 2:
				if (hand_type == QUAD_MASK) {
					counterfeited = 1;
				} else if (hand_type == BOAT_MASK) {
					lsp_mask = lsb;
				} else if (hand_type == TRIP_MASK) {
					hand_type = BOAT_MASK;
					lsp_mask = lsb;
				} else if (msp_mask != 0) {
					hand_type = TWO_PAIR_MASK;
					if (lsp_mask != 0) counterfeited = 1;
					lsp_mask = msp_mask;
					msp_mask = lsb;
				} else {
					msp_mask = lsb;
				}
			}
			paired &= paired - 1;
		}
		long unpaired = valuesOnly(hand) & ~(msp_mask | lsp_mask);
		if (counterfeited < 2) unpaired &= unpaired - 1;
		if (counterfeited < 1) unpaired &= unpaired - 1;
		return hand_type | (msp_mask << MSP_INDEX) | (lsp_mask << LSP_INDEX) | unpaired;
	}
	
	private static final long HAMMING8  = (1L << 8) - 1;
	private static final long HAMMING4  = HAMMING8 ^ (HAMMING8 << 4);
	private static final long HAMMING2  = HAMMING4 ^ (HAMMING4 << 2);
	private static final long HAMMING1  = HAMMING2 ^ (HAMMING2 << 1);
	
	private static long evaluateFlush(long hand) {
		while(hand != 0) {
			short suit = (short) hand;
			long bits = countBits(suit);
			if (bits >= 5) {
				while(bits-- > 5) suit &= suit - 1;
				return FLUSH_MASK | suit;
			}
			hand >>>= BITS_PER_SUIT;
		}
		return 0L;
	}

	private static long countBits(long cardsInSuit) {
		long bits = cardsInSuit;
		bits = (bits & HAMMING1) + ((bits >> 1) & HAMMING1);
		bits = (bits & HAMMING2) + ((bits >> 2) & HAMMING2);
		bits = (bits & HAMMING4) + ((bits >> 4) & HAMMING4);
		bits = (bits & HAMMING8) + (bits >> 8);
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
		hand |= hand >> 2 * BITS_PER_SUIT;
		hand |= hand >> BITS_PER_SUIT;
		return hand & CLUB_MASK;
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
