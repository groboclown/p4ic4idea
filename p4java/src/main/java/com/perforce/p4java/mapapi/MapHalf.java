package com.perforce.p4java.mapapi;

import java.util.concurrent.atomic.AtomicInteger;

import static com.perforce.p4java.mapapi.MapChar.MapCharClass.cCHAR;
import static com.perforce.p4java.mapapi.MapChar.MapCharClass.cDOTS;
import static com.perforce.p4java.mapapi.MapChar.MapCharClass.cEOS;
import static com.perforce.p4java.mapapi.MapChar.MapCharClass.cSLASH;
import static com.perforce.p4java.mapapi.MapChar.PARAM_BASE_DOTS;
import static com.perforce.p4java.mapapi.MapChar.PARAM_BASE_TOP;

public class MapHalf {

	static final int PARAM_VECTOR_LENGTH = 30; // %0 - %9, 10 *'s, and 10 ...'s
	static final int PARAM_MAX_BACKTRACK = 10;
	static final int PARAM_MAX_WILDS = 10;

//
// MapHalf - half of a mapping, i.e. a pattern
//
// Methods defined:
//
//	MapHalf::MapHalf() - convert pattern to internal form and save
//	MapHalf::~MapHalf() - dispose of saved internal form of map
//	MapHalf::Compare() - strcmp() two MapHalf patterns for sorting
//	MapHalf::GetCommonLen() - match non-wild initial substring of pattern
//	MapHalf::Get() - return the pattern as a char *
//	MapHalf::HasEmbWild() - returns non-zero on embedded/leading wildcards
//	MapHalf::HasPosWild() - returns non-zero on positional wildcards
//	MapHalf::Match() - fast match a string against a map pattern
//	MapHalf::Expand() - expand result of Match using a pattern
//	MapHalf::Join() - join two MapHalfs together
// 	MapHalf::Validate() - do these patterns have the same wildcards
//
// Internal routines:
//	JoinWild() - push retry on stack when wildcard encountered in pattern
//

	private MapChar[] mapChar = null;    // compiled version
	private int mapTail;    // non-wildcard tail start
	private int mapEnd;    // non-wildcard tail end
	private int fixedLen;    // How much until wildcard
	private boolean isWild;        // has a wildcard at all?
	private int nWilds;        // number of wildcards
	private int caseMode = -1;    // case sensitivity mode
	private String half = "";

	MapHalf() {
	}

	MapHalf(String newHalf) {
		set(newHalf);
	}

	public String get() {
		return toString();
	}

	@Override
	public String toString() {
		return half;
	}

//
// MapHalf::MapHalf() - convert pattern to internal form and save
//

	void set(String newHalf) {
		if (newHalf == null) {
			newHalf = "";
		}

		// Allocate

		int l = newHalf.length() + 1;

		// Save the string; passed outwards by MapTable::Strings

		half = newHalf;
		mapChar = new MapChar[l];
		for (int i = 0; i < mapChar.length; i++) {
			mapChar[i] = new MapChar();
		}

		// Compile into internal form.

		AtomicInteger nStars = new AtomicInteger(0);
		AtomicInteger nDots = new AtomicInteger(0);
		AtomicInteger pos = new AtomicInteger(0);
		int mc = 0;

		while (mapChar[mc].set(newHalf.toCharArray(), pos, nStars, nDots, caseMode))
			++mc;

		// Find non-wildcard tail

		mapEnd = mc;

		while (mc > 0 && (mapChar[mc - 1].cc == cCHAR || mapChar[mc - 1].cc == cSLASH))
			--mc;

		mapTail = mc;

		// Find the length of the non-wildcard initial substring
		// This is used by MapTable::Strings to figure out what
		// initial substrings to use for table probes.

		mc = 0;

		while (mapChar[mc].cc == cCHAR || mapChar[mc].cc == cSLASH)
			++mc;

		isWild = mapChar[mc].cc != cEOS;
		fixedLen = mc;

		// Count number of wildcards in string

		for (nWilds = 0, mc = 0; mapChar[mc].cc != cEOS; mc++)
			if (mapChar[mc].isWild())
				++nWilds;
	}

	boolean tooWild(Error e) {
		if (nWilds > 10) //ToDO: p4tunable.Get( P4TUNE_MAP_MAXWILD ) )
		{
			//ToDo: e.Set( MsgDb::TooWild2 );
			return true;
		}
		return false;
	}

