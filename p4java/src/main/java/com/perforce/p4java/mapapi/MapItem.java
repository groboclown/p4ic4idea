package com.perforce.p4java.mapapi;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import static com.perforce.p4java.mapapi.MapFlag.MfAndmap;
import static com.perforce.p4java.mapapi.MapFlag.MfUnmap;

public class MapItem {

    /*
     * chain - linked list
     * mapFlag - represent +map, or -map
     * slot - precedence (higher is better) on the chain
     */

	public MapItem chain;
	public MapFlag mapFlag;
	public int slot;

	public class MapWhole {
		public MapHalf half = new MapHalf();

		public MapItem left;
		public MapItem center;
		public MapItem right;

		public int maxSlot;
		public int overlap;
		public boolean hasands;
		public int maxSlotNoAnds;
	}

	/**
	 * halves - MapHalf and trinary tree for each direction
	 * <p>
	 * Trinary tree?
	 * <p>
	 * left: 	less than this mapping
	 * right: 	greater than this mapping
	 * center:	included in this mapping
	 * <p>
	 * e.g. a center for //depot/... might be //depot/main/...
	 */
	public MapWhole halves[] = new MapWhole[]{new MapWhole(), new MapWhole()};

	public MapWhole whole(MapTableT dir) {
		return halves[dir.dir];
	}

	public MapHalf half(MapTableT dir) {
		return halves[dir.dir].half;
	}

	public boolean isParent(MapItem other, MapTableT dir) {
		return ths(dir).getFixedLen() ==
				ths(dir).getCommonLen(other.ths(dir));
	}

	/**
	 * MapItem -- mapping entries on a chain
	 * <p>
	 * A MapItem holds two MapHalfs that constitute a single entry in
	 * a MapTable.  MapItem also implement fast searching for entries
	 * for MapTable::Check() and MapTable::Translate().
	 */
	public MapItem(MapItem c, String l,
	               String r, MapFlag f, int s) {
		lhs().set(l);
		rhs().set(r);
		mapFlag = f;
		chain = c;
		slot = s;
		halves[0].left = null;
		halves[0].center = null;
		halves[0].right = null;
		halves[1].left = null;
		halves[1].center = null;
		halves[1].right = null;
	}

	/**
	 * MapItem -- mapping entries on a chain
	 * <p>
	 * A MapItem holds two MapHalfs that constitute a single entry in
	 * a MapTable.  MapItem also implement fast searching for entries
	 * for MapTable::Check() and MapTable::Translate().
	 */
	public MapItem(MapItem c, String l,
	               String r, MapFlag f, int s,
	               int caseMode) {
		lhs().set(l);
		rhs().set(r);
		mapFlag = f;
		chain = c;
		slot = s;
		halves[0].left = null;
		halves[0].center = null;
		halves[0].right = null;
		halves[1].left = null;
		halves[1].center = null;
		halves[1].right = null;
		if (caseMode == 0 || caseMode == 1) {
			halves[0].half.setCaseMode(caseMode);
			halves[1].half.setCaseMode(caseMode);
		}
	}

	public MapHalf lhs() {
		return half(MapTableT.LHS);
	}

	public MapHalf rhs() {
		return half(MapTableT.RHS);
	}

	public MapHalf ths(MapTableT dir) {
		return half(dir);
	}

	public MapHalf ohs(MapTableT dir) {
		return half(MapTableT.LHS == dir ? MapTableT.RHS : MapTableT.LHS);
	}

	public MapItem next() {
		return chain;
	}

	public MapFlag flag() {
		return mapFlag;
	}

	public int slot() {
		return slot;
	}

	/**
	 * MapItem::Reverse - reverse the chain, to swap precedence
	 */
	public MapItem reverse() {
		MapItem m = this;
		MapItem entry = null;
		int top = m != null ? m.slot : 0;

		while (m != null) {
			MapItem n = m.chain;
			m.chain = entry;
			m.slot = top - m.slot;
			entry = m;
			m = n;
		}

		return entry;
	}

