package faubiguy.cityclaims;

public class PlotSize {

	public int width; // The shorter side by default
	public int length; // The longer side by default

	public PlotSize(int l, int w) {
		width = w < l ? w : l;
		length = w < l ? l : w;
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof PlotSize)) {
			return false;
		}
		PlotSize size = (PlotSize) obj;
		return (this.length == size.length && this.width == size.width)
				|| (this.length == size.width && this.width == size.length);
	}

	@Override
	public int hashCode() {
		return length + width + length * width;
	}

	@Override
	public String toString() {
		return Integer.toString(width < length ? width : length) + "x"
				+ Integer.toString(width < length ? length : width);
	}

	public static PlotSize fromString(String str) {
		String[] wl = str.split("x");
		if (!(wl.length == 2)) {
			return null;
		}
		try {
			return new PlotSize(Integer.parseInt(wl[0]),
					Integer.parseInt(wl[1]));
		} catch (NumberFormatException e) {
			return null;
		}
	}

}
