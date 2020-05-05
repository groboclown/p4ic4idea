package com.perforce.p4java.mapapi;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static com.perforce.p4java.mapapi.MapFlag.MfAndmap;
import static com.perforce.p4java.mapapi.MapFlag.MfHavemap;
import static com.perforce.p4java.mapapi.MapFlag.MfRemap;
import static com.perforce.p4java.mapapi.MapFlag.MfUnmap;
import static com.perforce.p4java.mapapi.MapTableT.LHS;
import static com.perforce.p4java.mapapi.MapTableT.RHS;

public class MapTable {

	public int count = 0;
	public MapItem entry = null;
	public boolean hasMaps = false;
	public boolean hasOverlays = false;
	public boolean hasHavemaps = false;
	public boolean hasAndmaps = false;
	public String emptyReason = "";
	public boolean joinError = false;
	public int caseMode = -1;

	public MapTree trees[] = new MapTree[]{new MapTree(), new MapTree()};

	//
	// CHARHASH - see diff sequencer for comments
	//
	private static long CHARHASH(long h, int c) {
		return (293 * h) + c;
	}

	public boolean isEmpty() {
		return !hasMaps;
	}

	public boolean joinError() {
		return joinError;
	}

	public boolean hasOverlays() {
		return hasOverlays;
	}

	public boolean hasHavemaps() {
		return hasHavemaps;
	}

	public boolean hasAndmaps() {
		return hasAndmaps;
	}

	public MapTable set(MapTable f) {
		if (this == f)
			return this;

		clear();
		insert(f, true, false);

		return this;
	}

	public void clear() {
		count = 0;
		entry = null;
		hasMaps = false;
		hasOverlays = false;
		hasHavemaps = false;
		hasAndmaps = false;

		trees[LHS.dir].clear();
		trees[RHS.dir].clear();
	}

	public void setCaseSensitivity(int mode) {
		if (mode != 0 && mode != 1)
			return;

		caseMode = mode;

		for (MapItem map = entry; map != null; map = map.next()) {
			map.half(LHS).setCaseMode(mode);
			map.half(RHS).setCaseMode(mode);
		}
	}

	public void reverse() {
		if (entry != null) {
			entry = entry.reverse();
		}
	}

	public void insert(MapTable table, boolean fwd, boolean rev) {
		MapItem map;

		for (map = table.entry; map != null; map = map.next()) {
			if (fwd) insert(map.lhs().get(), map.rhs().get(), map.flag());
			if (rev) insert(map.rhs().get(), map.lhs().get(), map.flag());
		}

		reverse();
	}

	public void insert(String lhs, String rhs, MapFlag mapFlag) {
		entry = new MapItem(entry, lhs, rhs, mapFlag, count++, caseMode);

		// For IsEmpty(), HasOverlays() and HasHavemaps()

		if (mapFlag != MfUnmap)
			hasMaps = true;

		if (mapFlag == MfRemap || mapFlag == MfHavemap)
			hasOverlays = true;

		if (mapFlag == MfHavemap)
			hasHavemaps = true;

		if (mapFlag == MfAndmap)
			hasAndmaps = true;

		trees[LHS.dir].clear();
		trees[RHS.dir].clear();
	}

	public void insert(String lhs, int slot, String rhs, MapFlag mapFlag) {
		insert(lhs, rhs, mapFlag);
		entry = entry.move(slot);
	}