	/**
	 * MapItem::Move - moves an item up the chain
	 */
	MapItem move(int slot) {
		MapItem m = this;
		MapItem entry = m.chain;

		int start = m.slot;

		// This has no error state, but this is bad
		// We can't go below 0 and we can't go back up either
		if (start <= slot)
			return m;

		if (slot < 0)
			slot = 0;

		MapItem n = m.chain;
		while (n != null) {
			if (n.slot != slot) {
				n.slot++;
				n = n.chain;
				continue;
			}

			n.slot++;
			m.slot = slot;
			m.chain = n.chain;
			n.chain = m;

			break;
		}

		return entry;
	}

	/**
	 * MapItem::Tree - recursively construct a trinary sort tree
	 */
	static MapItem tree(
			ArrayList<MapItem> items,
			int start,
			int end,
			MapTableT dir,
			MapItem parent,
			AtomicInteger depth) {
		/* No empties */

		if (items.size() == 0 || start == end)
			return null;

        /*
         * start li (middle) ri end
         *
         * (middle) is halfway between start and end.
         * li is first item that is a parent of middle.
         * ri is last item li is a parent of.
         *
         * We return
         *
         *	           *li
         *           /  |  \
         *          /   |   \
         *         /    |    \
         *        /     |     \
         * start.li   li+1.ri  ri.end
         */

		int li = start;
		int ri = -1;

        /*
         * Quick check: the center tree often ends up in the
         * shape of a linked list (due to identical entries).
         * This is an optimization for that case.
         */

		if (start == end - 1 || items.get(start).isParent(items.get(end - 1), dir)) {
			ri = end;

			int overlap = 0;
			AtomicInteger depthBelow = new AtomicInteger(0);
			int maxSlot = 0;
			boolean hasands = false;
			int maxSlotNoAnds = -1;
			int pLength = items.get(start).ths(dir).getFixedLen();

			int last = -1;
			MapItem.MapWhole t;

			while (--ri > start) {
				if (items.get(ri).ths(dir).getFixedLen() == pLength) {
					break;
				}
			}

			if (parent != null) {
				overlap = items.get(start).ths(dir).getCommonLen(parent.ths(dir));
			}
			if (ri < end - 1) {
				t = items.get(ri).whole(dir);

				t.overlap = overlap;
				t.maxSlot = items.get(ri).slot;
				t.right = t.left = null;
				t.hasands = false;
				t.maxSlotNoAnds = items.get(ri).flag() != MfAndmap ? items.get(ri).slot : -1;

				t.center = tree(items, ri + 1, end, dir, items.get(ri), depthBelow);

				if (maxSlot < t.maxSlot)
					maxSlot = t.maxSlot;

				if (maxSlotNoAnds < t.maxSlotNoAnds)
					maxSlotNoAnds = t.maxSlotNoAnds;

				if (t.hasands)
					hasands = true;

				if (parent != null && (items.get(ri).mapFlag == MfAndmap || t.hasands)) {
					parent.whole(dir).hasands = true;
				}

				last = ri--;
				depthBelow.incrementAndGet();
			}

			depthBelow.addAndGet(ri - start + 1);

			while (ri >= start) {
				t = items.get(ri).whole(dir);

				t.overlap = overlap;

				if (maxSlot < items.get(ri).slot) {
					maxSlot = items.get(ri).slot;
				}
				t.maxSlot = maxSlot;

				if (items.get(ri).flag() != MfAndmap && maxSlotNoAnds < items.get(ri).slot) {
					maxSlotNoAnds = items.get(ri).slot;
				}
				t.maxSlotNoAnds = maxSlotNoAnds;

				hasands = last != -1 && items.get(last).mapFlag == MfAndmap;
				t.hasands = hasands;

				t.right = t.left = null;
				t.center = last == -1 ? null : items.get(last);
				last = ri--;
			}

			if (parent != null && parent.whole(dir).maxSlot < maxSlot) {
				parent.whole(dir).maxSlot = maxSlot;
			}

			if (parent != null && parent.whole(dir).maxSlotNoAnds < maxSlotNoAnds) {
				parent.whole(dir).maxSlotNoAnds = maxSlotNoAnds;
			}

			if (parent != null && (hasands || (last != -1 && items.get(last).mapFlag == MfAndmap))) {
				parent.whole(dir).hasands = true;
			}

			if (depth.get() < depthBelow.get()) {
				depth.set(depthBelow.get());
			}

			return items.get(li);
		} else

        /*
         * Start in middle.
         * Move li from start until we find first parent of ri.
         * Move ri right until we find last child of li.
         */

			ri = start + (end - start) / 2;

		while (li < ri && !items.get(li).isParent(items.get(ri), dir)) {
			++li;
		}

		while (ri < end && items.get(li).isParent(items.get(ri), dir)) {
			++ri;
		}

        /*
         * Fill in the *li node, which we will return.
         *
         * left, right, center computed recursively.
         */

		MapItem.MapWhole t = items.get(li).whole(dir);

		AtomicInteger depthBelow = new AtomicInteger(0);

		t.overlap = 0;
		t.maxSlot = items.get(li).slot;
		t.hasands = false;
		t.maxSlotNoAnds = items.get(li).flag() != MfAndmap ? items.get(li).slot : -1;

		t.left = tree(items, start, li, dir, items.get(li), depthBelow);
		t.center = tree(items, li + 1, ri, dir, items.get(li), depthBelow);
		t.right = tree(items, ri, end, dir, items.get(li), depthBelow);

        /*
         * Current depth is 1 + what's below us, as long as one of
         * our peers isn't deeper.
         */

		if (depth.get() < depthBelow.get() + 1)
			depth.set(depthBelow.get() + 1);

        /*
         * Relationship to parent:
         * parent's maxSlot includes our maxSlot.
         * our initial substring overlap with our parent.
         */

		if (parent != null) {
			if (parent.whole(dir).maxSlot < t.maxSlot)
				parent.whole(dir).maxSlot = t.maxSlot;

			if (parent.whole(dir).maxSlotNoAnds < t.maxSlotNoAnds)
				parent.whole(dir).maxSlotNoAnds = t.maxSlotNoAnds;

			t.overlap = t.half.getCommonLen(parent.ths(dir));

			if (items.get(li).mapFlag == MfAndmap || t.hasands)
				parent.whole(dir).hasands = true;
		}

		return items.get(li);
	}

