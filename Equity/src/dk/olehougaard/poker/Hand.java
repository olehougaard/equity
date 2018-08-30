package dk.olehougaard.poker;

public class Hand {
	public static final int BITS_PER_SUIT = 16;
	
	public static final int CLUB_INDEX = 0;
	public static final int DIAMOND_INDEX = CLUB_INDEX + BITS_PER_SUIT;
	public static final int HEART_INDEX = DIAMOND_INDEX + BITS_PER_SUIT;
	public static final int SPADE_INDEX = HEART_INDEX + BITS_PER_SUIT;
	
	public static final long CLUB_POSITION = 1L << CLUB_INDEX;
	public static final long DIAMOND_POSITION = 1L << DIAMOND_INDEX;
	public static final long HEART_POSITION = 1L << HEART_INDEX;
	public static final long SPADE_POSITION = 1L << SPADE_INDEX;
	public static final long ALL_SUIT_POSITIONS = CLUB_POSITION | DIAMOND_POSITION | HEART_POSITION | SPADE_POSITION;
	
	public static final long CLUB_MASK = (1L << DIAMOND_INDEX) - 1;
	public static final long DIAMOND_MASK = (1L << HEART_INDEX) - 1 ^ (1L << DIAMOND_INDEX) - 1;
	public static final long HEART_MASK = (1L << SPADE_INDEX) - 1 ^ (1L << HEART_INDEX) - 1;
	public static final long SPADE_MASK =  -1L ^ (1L << SPADE_INDEX) - 1;
	
	public static final int LOW_ACE_INDEX = 0;
	public static final int DEUCE_INDEX = 1;
	public static final int TREY_INDEX = 2;
	public static final int FOUR_INDEX = 3;
	public static final int FIVE_INDEX = 4;
	public static final int SIX_INDEX = 5;
	public static final int SEVEN_INDEX = 6;
	public static final int EIGHT_INDEX = 7;
	public static final int NINE_INDEX = 8;
	public static final int TEN_INDEX = 9;
	public static final int JACK_INDEX = 10;
	public static final int QUEEN_INDEX = 11;
	public static final int KING_INDEX = 12;
	public static final int ACE_INDEX = 13;
	
	private static final String SUITS = "CDHS";
	private static final String VALUES = "23456789TJQKA";
	
	public static long createCard(String description) {
		if (description == null || description.length() != 2) throw new IllegalArgumentException("Not a card description: " + description);
		description = description.toUpperCase();
		int valueIndex = VALUES.indexOf(description.charAt(0));
		int suitIndex = SUITS.indexOf(description.charAt(1));
		if (suitIndex == -1 || valueIndex == -1) throw new IllegalArgumentException("Not a card description: " + description);
		return 1L << ( suitIndex * BITS_PER_SUIT + valueIndex + 1 );
	}
	
	public static long createHand(String... descriptions) {
		long hand = 0;
		for( String description: descriptions ) {
			hand |= createCard(description);
		}
		return hand;
	}
}
