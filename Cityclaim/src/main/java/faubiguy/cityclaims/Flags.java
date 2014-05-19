package faubiguy.cityclaims;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public abstract class Flags {
	
	public final Map<String,String> flagTypes;
	
	public Flags inheritFrom;
	
	public Flags(Map<String,String> flagTypes) {
		this.flagTypes = flagTypes;
	}
	
	public Map<String,String> getFlagTypes() {
		return flagTypes;
	}
	
	public abstract Flags getDefaults();

	protected Map<String, Object> flags;

	public List<String> listFlags() {
		return new ArrayList<String>(getFlagTypes().keySet());
	}

	public Map<String, Object> getMap() {
		return flags;
	}

	public Object getFlag(String flag) {
		Object value = getFlagNoInherit(flag);
		if (value == null && inheritFrom != null) {
			value = inheritFrom.getFlag(flag);
		}
		return value == null ? getDefaults().getFlagNoInherit(flag) : value;
	}
	
	public Object getFlagNoInherit(String flag) {
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

	public String getFlagString(String flag) {
		Object value = getFlag(flag);
		if (value instanceof String) {
			return (String)value;
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
	
	public void setFlags(Map<String,Object> flags) {
		for (Map.Entry<String, Object> entry : flags.entrySet()) {
			setFlag(entry.getKey(), entry.getValue());
		}
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
		} else if (type == "string") {
			return value;
		}
		return null;
	}
	
	public abstract void save();
	
	public boolean isFlag(String flag) {
		return getFlagTypes().get(flag) != null;
	}
	
	public boolean equals(Object other) {
		return (other instanceof Flags) && flags.equals(((Flags)other).getMap());
	}
	
	public int hashCode() {
		return flags.hashCode();
	}

}