	/**
	 * MapItem::Match() - find best matching MapItem
	 * <p>
	 * We have a chain of MapItems, but instead use trinary tree constructed
	 * by MapItem::Tree for this direction.
	 * <p>
	 * This has three separate optimizations:
	 * <p>
	 * 1. The trinary tree itself (instead of a linear scan).  We use
	 * a trinary tree because we need to order mappings that are
	 * neither less than nor greater than others, but including them.
	 * e.g. //depot/main/... includes //depot/main/p4...  As we
	 * decend the tree, we use MapHalf::Match1 to compare the initial
	 * substring.  If it matches, we will use MapHalf::Match2 to
	 * do the wildcard comparison and then follow the center down.
	 * <p>
	 * 2. Slot precedence.  Once we've had a match, we only need to Match2
	 * nodes with better precedence (map.slot > best).  Further, once
	 * we've had a match, we can give up entirely if the tree below
	 * has no nodes with higher precedence (best > t.maxSlot).
	 * <p>
	 * 3. Overlap.  As we decend the tree, many of the strings have
	 * initial substrings in common.  So we remember the offset of
	 * the last matching character in the parent's MapHalf, adjust it
	 * down if needed so as not to exceed the operlap with this
	 * MapHalf, and start the match from there.
	 */
	MapItem match(MapTableT dir, String from, MapItemArray ands) {
		int coff = 0;
		int best = -1;
		int bestnotands = -1;
		MapItem map = null;
		MapItem tree = this;
		MapParams params = new MapParams();

		if (ands == null && (tree.whole(dir).hasands || tree.flag() == MfAndmap)) {
			ands = new MapItemArray();
		}

        /* Decend */

		while (tree != null) {
			MapItem.MapWhole t = tree.whole(dir);

            /*
             * No better precendence below?  Bail.
             * Unless we're looking for andmaps
             */

			if (best > t.maxSlot &&        // Have we already got the best?
					!t.hasands &&            // Are there andmaps down the tree?
					tree.flag() != MfAndmap && // This is an andmap?
					bestnotands > t.maxSlotNoAnds) // We prefer a real mapping
				break;

            /*
             * Match with prev map greater than overlap?  trim.
             */

			if (coff > t.overlap)
				coff = t.overlap;

            /*
             * Match initial substring (by which the tree is ordered).
             * Can skip match if same initial substring as previous map.
             */

			int r = 0;

			if (coff < t.half.getFixedLen())
				r = t.half.match1(from, coff);

            /*
             * Match?  Higher precedence?  Wildcard match?  Save.
             */

			if (r == 0 &&
					best < tree.slot &&
					t.half.match2(from, params)) {
				map = tree;
				best = map.slot;
				if (ands != null)
					ands.put(tree, null);
				if (tree.flag() != MfAndmap)
					bestnotands = tree.slot;
			}

            /*
             * Not higher precedence? AndMap Array? Wildcard match? Save
             */

			if (r == 0 &&
					ands != null &&
					map != tree &&
					best >= tree.slot &&
					t.half.match2(from, params)) {
				ands.put(tree, null);
				if (tree.flag() != MfAndmap)
					bestnotands = tree.slot;
			}

            /*
             * Follow to appropriate child.
             */

			if (r < 0) tree = t.left;
			else if (r > 0) tree = t.right;
			else tree = t.center;
		}

        /*
         * If we were dealing with & maps, we need to make sure we either:
         *   1. return the highest precedence non-andmap mapping
         *   2. return the highest precedence andmap mapping
         */

		if (map != null && ands != null) {
			MapItem m0 = null;
			int i = 0;
			while ((m0 = ands.getItem(i++)) != null)
				if (m0.flag() != MfAndmap) {
					if (m0.mapFlag == MfUnmap)
						break; // Take the best & mapping and break

					map = m0; // Take the best non-& mapping and break
					break;
				} else if (i == 1)
					map = m0; // Take the best & mapping; keep going
		}

        /*
         * Best mapping an unmapping?  That's no mapping.
         */

		if (map == null || map.mapFlag == MfUnmap)
			return null;

		return map;
	}

	/*
	 * MapItem::Dump() - dump tree, rooted at this
	 */
	void dump(StringBuffer buf, MapTableT d, String name) {
		dump(buf, d, name, 0);
	}

	void dump(StringBuffer buf, MapTableT d, String name, int l) {
		String indent = "";
		for (int i = 0; i < l && i < 8; i++) {
			indent += "\t";
		}

		if (l == 0) {
			if (buf == null)
				System.out.print("MapTree\n");
			else
				buf.append("MapTree\n");
		}

		if (whole(d).left != null) {
			whole(d).left.dump(buf, d, "<<<", l + 1);
		}

		String out = String.format("%s%s %c%s <-> %s%s (maxslot %d (%d))\n", indent, name,
				" -+$@&    123456789".charAt(mapFlag.code), ths(d).get(), ohs(d).get(),
				whole(d).hasands ? " (has &)" : "", whole(d).maxSlot, whole(d).maxSlotNoAnds);
		if (buf == null)
			System.out.print(out);
		else
			buf.append(out);

		if (whole(d).center != null) {
			whole(d).center.dump(buf, d, "===", l + 1);
		}

		if (whole(d).right != null) {
			whole(d).right.dump(buf, d, ">>>", l + 1);
		}
	}
}
