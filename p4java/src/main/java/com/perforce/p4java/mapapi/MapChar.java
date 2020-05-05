package com.perforce.p4java.mapapi;

import com.perforce.p4java.common.base.OSUtils;

import java.util.concurrent.atomic.AtomicInteger;

public class MapChar {
    /*
     * MapChar -- a MapHalf's character string
     *
     * Public Methods:
     *
     *	MapChar::Set() - initialize MapChar, given the string
     *	MapChar::Advance() - advance to the next lexical element
     *	MapChar::ParamNumber() - return wildcard slot of current element
     *			%x = x
     *			* = 10 + nStars
     *			... = 20 + nDots
     *	MapChar::MakeParam() - format a parameter element
     *	MapChar::Name() - dump out current element name for debugging
     *
     * Public attributes:
     *	p - pointer to current element
     *	cc - current element's type
     *	ccPre - previous element's type
     */

	static final int PARAM_BASE_PERCENT = 0;    // parameter slots for %x
	static final int PARAM_BASE_STARS = 10;    // parameter slots for *'s
	static final int PARAM_BASE_DOTS = 20;        // parameter slots for ...'s
	static final int PARAM_BASE_TOP = 23;        // last slot

	enum MapCharClass {
		cEOS(0, '\0', false),        // \0
		cCHAR(1, 'c', false),        // any char
		cSLASH(2, '/', false),    // /
		cPERC(3, '%', true),        // %x
		cSTAR(4, '*', true),        // *
		cDOTS(5, '.', true);        // ...

		MapCharClass(int i, char c, boolean wild) {
			chr = c;
			isWild = wild;
			code = i;
		}

		public char chr;
		public boolean isWild;
		public int code;

	}

	char c;            // current character
	int paramNumber;        // current ParamNumber
	MapCharClass cc;            // current char's type
	int caseMode;        // case handling

	int compare(char oc) {
		return caseMode == 0 || !OSUtils.isWindows() ? c - oc
				: String.valueOf(c).compareToIgnoreCase(String.valueOf(oc));
	}

	boolean isEqual(char oc) {
		return compare(oc) == 0;
	}

	public char name() {
		return cc.chr;
	}

	public boolean isWild() {
		return cc.isWild;
	}

	public boolean set(char[] p, AtomicInteger pos, AtomicInteger nStars, AtomicInteger nDots, int caseMode) {
		this.caseMode = caseMode;

		if (pos.get() == p.length || p[pos.get()] == '\0') {
			cc = MapCharClass.cEOS;
			this.c = '\0';
			return false;
		}

		this.c = p[pos.get()];

		if (p[pos.get()] == '/') {
			cc = MapCharClass.cSLASH;
			pos.incrementAndGet();
		} else if (pos.get() <= p.length - 3 && p[pos.get()] == '.' && p[pos.get() + 1] == '.' && p[pos.get() + 2] == '.') {
			cc = MapCharClass.cDOTS;
			paramNumber = PARAM_BASE_DOTS + nDots.getAndAdd(1);
			pos.addAndGet(3);
		} else if (pos.get() <= p.length - 3 && p[pos.get()] == '%' && p[pos.get() + 1] == '%' && p[pos.get() + 2] >= '0' && p[pos.get() + 2] <= '9') {
			cc = MapCharClass.cPERC;
			paramNumber = PARAM_BASE_PERCENT + (p[pos.get() + 2] - '0');
			pos.addAndGet(3);
		} else if (p[pos.get()] == '*') {
			cc = MapCharClass.cSTAR;
			paramNumber = PARAM_BASE_STARS + nStars.getAndAdd(1);
			pos.incrementAndGet();
		} else {
			cc = MapCharClass.cCHAR;
			pos.incrementAndGet();
		}

		return true;
	}

	public String makeParam(String p, MapChar mc2, AtomicInteger wildSlot) {
		if (cc == MapCharClass.cDOTS && mc2.cc == MapCharClass.cDOTS) {
			p += "...";
		} else {
			p += "%%" + wildSlot.addAndGet(1);
		}
		return p;
	}
}