	boolean hasSubDirs(int match) {
		int mc = match;

		// Can this pattern match a subdirectory after its
		// non-wildcard initial substring?

		while (mapChar[mc].cc != cEOS && mapChar[mc].cc != cSLASH && mapChar[mc].cc != cDOTS)
			++mc;

		return mapChar[mc].cc != cEOS;
	}

	boolean hasEndSlashEllipses() {
		// Test for '/...', or '\..' (UNC path) at end of string

		int mc = mapEnd - 1;

		if (!isWild)
			return false;

		if (mc == 0 || (mapChar[mc - 1].cc != cSLASH && mapChar[mc - 1].c != '\\')) {
			return false;
		}

		return mapChar[mc].cc == cDOTS;
	}

//
// MapHalf::Compare() - strcmp() two MapHalf patterns for sorting
//

	static final int CmpGrid[][] = new int[][]{
	    /*           EOS  CHAR  SLASH  PERC  STAR  DOTS */
        /* EOS */  {0, -1, -1, 0, 0, 0},
        /* CHAR */ {1, -2, -2, 1, 1, 1},
        /* SLASH */{1, -2, 2, 1, 1, 1},
        /* PERC */ {0, -1, -1, 0, 0, 0},
        /* STAR */ {0, -1, -1, 0, 0, 0},
        /* DOTS */ {0, -1, -1, 0, 0, 0}
	};

	int compare(MapHalf item) {
		int mc1 = 0;
		int mc2 = 0;

		// Do a quick strcmp of non-wildcard initial string

		int l = fixedLen < item.fixedLen ?
				fixedLen : item.fixedLen;

		for (; l-- != 0 && (mapChar[mc1].c - item.mapChar[mc2].c) == 0; ++mc1, ++mc2) {
			// noop
		}

		// Now do more sensitive compare

		for (; ; ++mc1, ++mc2) {
			int d;

			switch (CmpGrid[mapChar[mc1].cc.code][item.mapChar[mc2].cc.code]) {
				case -1:
					return -1;
				case 1:
					return 1;
				case 0:
					return 0;
				case -2:
					if ((d = (mapChar[mc1].c - item.mapChar[mc2].c)) != 0) return d;
				case 2:
					break;
			}
		}
	}

//
// MapHalf::Validate() - do these patterns have the same wildcards
//

	private void findParams(char[] params) throws Exception {
		int mc = 0;
		int mn = 0;
		int wilds = 0;

		for (; mapChar[mc].cc != cEOS; ++mc) {
			switch (mapChar[mc].cc) {
				case cDOTS:
					if (mapChar[mc].paramNumber >= PARAM_BASE_TOP) {
						// ToDo: e.Set( MsgDb::ExtraDots ) << *this;
						throw new Exception("MsgDb::ExtraDots " + half);
					}

					params[mapChar[mc].paramNumber] = 1;
					wilds++;
					break;

				case cSTAR:
					if (mapChar[mc].paramNumber >= PARAM_BASE_DOTS) {
						//e.Set( MsgDb::ExtraStars ) << *this;
						throw new Exception("MsgDb::ExtraStars " + half);
					}
					// fall through

				case cPERC:
					if (params[mapChar[mc].paramNumber] != 0) {
						//e.Set( MsgDb::Duplicate ) << *this;
						throw new Exception("MsgDb::Duplicate " + half);
					}
					params[mapChar[mc].paramNumber] = 1;
					wilds++;
					break;

				default:
					mn = mc;
					break;
			}

			if (mapChar[mn].c < mapChar[mc].c - 1) {
				//e.Set( MsgDb::Juxtaposed ) << *this;
				throw new Exception("MsgDb::Juxtaposed " + half);
			}
		}

		if (wilds > 10) //ToDo: p4tunable.Get( P4TUNE_MAP_MAXWILD ) )
		{
			//e.Set( MsgDb::TooWild2 );
			throw new Exception("MsgDb::TooWild2 " + half);
		}
		// We don't validate the params yet.
	}