	public void insertNoDups(String lhs, String rhs, MapFlag mapFlag) {
		// Try to suppress duplicate mappings.

		// Would be nice if insert() took MapHalfs, rather
		// than starting with the StrPtr again.

		MapHalf hLhs = new MapHalf(lhs);
		MapHalf hRhs = new MapHalf(rhs);

		// We only look back so far, because big maps cost a
		// lot to generate, as well.

		int max = 8;

		for (MapItem map = entry; map != null && max-- != 0; map = map.next()) {
			if (mapFlag == MfRemap || map.mapFlag == MfRemap ||
					mapFlag == MfHavemap || map.mapFlag == MfHavemap) {
				// Remap and havemaps are additive, so we can
				// only eliminate literal duplicates.

				if (map.lhs().get().equals(lhs) && map.rhs().get().equals(rhs)) {
					return;
				}
			} else {
				// Regular maps have precedence, and so earlier map entries
				// mask later additions.  We used to just check for duplicates,
				// but MapHalf::Match( MapHalf ) allows us to check for
				// superceding maps.  This limits wildcard expansion of maps
				// significantly.

				if (map.lhs().match(hLhs) && map.rhs().match(hRhs))
					return;
			}
		}

		insert(lhs, rhs, mapFlag);
	}

	public void remove(int slotNum) {
		int slot = count - slotNum - 1;

		// Validate first
		if (slot < 0 || slot > entry.slot)
			return;

		MapItem target, prev = null;
		target = entry;

		// Reduce the slot number until we hit our target
		while (target.slot > slot) {
			target.slot--;
			prev = target;
			target = target.chain;
		}

		// Rewire the chain (if no previous we're replacing the root)
		if (prev != null)
			prev.chain = target.chain;
		else
			entry = target.chain;

		// Free the memory
		count--;

		// Clean the cache
		trees[LHS.dir].clear();
		trees[RHS.dir].clear();
	}

	public void validate(String lhs, String rhs) throws Exception {
		MapHalf l = new MapHalf();
		MapHalf r = new MapHalf();

		l.set(lhs);
		r.set(rhs);

		l.validate(r);
	}

	public void validHalf(MapTableT dir) throws Exception {
		MapItem map;

		for (map = entry; map != null; map = map.next())
			map.ths(dir).validate(null);
	}

	long getHash() {
		long h = 0;
		MapItem map;
		String c;
		int i;

		for (map = entry; map != null; map = map.next()) {
			c = map.lhs().get();
			for (i = 0; i < map.lhs().get().length(); ++i) {
				h = CHARHASH(h, c.charAt(i));
			}
			c = map.rhs().get();
			for (i = 0; i < map.rhs().get().length(); ++i) {
				h = CHARHASH(h, c.charAt(i));
			}
			h = CHARHASH(h, map.flag().code);
		}

		return h;
	}

	public void dump(StringBuffer buf, String trace, int fmt) {
		MapItem map;

		String out = String.format("map %s: %d items, joinError %s, emptyReason %s\n",
				trace, count, joinError ? "true" : "false",
				(emptyReason == null ? "NULL" : emptyReason));
		if (buf == null)
			System.out.print(out);
		else
			buf.append(out);

		if (fmt == 0) {
			// dump in precedence order (most significant first)
			for (map = entry; map != null; map = map.next()) {
				out = String.format("\t%c %s -> %s\n",
						" -+$@&    123456789".charAt(map.flag().code),
						map.lhs().get(),
						map.rhs().get());
				if (buf == null)
					System.out.print(out);
				else
					buf.append(out);
			}
		} else {
			// dump in the order of a client view
			for (int i = count - 1; i >= 0; i--) {
				out = String.format("\t%c %s . %s\n",
						" -+$@&    123456789".charAt(getFlag(get(i)).code),
						get(i).lhs().get(),
						get(i).rhs().get());
				if (buf == null)
					System.out.print(out);
				else
					buf.append(out);
			}
		}
	}

	public void dumpTree(StringBuffer buf, MapTableT dir, String trace) {
		if (trees[dir.dir].tree == null)
			makeTree(dir);
		if (trees[dir.dir].tree != null)
			trees[dir.dir].tree.dump(buf, dir, trace);
	}

	public boolean isSingle() {
		// There may be more than one valid mapping: we only look at the
		// highest precendence one, and it must have no widcards.
		// In theory, the check of the rhs is redundant: views are supposed
		// to have matching wildcards.  In theory, there is no difference
		// between theory and practice.  But in practice there is.

		return count >= 1 && !entry.lhs().isWild() && !entry.rhs().isWild();
	}

