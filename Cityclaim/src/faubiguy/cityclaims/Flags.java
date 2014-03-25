package faubiguy.cityclaims;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public abstract class Flags {
	
	public final Map<String,String> flagTypes;
	
	public Flags(Map<String,String> flagTypes) {
		this.flagTypes = flagTypes;
	}
	
	public Map<String,String> getFlagTypes() {
		return flagTypes;
	}

	protected Map<String, Object> flags;

	public List<String> listFlags() {
		return new ArrayList<>(getFlagTypes().keySet());
	}

	public Map<String, Object> getMap() {
		return flags;
	}

	public Object getFlag(String flag) {
		return flags.get(flag);
	}

	public Boolean getFlagBoolean(String flag) {
		Object value = getFlag(flag);
		if (value instanceof Boolean) {
			return (Boolean)value;
		}
		return null;
	}

	public Double getFlagDouble(String flag) {
		Object value = getFlag(flag);
		if (value instanceof Double) {
			return (Double)value;
		}
		return null;
	}

	public Integer getFlagInt(String flag) {
		Object value = getFlag(flag);
		if (value instanceof Integer) {
			return (Integer)value;
		}
		return null;
	}

	public boolean setFlag(String flag, Object value) {
		flags.put(flag, value);
		return true;
	}

	public boolean removeFlag(String flag) {
		return flags.remove(flag) != null;
	}

	public Object getFlagValueFromString(String flag, String value) {
		String type = getFlagTypes().get(flag);
		if (type == null) {
			return null;
		} else if (type == "integer") {
			try {
				return Integer.parseInt(value);
			} catch (NumberFormatException e) {
				return null;
			}
		} else if (type == "double") {
			try {
				return Double.parseDouble(value);
			} catch (NumberFormatException e) {
				return null;
			}
		} else if (type == "boolean") {
			if (value.equalsIgnoreCase("true")) {
				return true;
			} else if (value.equalsIgnoreCase("false")) {
				return false;
			} else {
				return null;
			}
		}
		return null;
	}
	
	public abstract void save();
	
	public boolean equals(Object other) {
		return (other instanceof Flags) && flags.equals(((Flags)other).getMap());
	}
	
	public int hashCode() {
		return flags.hashCode();
	}

}