	boolean validate(MapHalf item) throws Exception {
		int i;
		char params[][] = new char[2][PARAM_VECTOR_LENGTH];

		for (i = 0; i < PARAM_VECTOR_LENGTH; i++)
			params[0][i] = params[1][i] = 0;

		findParams(params[0]);

		// If no item provided, just check the half.

		if (item == null)
			return true;

		item.findParams(params[1]);

		for (i = 0; i < PARAM_VECTOR_LENGTH; i++) {
			if (params[0][i] != params[1][i]) {
				//ToDo: e.Set( MsgDb::WildMismatch ) << *this << *item;
				throw new Exception("MsgDb::WildMismatch " + half + " " + item.get());
			}
		}
		return true;
	}

	void setCaseMode(int caseMode) {
		this.caseMode = caseMode;
		if (mapChar != null) {
			for (int mc = 0; mapChar[mc].cc != cEOS; mc++) {
				mapChar[mc].caseMode = caseMode;
			}
		}
	}

//
// MapHalf::GetCommonLen() - match non-wild initial substring of pattern
//

	int getCommonLen(MapHalf prev) {
		int matchLen = 0;
		int mc1 = 0;
		int mc2 = 0;

		// Non-wildcard ("fixed"), matching prev

		while (matchLen < fixedLen && (mapChar[mc1].c - prev.mapChar[mc2].c) == 0) {
			++matchLen;
			++mc1;
			++mc2;
		}

		return matchLen;
	}

//
// MapHalf::Match() - fast match a string against a map pattern
//
// Broken into two parts: Match1 does the initial constant string match,
// optionally starting only at the point where the string differs from
// the previously matched string.  Match2 does the harder wildcarding
// match.
//
// Match1 compares and returns <0, 0, or >0.
// Match2 return 0 (no match) and 1 (match).
//
// Match1 against another MapHalf only compares up to smallest fixedLen.
//

	int match1(String from, int coff) {
		// 1st half matching: match the initial non-wildcard portion

		// coff allows us to start at the point where this pattern
		// differs from the previous (parent) pattern.

		int r = 0;
		for (; coff < fixedLen && coff < from.length(); ++coff) {
			if ((r = mapChar[coff].c - from.charAt(coff)) != 0) {
				return -r;
			}
		}

		if (from.length() < fixedLen)
			return -1;

		return 0;
	}

	private class match2Backup {
		int mc;
		MapParam param;
	}

	boolean match2(String from, MapParams params) {
		// 2nd half matching: match the wildcard portion.

		if (from.length() < fixedLen)
			return false;

		int input;
		int mc;

		// Check non-wildcard tail.  Handles '....gif' efficiently.

		if (isWild) {
			for (input = from.length() - 1, mc = 0; mc > mapTail && input > 0; ) {
				if ((mapChar[--mc].c - from.charAt(--input)) != 0) {
					return false;
				}
			}
		}

		// Full match after initial fixed string.

		mc = fixedLen;
		input = fixedLen;

		// If the MapChar::Sort() in Match1() was insufficient to
		// check the initial fixedLen of the map, we'll do it now.
		// See comments in strbuf.cc.

		if (caseMode == 2) {
			// Hybrid case mode...
			input -= fixedLen;
			mc -= fixedLen;
		}

		int backup = 0;
		match2Backup[] backups = new match2Backup[PARAM_MAX_BACKTRACK * 2];
		for (int i = 0; i < backups.length; i++) {
			backups[i] = new match2Backup();
		}

		for (; ; ) {
			// if( DEBUG_MATCH )
			//    p4debug.printf("matching %c vs %s\n", mc.c, input );

			switch (mapChar[mc].cc) {
				case cDOTS:
				case cPERC:
				case cSTAR:
					backups[backup].param = params.vector[mapChar[mc].paramNumber];
					backups[backup].param.start = input;

					if (mapChar[mc].cc == cDOTS) {
						while (input < from.length() && from.charAt(input) != 0) {
							++input;
						}
					} else {
						while (input < from.length() && from.charAt(input) != 0 && from.charAt(input) != '/') {
							++input;
						}
					}

					backups[backup].param.end = input;
					backups[backup].mc = ++mc;
					backup++;
					break;

				case cSLASH:
				case cCHAR:
					do {
						if (input == from.length() || mapChar[mc++].c != from.charAt(input++)) {
							if (input == from.length()) {
								input++;
								mc++;
							}
							for (; ; --backup) {
								if (backup <= 0)
									return false;

								mc = backups[backup - 1].mc;
								input = --(backups[backup - 1].param.end);
								if (input >= backups[backup - 1].param.start) {
									break;
								}
							}
							break;
						}
					} while (mapChar[mc].cc == cCHAR || mapChar[mc].cc == cSLASH);
					break;

				case cEOS:
					if (input < from.length() && from.charAt(input) != '\0') {
						for (; ; --backup) {
							if (backup <= 0)
								return false;

							mc = backups[backup - 1].mc;
							input = --(backups[backup - 1].param.end);
							if (input >= backups[backup - 1].param.start) {
								break;
							}
						}
						break;
					}
					return true;
			}
		}
	}