	//
	// Sort for MakeTree, Strings
	//
	// Sort produces a MapItem * array, with the MapItem *'s in sorted order.
	//
	// Put higher precedent maps first -- this helps MapItem::Match().
	//

	public class compareLHS implements Comparator<MapItem> {
		@Override
		public int compare(MapItem e1, MapItem e2) {
			int r = e1.lhs().compare(e2.lhs());
			return r != 0 ? r : e2.slot() - e1.slot();
		}
	}

	public class compareRHS implements Comparator<MapItem> {
		@Override
		public int compare(MapItem e1, MapItem e2) {
			int r = e1.rhs().compare(e2.rhs());
			return r != 0 ? r : e2.slot() - e1.slot();
		}
	}

	public class compareStreamLHS implements Comparator<MapItem> {
		@Override
		public int compare(MapItem e1, MapItem e2) {
			char c1, c2;
			String str1 = e1.lhs().get();
			String str2 = e2.lhs().get();

			int i = 0;
			int j = 0;

			// skip type values on the start of the RHS
			if ((c1 = str1.charAt(0)) == '%' || Character.isDigit(c1)) {
				while (str1.charAt(i) != 0 && (c1 = str1.charAt(i)) != '/')
					i++;
			}
			if ((c2 = str2.charAt(0)) == '%' || Character.isDigit(c2)) {
				while (str2.charAt(j) != 0 && (c2 = str2.charAt(j)) != '/')
					j++;
			}

			for (; i < str1.length() && (c1 = str1.charAt(i)) != 0 && j < str2.length() && (c2 = str2.charAt(j)) != 0; i++, j++) {
				if (c1 == c2)
					continue;

				if (str1.substring(i).startsWith("..."))
					return (-1);

				if (str2.substring(j).startsWith("..."))
					return (1);

				if (c1 == '*')
					return (-1);

				if (c2 == '*')
					return (1);

				if (c1 == '/')
					return (1);

				if (c2 == '/')
					return (-1);

				// starting with 2013.1, this becomes non-default behavior.

                /*if (p4tunable.Get(P4TUNE_STREAMVIEW_DOTS_LOW)) {
                    // make '.' lower than anything else
                    if (c1 == '.')
                        return (1);

                    if (c2 == '.')
                        return (-1);
                }*/

				return (c1 - c2);
			}

			return e1.slot() - e2.slot();
		}
	}

	public class compareStreamRHS implements Comparator<MapItem> {
		@Override
		public int compare(MapItem e1, MapItem e2) {
			char c1, c2;
			String str1 = e1.rhs().get();
			String str2 = e2.rhs().get();

			int i = 0;
			int j = 0;

			// skip type values on the start of the RHS
			if ((c1 = str1.charAt(0)) == '%' || Character.isDigit(c1)) {
				while (str1.charAt(i) != 0 && (c1 = str1.charAt(i)) != '/')
					i++;
			}
			if ((c2 = str2.charAt(0)) == '%' || Character.isDigit(c2)) {
				while (str2.charAt(j) != 0 && (c2 = str2.charAt(j)) != '/')
					j++;
			}

			for (; (c1 = str1.charAt(i)) != 0 && (c2 = str2.charAt(j)) != 0; i++, j++) {
				if (c1 == c2)
					continue;

				if (str1.substring(i).startsWith("..."))
					return (-1);

				if (str2.substring(j).startsWith("..."))
					return (1);

				if (c1 == '*')
					return (-1);

				if (c2 == '*')
					return (1);

				if (c1 == '/')
					return (1);

				if (c2 == '/')
					return (-1);

				// starting with 2013.1, this becomes non-default behavior.

                /*if (p4tunable.Get(P4TUNE_STREAMVIEW_DOTS_LOW)) {
                    // make '.' lower than anything else
                    if (c1 == '.')
                        return (1);

                    if (c2 == '.')
                        return (-1);
                }*/

				return (c1 - c2);
			}

			return e1.slot() - e2.slot();
		}
	}

