package me.roan.imagescaler;

import com.twelvemonkeys.image.ResampleOp;

/**
 * Enum listing the available image scaling modes
 * @author Roan
 */
public enum ScalingMode{
	//Normal quality algorithms
	POINT          ("Point",     ResampleOp.FILTER_POINT),
	BOX            ("Box",       ResampleOp.FILTER_BOX),
	TRIANGLE       ("Triangle",  ResampleOp.FILTER_TRIANGLE),
	HERMITE        ("Hermite",   ResampleOp.FILTER_HERMITE),
	HANNING        ("Hanning",   ResampleOp.FILTER_HANNING),
	HAMMING        ("Hamming",   ResampleOp.FILTER_HAMMING),
	BLACKMAN       ("Blackman",  ResampleOp.FILTER_BLACKMAN),
	GAUSSIAN       ("Gaussian",  ResampleOp.FILTER_GAUSSIAN),
	QUADRATIC      ("Quadratic", ResampleOp.FILTER_QUADRATIC),
	CUBIC          ("Cubic",     ResampleOp.FILTER_CUBIC),
	CATROM         ("Catrom",    ResampleOp.FILTER_CATROM),
	//High quality algorithms
	MITCHELL       ("Mitchell (high quality)",        ResampleOp.FILTER_MITCHELL),
	LANCZOS        ("Lanczos (high quality)",         ResampleOp.FILTER_LANCZOS),
	BLACKMAN_BESSEL("Blackman bessel (high quality)", ResampleOp.FILTER_BLACKMAN_BESSEL),
	BLACKMAN_SINC  ("Blackman sinc (high quality)",   ResampleOp.FILTER_BLACKMAN_SINC);

	/**
	 * Display name of the algorithm
	 */
	private final String name;
	/**
	 * Identifier of the algorithm
	 */
	protected final int mode;

	/**
	 * Constructs a new ScalingMode
	 * with the given name and mode identifier
	 * @param name The display name of the algorithm
	 * @param mode The mode identifier of the algorithm
	 */
	private ScalingMode(String name, int mode){
		this.name = name;
		this.mode = mode;
	}

	@Override
	public String toString(){
		return name;
	}
}