	private class matchBackup {
		int mc;
		int mc2start;
		int mc2end;
	}
//
// MapHalf::Match() - match a pattern against another
//
// Unlike MapTable::Join(), this just checks to see if one map is a superset
// of a 2nd.
//

	boolean match(MapHalf from) {
		int mc = 0;
		int mc2 = 0;

		int backup = 0;
		matchBackup[] backups = new matchBackup[PARAM_MAX_BACKTRACK * 2];
		for (int i = 0; i < PARAM_MAX_BACKTRACK * 2; i++) {
			backups[i] = new matchBackup();
		}

		for (; ; ) {
			switch (mapChar[mc].cc) {
				case cDOTS:
					backups[backup].mc2start = mc2;

					while (from.mapChar[mc2].cc != cEOS)
						++mc2;

					backups[backup].mc2end = mc2;
					backups[backup].mc = ++mc;
					backup++;
					break;

				case cPERC:
				case cSTAR:
					backups[backup].mc2start = mc2;

					while (from.mapChar[mc2].cc != cEOS &&
							from.mapChar[mc2].cc != cSLASH &&
							from.mapChar[mc2].cc != cDOTS)
						++mc2;

					backups[backup].mc2end = mc2;
					backups[backup].mc = ++mc;
					backup++;
					break;

				case cSLASH:
				case cCHAR:
					do {
						if (mapChar[mc].cc != from.mapChar[mc2].cc || !(mapChar[mc++].c == from.mapChar[mc2++].c)) {
							for (; ; --backup) {
								if (backup <= 0)
									return false;

								mc = backups[backup - 1].mc;
								mc2 = --(backups[backup - 1].mc2end);
								if (mc2 >= backups[backup - 1].mc2start) {
									break;
								}
							}
							break;
						}
					} while (mapChar[mc].cc == cCHAR || mapChar[mc].cc == cSLASH);
					break;

				case cEOS:
					if (from.mapChar[mc2].cc != cEOS) {
						for (; ; --backup) {
							if (backup <= 0)
								return false;

							mc = backups[backup - 1].mc;
							mc2 = --(backups[backup - 1].mc2end);
							if (mc2 >= backups[backup - 1].mc2start) {
								break;
							}
						}
						break;
					}

					return true;
			}
		}
	}

//
// MapHalf::MatchHead()
// MapHalf::MatchTail()
//
//	Compares the non-wildcard initial substring or tailing
//	substring of the two MapHalfs.
//

	int matchHead(MapHalf other) {
		// non-wildcard matching for map joins

		int coff = 0;
		int r;
		for (; coff < fixedLen && coff < other.fixedLen; ++coff) {
			if ((r = (mapChar[coff].c - other.mapChar[coff].c)) != 0) {
				return -r;
			}
		}

		return 0;
	}

	boolean matchTail(MapHalf other) {
		int mc1 = 0;
		int mc2 = 0;

		while (mc1 > mapTail && mc2 > other.mapTail) {
			if (mapChar[--mc1].c - other.mapChar[--mc2].c == 0) {
				return true;
			}
		}

		return false;
	}

//
// MapHalf::Expand() - expand result of Match using a pattern
//