	public ArrayList<MapItem> sort(MapTableT direction) {
		return sort(direction, false);
	}

	public ArrayList<MapItem> sort(MapTableT direction, boolean streamFlag) {
		// Both Strings and MakeTree want this.

        /*
         * We cache only non-stream sort results.
         * Stream sort calls happen at most once per MapTable instance
         * (in the td tests, so probably also in real life).
         * Incidentally, td measured an average of 1.45 non-stream sort calls
         * per instance, so not a huge win there either.
         * No instances received both stream and non-stream sort calls,
         * but we check just in case the usage changes in the future.
         */
		if (!streamFlag && trees[direction.dir].sort != null)
			return trees[direction.dir].sort;

		// Create sort tree

		ArrayList<MapItem> vec = new ArrayList<>();
		MapItem map = entry;

		for (; map != null; map = map.next()) {
			vec.add(map);
		}

		if (streamFlag) {
			if (direction == LHS) {
				java.util.Collections.sort(vec, new compareStreamLHS());
			} else {
				java.util.Collections.sort(vec, new compareStreamRHS());
			}
			return vec;
		} else {
			if (direction == LHS) {
				java.util.Collections.sort(vec, new compareLHS());
			} else {
				java.util.Collections.sort(vec, new compareRHS());
			}

			// save it
			return trees[direction.dir].sort = vec;
		}
	}

	//
	// Map tree construction
	//
	void makeTree(MapTableT dir) {
		AtomicInteger depth = new AtomicInteger(0);

		ArrayList<MapItem> vec = sort(dir);

		trees[dir.dir].tree = MapItem.tree(vec, 0, vec.size(), dir, null, depth);
		trees[dir.dir].depth = depth.get();
	}

	//
	// MapTable::Better - which table is better for matching?
	//
	boolean better(MapTable other, MapTableT dir) {
		// If we couldn't make a better map because of
		// wildcard explosion, this isn't better than the other.

        /*if( emptyReason == MsgDb::TooWild )
            return 0;*/

		// Compute the search trees, so we can compare depth.

		if (trees[dir.dir].tree == null)
			makeTree(dir);

		if (other.trees[dir.dir].tree == null)
			other.makeTree(dir);

		// shallower depth generally means faster matching

		return trees[dir.dir].depth < other.trees[dir.dir].depth;
	}

	//
	// MapStrings construction
	//
	public MapStrings strings(MapTableT direction) {
		List<MapItem> vec = sort(direction);
		MapStrings strings = new MapStrings();
		MapHalf oMapHalf = null;
		boolean oHasSubDirs = false;

		for (int i = 0; i < count; i++) {
			// If this is an unmapping, we're not going to get any
			// satisfaction looking for strings.  Just skip it.

			if (vec.get(i).flag() == MfUnmap)
				continue;

			// Find out how much of MapHalf is fixed (non wildcard)
			// and how much of that matches the saved MapHalf.
			// Note that match <= fixedLen, because we only match
			// the fixed portion.

			MapHalf mapHalf = vec.get(i).ths(direction);

			if (oMapHalf != null) {
				int match = oMapHalf.getCommonLen(mapHalf);

                /*if( DEBUG_STRINGS )
                    p4debug.printf( "MapStrings: %s match %d fixed %d\n",
                            mapHalf.Text(), match,
                            mapHalf.GetFixedLen() );*/

				// If this MapHalf matched the whole fixed part of the
				// saved MapHalf (GetCommonLen() can match no more),
				// then this MapHalf's is a substring of the saved,
				// and so won't needs its own string.

				// But: if this MapHalf has subdirectories, we'll have
				// to mark the saved MapHalf's string as having them.

				if (match == oMapHalf.getFixedLen()) {
					oHasSubDirs |= mapHalf.hasSubDirs(match);
					continue;
				}

				// Output old string if new is not substring.
				// Then continue with valid part of new

				if (mapHalf.getFixedLen() > match)
					strings.add(oMapHalf, oHasSubDirs);
			}

			oMapHalf = mapHalf;
			oHasSubDirs = mapHalf.hasSubDirs(mapHalf.getFixedLen());
		}

		// We've held onto oMapHalf for the possibility that a new
		// mapHalf would be an initial substring.  Now that there are
		// no more mapHalfs, output the oMapHalf.

		if (oMapHalf != null)
			strings.add(oMapHalf, oHasSubDirs);

        /*if( DEBUG_STRINGS )
            strings.Dump();*/

		return strings;
	}

