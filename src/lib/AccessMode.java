package lib;

public enum AccessMode {
	READ("r"), WRITE("w");

	private String value;

	private AccessMode(String value) {
		this.value = value;
	}

	@Override
	public String toString() {
		return value;
	}

	public static AccessMode getMode(String mode) throws IllegalArgumentException {
		for (AccessMode availableMode : AccessMode.values()) {
			if (availableMode.toString().equals(mode))
				return availableMode;
		}
		throw new IllegalArgumentException(
				"Requested an unsupported access mode for a file. Only supported modes are READ (\"r\") and WRITE (\"w\")");
	}
}