	String expand(String from, MapParams params) {
		int mc = 0;
		int slot;

		//if( DEBUG_MATCH )
		//    p4debug.printf( "Expand %s\n", Text() );

		String output = "";

		for (; mapChar[mc].cc != cEOS; ++mc) {
			if (mapChar[mc].isWild()) {
				slot = mapChar[mc].paramNumber;
				int in = params.vector[slot].start;
				int end = params.vector[slot].end;
				//if( DEBUG_MATCH )
				//    p4debug.printf( "... %d %p to '%.*s'\n", slot,
				//            &params.vector[ slot ], end - in, in );

				output += from.substring(in, end);
			} else {
				output += mapChar[mc].c;
			}
		}

		//if( DEBUG_MATCH )
		//    p4debug.printf( "Expanded to %s\n", output.Text() );
		return output;
	}

//
// begin support for MapHalf::Join()
//

// Retry - state for retrying a wildcard after a mismatch
//
// If two patterns fail to match on literal characters, then
// any wildcards previously encountered in the patterns get
// 'retried'.  That is, they match one more character than
// they did before and then the whole matching process is
// reattempted.
//

	enum Backup {
		BACKUP_NONE(0),        // in forward match
		BACKUP_LHS(1),        // backing up LHS
		BACKUP_RHS(2);        // backing up RHS

		Backup(int i) {
			code = i;
		}

		int code;
	}

	;

	private class Retry {
		int mc1;
		int mc2;
		MapParam param;
		Backup backup;
		int wilds;
	}

	;

// ActionGrid - control comparison of pattern characters
//
// While trying to join two patterns to make a third, we must compare
// the current character in each pattern and decide what to do.  This
// grid makes that decision.  There are five potential outcomes:
//
//	aMATCH		The characters match, so just advance to the
//			next character in both patterns.
//
//	aLWILD,R	The left (right) pattern is at a wildcard; save the
//			current state (for retry) and advance past
//			the wildcard.
//
//	aLBACK,R	Match failed, and retrying left (right) wildcard;
//			we consume a character on the right (copying it
//			into the param buf that holds the data matched
//			by the wildcard), save the state (for further
//			retry), and advance past the wildcard.
//
//	aLSTAR,R	Match failed, and retrying left (right) wildcard,
//			but now there is a wildcard on the other side; we
//			produce a wildcard into the param buf, save the
//			current state (for further retry), and start a new
//			retry for the other side.
//
//	aMISS		The characters are different, so back up the
//			retry list (setting "backup") and try again.
//
//	aOK		At the end of both patterns.  Return.
//

	enum Action {
		aMATCH(0, "MATCH"),
		aLWILD(1, "LHS-WILD"),
		aLBACK(2, "LHS-NEXT"),
		aRWILD(3, "RHS-WILD"),
		aRBACK(4, "RHS-NEXT"),
		aSTAR(5, "BOTH-STAR"),
		aLSTAR(6, "LHS-STAR"),
		aRSTAR(7, "RHS-STAR"),
		aMISS(8, "MISS"),
		aOK(9, "OK");

		Action(int i, String name) {
			code = i;
			this.name = name;
		}

		int code;
		String name;
	}

	static final Action Grid[][][] = new Action[][][]{
        /* Map \/ Pattern . */
        /*               EOS            CHAR           SLASH          PERC           STAR           DOTS */

        /* BACKUP_NONE */ {
            /* EOS */  {Action.aOK, Action.aMISS, Action.aMISS, Action.aRWILD, Action.aRWILD, Action.aRWILD},
            /* CHAR */ {Action.aMISS, Action.aMATCH, Action.aMISS, Action.aRWILD, Action.aRWILD, Action.aRWILD},
            /* SLASH */{Action.aMISS, Action.aMISS, Action.aMATCH, Action.aRWILD, Action.aRWILD, Action.aRWILD},
            /* PERC */ {Action.aLWILD, Action.aLWILD, Action.aLWILD, Action.aSTAR, Action.aSTAR, Action.aSTAR},
            /* STAR */ {Action.aLWILD, Action.aLWILD, Action.aLWILD, Action.aSTAR, Action.aSTAR, Action.aSTAR},
            /* DOTS */ {Action.aLWILD, Action.aLWILD, Action.aLWILD, Action.aSTAR, Action.aSTAR, Action.aSTAR}
	},
        /* BACKUP_LHS */ {
            /* EOS */  {Action.aOK, Action.aMISS, Action.aMISS, Action.aMISS, Action.aMISS, Action.aMISS},
            /* CHAR */ {Action.aMISS, Action.aMATCH, Action.aMISS, Action.aRBACK, Action.aRBACK, Action.aRBACK},
            /* SLASH */{Action.aMISS, Action.aMISS, Action.aMATCH, Action.aMISS, Action.aMISS, Action.aRBACK},
            /* PERC */ {Action.aMISS, Action.aLBACK, Action.aMISS, Action.aLSTAR, Action.aLSTAR, Action.aLSTAR},
            /* STAR */ {Action.aMISS, Action.aLBACK, Action.aMISS, Action.aLSTAR, Action.aLSTAR, Action.aLSTAR},
            /* DOTS */ {Action.aMISS, Action.aLBACK, Action.aLBACK, Action.aLSTAR, Action.aLSTAR, Action.aLSTAR}
	},
        /* BACKUP_RHS */ {
            /* EOS */  {Action.aOK, Action.aMISS, Action.aMISS, Action.aMISS, Action.aMISS, Action.aMISS},
            /* CHAR */ {Action.aMISS, Action.aMATCH, Action.aMISS, Action.aRBACK, Action.aRBACK, Action.aRBACK},
            /* SLASH */{Action.aMISS, Action.aMISS, Action.aMATCH, Action.aMISS, Action.aMISS, Action.aRBACK},
            /* PERC */ {Action.aMISS, Action.aLBACK, Action.aMISS, Action.aRSTAR, Action.aRSTAR, Action.aRSTAR},
            /* STAR */ {Action.aMISS, Action.aLBACK, Action.aMISS, Action.aRSTAR, Action.aRSTAR, Action.aRSTAR},
            /* DOTS */ {Action.aMISS, Action.aLBACK, Action.aLBACK, Action.aRSTAR, Action.aRSTAR, Action.aRSTAR}
	}
	};

//
// MapHalf::Join() - join two MapHalfs together
//