	//
	// MapTable::Check() - see if lhs matches map
	//
	public MapItem check(MapTableT dir, String from) {
		if (trees[dir.dir].tree == null)
			makeTree(dir);

		return trees[dir.dir].tree != null ? trees[dir.dir].tree.match(dir, from, null) : null;
	}

	//
	// MapTable::Translate() - map an lhs into an rhs
	//
	public MapWrap translate(MapTableT dir, String from) {
		MapWrap out = null;
		Error e;
		if (trees[dir.dir].tree == null)
			makeTree(dir);

		MapItem map = trees[dir.dir].tree != null
				? trees[dir.dir].tree.match(dir, from, null)
				: null;

		// Expand into target string.
		// We have to Match2 here, because the last Match2 done in
		// MapItem::Match may not have been the last to succeed.

		if (map != null) {
			out = new MapWrap();
			out.setMap(map);
			MapParams params = new MapParams();

			map.ths(dir).match2(from, params);
			out.setTo(map.ohs(dir).expand(from, params));

            /*if( DEBUG_TRANS )
                p4debug.printf( "MapTrans: %s (%d) . %s\n",
                        from.Text(), map.Slot(), to.Text() );*/
		}

		return out;
	}

	//
	// MapTable::Explode() - map an lhs into one or more rhs's
	//
	public MapItemArray explode(MapTableT dir, String from) {

		MapItemArray maps = new MapItemArray();
		Error e;

		if (trees[dir.dir].tree == null)
			makeTree(dir);

		MapItemArray ands = new MapItemArray();
		MapItem map;

		if (trees[dir.dir].tree != null)
			trees[dir.dir].tree.match(dir, from, ands);

		// Expand into target string.
		// We have to Match2 here, because the last Match2 done in
		// MapItem::Match may not have been the last to succeed.

		int i = 0;
		int nonand = 0;
		String to;
		while ((map = ands.getItem(i++)) != null) {
			MapParams params = new MapParams();
			if (!map.ths(dir).match2(from, params) ||
					map.flag() == MfUnmap)
				return maps;

			if (map.flag() != MfAndmap && nonand != 0)
				continue;

			nonand++;

			to = map.ohs(dir).expand(from, params);

            /*if( DEBUG_TRANS )
                p4debug.printf( "MapTrans: %s (%d) . %s\n",
                        from.Text(), map.Slot(), to.Text() );*/

			maps.put(map, to);
		}

		return maps;
	}

	//
	// MapTable::Match() - just match pattern against string
	//
	public boolean match(MapHalf l, String rhs) {
		MapParams params = new MapParams();
		return l.match(rhs, params);
	}

	//
	// MapTable::Match() - just match pattern against string
	//
	public boolean match(String lhs, String rhs) {
		MapHalf l = new MapHalf(lhs);
		MapParams params = new MapParams();
		return l.match(rhs, params);
	}

	//
	// MapTable::ValidDepotMap() - return 1 if map is a valid depot map entry
	//
	public boolean validDepotMap(String map) {
		MapHalf l = new MapHalf(map);

		// Valid depot map has only one wildcard, and it must
		// be a trailing wildcard of the form '/...'

		return l.wildcardCount() == 1 && l.hasEndSlashEllipses();
	}

