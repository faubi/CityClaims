package faubiguy.cityclaims;

import java.util.Map;

public class AdvancedPricing {

	public static class Range {
		int min;
		int max;
		boolean unbounded;

		public Range(int min, int max, boolean unbounded) {
			this.min = min;
			this.max = max;
			this.unbounded = unbounded;
		}

		public Range(int min, int max) {
			this(min, max, false);
		}

		public static Range fromString(String str) {
			if (str.matches("[0-9]+")) {
				return new Range(Integer.parseInt(str), Integer.parseInt(str));
			} else if (str.matches("[0-9]+\\-[0-9]+")) {
				String[] minmax = str.split("-");
				if (minmax.length != 2) {
					return null;
				}
				return new Range(Integer.parseInt(minmax[0]),
						Integer.parseInt(minmax[1]));
			} else if (str.matches("[0-9]+\\+")) {
				return new Range(Integer.parseInt(str.substring(0,
						str.length() - 1)), 0, true);
			} else {
				return null;
			}
		}

		public boolean contains(int i) {
			return i >= min && (i <= max || unbounded);
		}

		@Override
		public String toString() {
			if (unbounded) {
				return Integer.toString(min) + "+";
			} else {
				if (min == max) {
					return Integer.toString(min);
				} else {
					return Integer.toString(min) + "-" + Integer.toString(max);
				}
			}
		}

		@Override
		public boolean equals(Object obj) {
			if (!(obj instanceof Range)) {
				return false;
			}
			Range other = (Range) obj;
			return (min == other.min && max == other.max && !unbounded && !other.unbounded)
					|| (min == other.min && unbounded && other.unbounded);
		}

		@Override
		public int hashCode() {
			return unbounded ? -min : min + max + min * max;
		}
	}

	private Map<Range, Double> ranges;

	public Double getPrice(int owned) {
		for (Map.Entry<Range, Double> entry : ranges.entrySet()) {
			if (entry.getKey().contains(owned)) {
				return entry.getValue();
			}
		}
		return null;
	}

	public boolean addRange(Range range, double price) {
		if (!canAddRange(range)) {
			return false;
		}
		ranges.put(range, price);
		return true;
	}

	public boolean canAddRange(Range rangeToCheck) {
		for (Range range : ranges.keySet()) {
			if (rangeToCheck.unbounded) {
				if (rangeToCheck.min <= range.max || range.unbounded) {
					return false;
				}
			} else {
				if (range.contains(rangeToCheck.min)
						|| range.contains(rangeToCheck.max)) {
					return false;
				}
			}
		}
		return true;
	}

	public Map<Range, Double> getRanges() {
		return ranges;
	}

	@Override
	public boolean equals(Object other) {
		return (other instanceof AdvancedPricing)
				&& ranges.equals(((AdvancedPricing) other).getRanges());
	}

	@Override
	public int hashCode() {
		return ranges.hashCode();
	}

}