	void join(MapHalf map2, Joiner joiner) {
		Retry retries[] = new Retry[32]; //XXX: core dump when overflowed!
		for (int i = 0; i < retries.length; i++) {
			retries[i] = new Retry();
		}
		int retry = 0;
		joiner.clear();
		Backup backup = Backup.BACKUP_NONE;
		AtomicInteger wilds = new AtomicInteger(0);
		int maxWildChars = 10; //p4tunable.Get( P4TUNE_MAP_MAXWILD );

		//if( DEBUG_JOINHALF )
		//    p4debug.printf( "--- '%s','%s' ----\n", Text(), map2.Text() );

		// Optimization: match the non-wild initial substrings first.
		// Do it backwards, to eliminate mismatches quickly.  This helps
		// with large maps and large protections tables.

		int nonWild = map2.fixedLen;
		if (fixedLen < nonWild)
			nonWild = fixedLen;

		int mc1 = nonWild;
		int mc2 = nonWild;

		while (mc1 > 0) {
			if (mapChar[--mc1].c != map2.mapChar[--mc2].c) {
				return;
			}
		}

		// Now on with the show

		mc1 = nonWild;
		mc2 = nonWild;

		for (; ; ) {
			Action a = Grid[backup.code][mapChar[mc1].cc.code][map2.mapChar[mc2].cc.code];

			// Change aMATCH to aMISS is the characters differ

			if (a == Action.aMATCH && !(mapChar[mc1].c == map2.mapChar[mc2].c)) {
				a = Action.aMISS;
			}

            /*if( DEBUG_JOINHALF )
            {
                MapChar *c;
                char x;

                x = backup == BACKUP_LHS ? '=' : '-';

                p4debug.printf( "(" );

                for( c = mapChar; c.cc != cEOS; ++c )
                {
                    if( c == mc1 ) p4debug.printf( "%c", x );
                    p4debug.printf( "%c", c.c );
                }
                if( c == mc1 ) p4debug.printf( "%c", x );

                p4debug.printf( ") (" );

                x = backup == BACKUP_RHS ? '=' : '-';

                for( c = map2.mapChar; c.cc != cEOS; ++c )
                {
                    if( c == mc2 ) p4debug.printf( "%c", x );
                    p4debug.printf( "%c", c.c );
                }
                if( c == mc2 ) p4debug.printf( "%c", x );

                p4debug.printf( ") %d. %s\n", retry - retrys, actionNames[ a ] );
            }*/

			// Act according to the relationship of the current
			// characters in each input pattern.

			backup = Backup.BACKUP_NONE;

			switch (a) {
				case aMATCH:
					// Both the same: copy current character to
					// result and advance both input patterns.
					++mc1;
					++mc2;
					break;

				case aLWILD:
					// starting wildcard on left; initially match no chars
					retries[retry].wilds = wilds.get();
					retries[retry].backup = Backup.BACKUP_LHS;
					retries[retry].param = joiner.params.vector[mapChar[mc1].paramNumber];
					retries[retry].param.start = joiner.length();
					retries[retry].param.end = joiner.length();
					retries[retry].mc1 = mc1++;
					retries[retry].mc2 = mc2;
					++retry;
					break;

				case aRWILD:
					// starting wildcard on right; initially match no chars
					retries[retry].wilds = wilds.get();
					retries[retry].backup = Backup.BACKUP_RHS;
					retries[retry].param = joiner.params2.vector[map2.mapChar[mc2].paramNumber];
					retries[retry].param.start = joiner.length();
					retries[retry].param.end = joiner.length();
					retries[retry].mc1 = mc1;
					retries[retry].mc2 = mc2++;
					++retry;
					break;

				case aLBACK:
					// retrying wildcard on left; consume a char on right
					joiner.extend(map2.mapChar[mc2++].c);
					retries[retry].param.end = joiner.length();
					retries[retry].mc2 = mc2;

					if (Grid[retries[retry].backup.code][mapChar[mc1].cc.code][map2.mapChar[mc2].cc.code] == Action.aLSTAR) {
						backup = retries[retry].backup;
					} else {
						retries[retry].mc1 = mc1++;
						++retry;
					}
					break;

				case aRBACK:
					// retrying wildcard on right; consume a char on left

					joiner.extend(mapChar[mc1++].c);
					retries[retry].param.end = joiner.length();
					retries[retry].mc1 = mc1;

					// If the next character is a wildcard, keep consuming
					// this avoids wildcards matching empties next to wildcards

					if (Grid[retries[retry].backup.code][mapChar[mc1].cc.code][map2.mapChar[mc2].cc.code] == Action.aRSTAR) {
						backup = retries[retry].backup;
					} else {
						retries[retry].mc2 = mc2++;
						++retry;
					}
					break;

				case aSTAR:
					// two wildcards at the same time
					// make a new wildcard

					retries[retry].param = joiner.params.vector[mapChar[mc1].paramNumber];
					retries[retry].param.start = joiner.length();
					retries[retry].backup = Backup.BACKUP_LHS;

					// fall thru

				case aLSTAR:
					// retrying wildcard on left consumes wildcard on right
					// make a new wildcard and start a RWILD

					retries[retry + 1].param = joiner.params2.vector[map2.mapChar[mc2].paramNumber];
					retries[retry + 1].param.start = joiner.length();

					joiner.data = mapChar[mc1].makeParam(joiner.data, map2.mapChar[mc2], wilds);

					retries[retry].param.end = joiner.length();
					retries[retry + 1].param.end = joiner.length();
					retries[retry].mc1 = mc1++;
					retries[retry + 1].mc1 = mc1;
					retries[retry + 1].mc2 = mc2++;
					retries[retry].mc2 = mc2;
					retries[retry + 1].backup = Backup.BACKUP_RHS;
					retries[retry].wilds = wilds.get();
					retries[retry + 1].wilds = wilds.get();
					retry += 2;
					break;

				case aRSTAR:
					// retrying wildcard on right consumes wildcard on left
					// make a new wildcard and start a LWILD

					retries[retry + 1].param = joiner.params.vector[mapChar[mc1].paramNumber];
					retries[retry + 1].param.start = joiner.length();

					joiner.data = mapChar[mc1].makeParam(joiner.data, map2.mapChar[mc2], wilds);

					retries[retry].param.end = joiner.length();
					retries[retry + 1].param.end = joiner.length();
					retries[retry + 1].mc1 = mc1++;
					retries[retry].mc1 = mc1;
					retries[retry].mc2 = mc2++;
					retries[retry + 1].mc2 = mc2;
					retries[retry + 1].backup = Backup.BACKUP_LHS;
					retries[retry].wilds = wilds.get();
					retries[retry + 1].wilds = wilds.get();
					retry += 2;
					break;

				case aOK:
					// Full match: save result.
					if (wilds.get() > maxWildChars) {
						joiner.badJoin = true;
						return;
					}

					joiner.insert();

					// We fall through and produce other valid matches
					// at this point.

				case aMISS:
					// A mismatch: back up and try again.
					// We try each retry.

					// If we've exhausted all retries, we've failed.

					if (--retry < 0) {
						return;
					}

					mc1 = retries[retry].mc1;
					mc2 = retries[retry].mc2;
					backup = retries[retry].backup;
					joiner.setLength(retries[retry].param.end);
					wilds.set(retries[retry].wilds);

					// An ugly fix:
					//
					// In the *STAR cases, we pushed two retries on the retry
					// stack: one for the wildcard on the left and one for the
					// wildcard on the right.  Both retry's param pointers include
					// MakeParams() newly made output wildcard.  At this point,
					// the wildcard on the top of the stack matched nothing except
					// the other wildcard, and we must make the param pointer
					// reflect that.  However, it still includes characters after
					// the wildcard that eventually failed to match.  So we
					// back up our this retry.param.end to be the lower retry's
					// param.end (to include just the generated output wildcard).
					//
					// We only do this if there were at least two retries on the
					// stack, as we have no real way of checking if this is the
					// result of a *STAR case.  But it is harmless if not (I
					// think).
					//
					// This whole module needs to be rewritten again.
					//

					if (retry > 0)
						retries[retry].param.end = retries[retry - 1].param.end;

					break;
			}

            /*if( DEBUG_JOINHALF )
            {
                int i;
                int j;

                for( i = 0; retrys + i < retry; i++ )
                {
                    MapParam *p = retrys[i].param;
                    p4debug.printf( "\t\t\t\t%p ", p );
                    for( j = 0; j < p.start; j++ ) p4debug.printf( " " );
                    p4debug.printf( "\"" );
                    for( j = p.start; j < p.end; j++ ) p4debug.printf( "%c", joiner[j] );
                    p4debug.printf( "\"\n" );
                }
                p4debug.printf( "\t\t\t\t%p  ", joiner.Text() );
                for( j = 0; j < joiner.Length(); j++ ) p4debug.printf( "*" );
                p4debug.printf( "\n" );
            }*/
		}
	}