	//
	// MapTable::Get() - get a MapTable entry
	//
	public MapItem get(int n) {
		MapItem map;

		for (map = entry; map != null; map = map.next()) {
			if (n == 0) {
				return map;
			}
			n--;
		}

		return null;
	}

	//
	// MapItem accessors
	//

	public int getSlot(MapItem m) {
		return count - m.slot() - 1;
	}

	public MapFlag getFlag(MapItem m) {
		return m.flag();
	}

	public MapItem getNext(MapItem m) {
		return m.next();
	}

	public String getStr(MapItem m, MapTableT dir) {
		return m.ths(dir).get();
	}

	public String translate(
			MapItem m,
			MapTableT dir,
			String from) {
		MapParams params = new MapParams();

		if (m.flag() == MfUnmap) {
			return null;
		}

		if (!m.ths(dir).match(from, params)) {
			return null;
		}

		// Expand into target string.

		return m.ohs(dir).expand(from, params);
	}

	/*
	 * MapTable::StripMap() - return copy without mapFlag entries.
	 */
	public MapTable stripMap(MapFlag mapFlag) {
		MapTable m0 = new MapTable();
		MapItem map;

		for (map = entry; map != null; map = map.next()) {
			if (map.flag() != mapFlag) {
				m0.insert(map.lhs().get(), map.rhs().get(), map.flag());
			}
		}

		m0.reverse();

		return m0;
	}

	/*
	 * MapTable::Swap() - return copy with LHS and RHS swapped for each MapItem.
	 */
	public MapTable swap(MapTable table) {
		MapTable m0 = new MapTable();
		MapItem map;

		for (map = entry; map != null; map = map.next())
			m0.insert(map.rhs().get(), map.lhs().get(), map.flag());

		m0.reverse();

		return m0;
	}

	public int countByFlag(MapFlag mapFlag) {
		int result = 0;
		MapItem map;

		for (map = entry; map != null; map = map.next())
			result += (map.flag() == mapFlag) ? 1 : 0;

		return result;
	}

	public void insertByPattern(
			String lhs,
			String rhs,
			MapFlag mapFlag) {
		int l = lhs.length() - 1;
		int r = rhs.length() - 1;
		int ls = 0;
		int rs = 0;
		int slashes;

		// Insist on starting after //xxx/

		for (slashes = 0; slashes < 3 && ls < l; ++ls) {
			slashes += lhs.charAt(ls) == '/' ? 1 : 0;
		}

		for (slashes = 0; slashes < 3 && rs < r; ++rs) {
			slashes += rhs.charAt(rs) == '/' ? 1 : 0;
		}

		// Find matching ending substring

		slashes = 0;

		while (l > ls && r > rs && lhs.charAt(l - 1) == rhs.charAt(r - 1)) {
			--l;
			--r;
			slashes += lhs.charAt(l) == '/' ? 1 : 0;
		}

		// Don't strip off the last mismatching /

		if (l < (lhs.length() - 1) && lhs.charAt(l) == '/') {
			++l;
			++r;
			--slashes;
		}

		// Don't strip off trailing . if we are adding ...

		if (((l < (lhs.length() - 1) && lhs.charAt(l - 1) == '.') ||
				(r < (rhs.length() - 1) && rhs.charAt(r - 1) == '.')) && slashes != 0) {
			++l;
			++r;
		}

		// Replace end with * or ...
		// And put it on the map

		if (slashes != 0 && l < lhs.length() - 4) {
			String left = "";
			left += lhs.substring(0, l);
			left += "...";

			String right = "";
			right += rhs.substring(0, r);
			right += "...";

			insertNoDups(left, right, mapFlag);
		} else if (slashes == 0 && l < lhs.length() - 2) {
			String left = "";
			left += lhs.substring(0, l);
			left += "*";

			String right = "";
			right += rhs.substring(0, r);
			right += "*";

			insertNoDups(left, right, mapFlag);
		} else
			insertNoDups(lhs, rhs, mapFlag);
	}

