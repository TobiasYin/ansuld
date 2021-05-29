package org.minal.minal.vnc;


public enum COLORMODEL {
	C24bit, C16bit;

	public int bpp() {
		switch (this) {
		case C24bit:
			return 4;
		case C16bit:
			return 2;
		default:
			return 1;
		}
	}

	public String nameString()
	{
		return super.toString();
	}


	public String toString() {
		switch (this) {
		case C24bit:
			return "24-bit color (4 bpp)";
		case C16bit:
			return "16-bit color (2 bpp)";
		default:
			return "";
		}
	}

}
