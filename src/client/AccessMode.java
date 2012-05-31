package client;

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

	public static AccessMode getMode(String mode) {
		for (AccessMode availableMode : AccessMode.values()) {
			if (availableMode.toString().equals(mode))
				return availableMode;
		}
		throw new IllegalArgumentException();
	}
}