	/**
	 * MapTable::Disambiguate() - handle un/ambiguous mappings
	 * <p>
	 * Mappings provided by the user (client views, branch views)
	 * can be ambiguous since later mappings can override earlier
	 * ones.  Disambiguate() adds explicit exclusion (-) mappings
	 * for any ambiguous mappings, so that Join() and Translate()
	 * don't have to worry about them.
	 * <p>
	 * For example:
	 * <p>
	 * a/... b/...
	 * a/c b/d
	 * <p>
	 * becomes
	 * <p>
	 * a/... b/...
	 * -a/c -b/c
	 * -a/d -b/d
	 * a/c b/d
	 * <p>
	 * Unmaps are handled similarly, but the high precedence unmap
	 * line itself is not added, because everything that can be unmapped
	 * has been done so by joining it against the lower precedence
	 * mapping lines:
	 * <p>
	 * a/... b/...
	 * -a/c b/d
	 * <p>
	 * becomes
	 * <p>
	 * a/... b/...
	 * -a/c -b/c
	 * -a/d -b/d
	 */
	public void disambiguate() {
		MapDisambiguate j = new MapDisambiguate();

		// From high precendence to low precedence

		for (j.map = entry; j.map != null; j.map = j.map.next()) {
			// We skip unmap lines, because we only need to
			// unmap to the extent that the unmap lines match lower
			// precedence map lines.  We do that below.

			switch (j.map.flag()) {
				case MfUnmap:
					continue;
			}

			// Look for higher precedence mappings that match (join)
			// this mapping, and to the extent that they overlap add
			// unmappings.  We do this for both MfMap and MfUnmap.
			// A higher precedence MfRemap doesn't occlude us, so
			// we skip those.

			// From higher precedence back down to this mapping

			for (j.map2 = entry;
			     j.map2 != j.map;
			     j.map2 = j.map2.next()) {
				switch (j.map2.flag()) {
					case MfRemap:
					case MfHavemap:
						break;

					case MfAndmap:
						j.map2.lhs().join(j.map2.rhs(), j);
						j.map2.rhs().join(j.map.rhs(), j);
						break;

					default:
						j.map2.lhs().join(j.map.lhs(), j);
						j.map2.rhs().join(j.map.rhs(), j);
				}
			}

			// now the original map entry

			j.m0.insert(j.map.lhs().get(), j.map.rhs().get(), j.map.flag());
		}

		// Inserted order leaves low precedence at the head of the
		// list.  Reverse to get it back where it belongs.

		j.m0.reverse();

		// Just zonk our own and copy j.m0's

		clear();
		insert(j.m0, true, false);
	}

	/*
	 * MapTable::JoinCheck() - does this maptable include this string?
	 *
	 * This isn't simply finding if a file is mapped by the table, but
	 * rather if a _mapping_ is mapped by the table.  So we have to take
	 * the mapping, put it into its own table, join it, and then throw
	 * it all away.  Kinda _slow_.
	 */
	public boolean joinCheck(MapTableT dir, String lhs) {
		MapTable c = new MapTable();

		c.insert(lhs, null, MapFlag.MfMap);
		MapTable j = c.join(LHS, this, dir);
		return !j.isEmpty();
	}

	public boolean joinCheck(MapTableT dir, MapTable c, MapTableT dir2) {
		MapTable j = c.join(dir2, this, dir);
		return !j.isEmpty();
	}

	public void joinOptimizer(MapTableT dir2) {
		if (trees[dir2.dir].tree == null)
			makeTree(dir2);
	}