	int hasPosWild(String h) {
		int pos = h.indexOf("%%");
		if (pos != -1 && h.charAt(pos + 2) >= '0' && h.charAt(pos + 2) <= '9') {
			return 1;
		}
		return 0;
	}

//
//  MapHalf::HasEmbWild( StrPtr, int )
//
//  Returns 1 should StrPtr contain chars following a wildcard such as
//  ellipsis, positional, or asterisk.  The routine checks for leading wilds
//  not just embedded wilds.  Invoked with int set to 1, we were called with
//  an Ignored stream-path; the one path type allowing for leading ellipsis.
//

	int hasEmbWild(String h, boolean ignore) {
		int prevwild = -1;    // reference to previous char fetched

		for (int pos = 0; h.charAt(pos) != '\0'; pos++) {
			if (h.charAt(pos) == '.' && h.charAt(pos + 1) == '.' && h.charAt(pos + 2) == '.') {
				prevwild++;
				pos += 2;
			} else if (h.charAt(pos) == '%' && h.charAt(pos + 1) == '%' && h.charAt(pos + 2) >= '0' && h.charAt(pos + 2) <= '9') {
				prevwild++;
				pos += 2;
			} else if (h.charAt(pos) == '*') {
				prevwild++;
			} else if (h.charAt(pos) == '\0') {
				break;
			} else    // alphanumeric,slash,dot,percent: non-wild path chars
			{
				// for job050063 - check for no further paths, ellipsis, wilds,
				// so we might assume remaining chars are a file-extension.

				if (h.indexOf('/', pos) == -1 &&
						h.indexOf('*', pos) == -1 &&
						h.indexOf("...", pos) == -1) {
                    /*if( p4debug.GetLevel( DT_VIEWGEN ) >= 1 )
                        p4debug.printf( "Stream Path embedded wild:[%s]\n", \
                                hPtr );*/
					break;
				}

				if ((prevwild != -1 && !ignore) || (ignore && prevwild > 1))
					return 1;
			}
		}

		return 0;
	}

	int getFixedLen() {
		return fixedLen;
	}

	boolean isWild() {
		return isWild;
	}

	int wildcardCount() {
		return nWilds;
	}

	boolean match(String i, MapParams p) {
		MapParam o = new MapParam();
		return match1(i, 0) == 0 && match2(i, p);
	}
}