	public void join(
			MapTable m1, MapTableT dir1,
			MapTable m2, MapTableT dir2,
			MapJoiner j, String reason) {
		/*if( DEBUG_JOIN )
        {
            m1.Dump( dir1 == LHS ? "lhs" : "rhs" );
            m2.Dump( dir2 == LHS ? "lhs" : "rhs" );
        }*/

		if (m1.caseMode == 0 || m1.caseMode == 1)
			this.setCaseSensitivity(m1.caseMode);

		// Give up if we internally produce more than 1,000,000 rows
		// or more than 10000 + the sum of the two mappings.

		// This can happen when joining multiple lines with multiple
		// ...'s on them.

		final int max1 = 100000; //ToDo: p4tunable.Get( P4TUNE_MAP_JOINMAX1 );
		final int max2 = 100000; //ToDo: p4tunable.Get( P4TUNE_MAP_JOINMAX2 );

		int m = max1 + m1.count + m2.count;
		if (m > max2) m = max2;

		if (m2.trees[dir2.dir].tree == null) {
			for (j.map = m1.entry; j.map != null && count < m; j.map = j.map.next()) {
				for (j.map2 = m2.entry; j.map2 != null; j.map2 = j.map2.next()) {
					j.map.ths(dir1).join(j.map2.ths(dir2), j);
					if (j.badJoin) {
						joinError = true;
						emptyReason = "TooWild2"; //&MsgDb::TooWild2;
						this.joinError = true;
						this.emptyReason = "TooWild"; //&MsgDb::TooWild;
						return;
					}
				}
			}
		} else {
			// Generate a list of possible pairs of maps to join.
			// These are maps whose non-wild initial substrings match.
			// We used to just do for()for(), but when joining large
			// maps that's slow.  So the inner loop is now a tree
			// search.

			MapPairArray pairArray = new MapPairArray(dir1, dir2);

			if (m2.trees[dir2.dir].tree != null) {
				for (MapItem i1 = m1.entry; i1 != null && count < m; i1 = i1.next()) {
					// This inserts entries in tree order,
					// rather than chain order (precedence), so we have to
					// resort before doing the MapHalf::Join() calls.

					pairArray.clear();
					pairArray.match(i1, m2.trees[dir2.dir].tree);
					pairArray.sort();

					MapPair jp;

					// Walk the list of possible pairs and call MapHalf::Join()
					// to do the wildcard joining.

					for (int i = 0; i < pairArray.size(); i++) {
						jp = pairArray.get(i);
						j.map = jp.item1;
						j.map2 = jp.tree2;
						jp.h1.join(jp.h2, j);
					}
				}
			}
		}

		// insert() reverses the order

		this.reverse();

		// Empty reasons

		if (count >= m) {
			emptyReason = "TooWild"; //&MsgDb::TooWild;
			clear();
		} else if (m1.isEmpty() && m1.emptyReason != null) {
			emptyReason = m1.emptyReason;
		} else if (m2.isEmpty() && m2.emptyReason != null) {
			emptyReason = m2.emptyReason;
		} else if (!hasMaps && reason != null) {
			emptyReason = reason;
		}

		//if( DEBUG_JOIN )
		//    this.Dump( "map joined" );
	}

	public MapTable join(
			MapTableT dir1,
			MapTable m2,
			MapTableT dir2) {
		return join(dir1, m2, dir2, null);
	}

	public MapTable join(
			MapTableT dir1,
			MapTable m2,
			MapTableT dir2,
			String reason) {
		MapJoiner j = new MapJoiner();
		j.m0.join(this, dir1, m2, dir2, j, reason);
		return j.m0;
	}

	public MapTable join2(
			MapTableT dir1,
			MapTable m2,
			MapTableT dir2) {
		return join2(dir1, m2, dir2, null);
	}

	public MapTable join2(
			MapTableT dir1,
			MapTable m2,
			MapTableT dir2,
			String reason) {
		MapJoiner2 j = new MapJoiner2(dir1, dir2);
		j.m0.join(this, dir1, m2, dir2, j, reason);
		return j.m0;
	}
}
